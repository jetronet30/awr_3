package com.jaba.awr_3.core.parsers;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;

import com.jaba.awr_3.controllers.emitter.EmitterServic;
import com.jaba.awr_3.core.prodata.services.TrainService;
import com.jaba.awr_3.seversettings.basic.BasicService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TunaylarParser {
    private final TrainService trainService;

    private static final double WEIGHT_DEVIATION_LIMIT = 0.2;
    private static final double NEW_WAGON_TRANSITION_THRESHOLD = 0.5;
    private static final double START_WEIGHT_THRESHOLD = 20.0;
    private static final long START_DURATION_MS = 2_000L;
    private static final long STABLE_DURATION_MS = 10_000L;
    private static final long STOP_DURATION_MS = 10_000L;
    private static final long ABORT_DURATION_MS = 300_000L;

    private final EmitterServic emitterServic;
    private final ConcurrentHashMap<String, AtomicReference<ParserState>> states = new ConcurrentHashMap<>();

    public void parseSectors(String text, String scaleName, String conId, int scaleIndex,
            boolean automatic, boolean rightToUpdateTare) {

        String[] lines = text.split("\\s+");
        if (lines.length < 2) {
            return;
        }

        String normalizedWeight = getWeight(lines[1]);
        double currentWeight;
        try {
            currentWeight = Double.parseDouble(normalizedWeight);
        } catch (NumberFormatException e) {
            log.warn("ConId: {}, invalid weight payload: {}", conId, lines[1]);
            return;
        }

        long now = System.currentTimeMillis();
        log.debug("ConId: {}, incoming weight: {}", conId, normalizedWeight);

        AtomicReference<ParserState> stateRef = states.computeIfAbsent(
                conId, key -> new AtomicReference<>(ParserState.initial()));

        while (true) {
            ParserState currentState = stateRef.get();
            Transition transition = nextState(currentState, currentWeight, now, normalizedWeight, conId);

            if (stateRef.compareAndSet(currentState, transition.newState())) {
                emitTransition(conId, transition, scaleName, scaleIndex, automatic, rightToUpdateTare);
                return;
            }
        }
    }

    private Transition nextState(ParserState state, double currentWeight, long now, String normalizedWeight, String conId) {

        if (!state.weighingStarted()) {
            if (currentWeight < START_WEIGHT_THRESHOLD) {
                if (state.startCandidateActive()) {
                    log.info("ConId: {}, start candidate cleared, weight dropped below threshold: {}", conId, currentWeight);
                }
                return new Transition(
                        state.withStartCandidate(false, 0L),
                        false, null, false, false, false, 0);
            }

            if (!state.startCandidateActive()) {
                log.info("ConId: {}, start candidate detected at weight {}", conId, currentWeight);
                return new Transition(
                        state.withStartCandidate(true, now)
                                .withStableWindow(now, currentWeight, currentWeight),
                        false, null, false, false, false, 0);
            }

            if ((now - state.startCandidateTime()) >= START_DURATION_MS) {
                ParserState startedState = state.withWeighingStarted(true)
                        .withStartCandidate(false, 0L)
                        .withLowWeightTiming(false, 0L)
                        .withStableWindow(now, currentWeight, currentWeight)
                        .withLastEmittedWeight(null)
                        .withRowIndex(0)
                        .withWeightTransitionDetected(false)
                        .withLastRowIndexChangeTime(now);

                log.info("ConId: {}, weighing started at weight {}", conId, currentWeight);
                return new Transition(startedState, false, null, true, false, false, 0);
            }

            return new Transition(
                    state.withStableWindow(state.stableWindowStartTime(), currentWeight, currentWeight),
                    false, null, false, false, false, 0);
        }

        if ((now - state.lastRowIndexChangeTime()) >= ABORT_DURATION_MS) {
            log.warn("ConId: {}, abort timeout reached, last row index: {}, idleMs: {}",
                    conId, state.rowIndex(), now - state.lastRowIndexChangeTime());
            return new Transition(ParserState.initial(), false, null, false, false, true, state.rowIndex());
        }

        if (currentWeight < START_WEIGHT_THRESHOLD) {
            if (!state.lowWeightTimingStarted()) {
                log.info("ConId: {}, low weight detected, stop timer started at weight {}", conId, currentWeight);
                ParserState lowState = state.withLowWeightTiming(true, now);
                return new Transition(lowState, false, null, false, false, false, 0);
            }

            if ((now - state.lowWeightStartTime()) >= STOP_DURATION_MS) {
                log.info("ConId: {}, weighing ended after low weight timeout, last row index: {}", conId, state.rowIndex());
                return new Transition(ParserState.initial(), false, null, false, true, false, state.rowIndex());
            }

            return new Transition(state, false, null, false, false, false, 0);
        }

        ParserState activeState = state.lowWeightTimingStarted()
                ? state.withLowWeightTiming(false, 0L)
                : state;

        if (state.lowWeightTimingStarted()) {
            log.info("ConId: {}, low weight timer cleared, weight returned to {}", conId, currentWeight);
        }

        if ((now - activeState.lastRowIndexChangeTime()) >= ABORT_DURATION_MS) {
            log.warn("ConId: {}, abort timeout reached, last row index: {}, idleMs: {}",
                    conId, activeState.rowIndex(), now - activeState.lastRowIndexChangeTime());
            return new Transition(ParserState.initial(), false, null, false, false, true, activeState.rowIndex());
        }

        if (activeState.lastEmittedWeight() == null) {
            return evaluateStableWeight(activeState, currentWeight, now, normalizedWeight, false, conId);
        }

        double diffFromLastEmitted = Math.abs(activeState.lastEmittedWeight() - currentWeight);

        boolean significantTransitionStarted = !activeState.weightTransitionDetected()
                && diffFromLastEmitted > NEW_WAGON_TRANSITION_THRESHOLD;

        if (significantTransitionStarted) {
            log.info("ConId: {}, new wagon transition detected. lastWeight={}, currentWeight={}, diff={}",
                    conId, activeState.lastEmittedWeight(), currentWeight, diffFromLastEmitted);

            ParserState transitionState = activeState
                    .withStableWindow(now, currentWeight, currentWeight)
                    .withWeightTransitionDetected(true);

            return new Transition(transitionState, false, null, false, false, false, 0);
        }

        if (activeState.weightTransitionDetected()) {
            return evaluateStableWeight(activeState, currentWeight, now, normalizedWeight, true, conId);
        }

        return new Transition(activeState, false, null, false, false, false, 0);
    }

    private Transition evaluateStableWeight(ParserState state, double currentWeight, long now,
            String normalizedWeight, boolean emitAfterStable, String conId) {

        double newMin = Math.min(state.minStableWeight(), currentWeight);
        double newMax = Math.max(state.maxStableWeight(), currentWeight);

        if ((newMax - newMin) > (WEIGHT_DEVIATION_LIMIT * 2)) {
            log.debug("ConId: {}, stable window reset. min={}, max={}, current={}",
                    conId, newMin, newMax, currentWeight);

            ParserState resetWindowState = state.withStableWindow(now, currentWeight, currentWeight);
            return new Transition(resetWindowState, false, null, false, false, false, 0);
        }

        ParserState updatedState = state.withStableWindow(state.stableWindowStartTime(), newMin, newMax);

        if ((now - updatedState.stableWindowStartTime()) < STABLE_DURATION_MS) {
            return new Transition(updatedState, false, null, false, false, false, 0);
        }

        if (!emitAfterStable && updatedState.lastEmittedWeight() != null) {
            return new Transition(updatedState, false, null, false, false, false, 0);
        }

        int nextRowIndex = updatedState.rowIndex() + 1;

        log.info("ConId: {}, stable wagon weight confirmed. rowIndex={}, weight={}, stableForMs={}",
                conId, nextRowIndex, normalizedWeight, now - updatedState.stableWindowStartTime());

        ParserState emittedState = updatedState
                .withStableWindow(now, currentWeight, currentWeight)
                .withLastEmittedWeight(currentWeight)
                .withRowIndex(nextRowIndex)
                .withWeightTransitionDetected(false)
                .withLastRowIndexChangeTime(now);

        return new Transition(emittedState, true, normalizedWeight, false, false, false, nextRowIndex);
    }

    private void emitTransition(String conId, Transition transition, String scaleName, int scaleIndex,
            boolean automatic, boolean rightToUpdateTare) {
        if (transition.sendStart()) {
            trainService.closeTrainAndOpenNewTrain(conId, scaleName, scaleIndex);
            emitterServic.sendToScale(conId, "update-data-container");
            emitterServic.sendToScale(conId, "update-data-works-start");
            log.info("ConId: {}, STARTING WEIGHING PROCESS", conId);
        }

        if (transition.shouldEmitWeight()) {
            trainService.addWagonToTrain(conId, "", transition.rowIndexToEmit(), transition.weightToEmit(),
                    BasicService.getDateTime(), "0.0", 0, rightToUpdateTare);

            emitterServic.sendToScale(conId, transition.weightToEmit());
            if (automatic) {
                emitterServic.sendToScale(conId, "update-data-container");
            }
            log.info("ConId: {}, ROW_INDEX: {}, WEIGHT: {}",
                    conId, transition.rowIndexToEmit(), transition.weightToEmit());
        }

        if (transition.sendEnd()) {
            trainService.updateTrain(conId, "", "0.0", BasicService.getDateTime(), "0.0", "0.0",
                    transition.rowIndexToEmit());
            trainService.updateTrainAndWagons(conId, "NO", "static");
            emitterServic.sendToScale(conId, "update-data-container");
            emitterServic.sendToScale(conId, "update-data-works-stop");
            log.info("ConId: {}, ENDING WEIGHING PROCESS", conId);
        }

        if (transition.sendAbort()) {
            trainService.updateTrain(conId, "", "0.0", BasicService.getDateTime(), "0.0", "0.0",
                    transition.rowIndexToEmit());
            trainService.deleteTrainByConId(conId);
            emitterServic.sendToScale(conId, "ABORT");
            emitterServic.sendToScale(conId, "update-data-container");
            log.warn("ConId: {}, ABORTING WEIGHING PROCESS", conId);
        }
    }

    private String getWeight(String input) {
        if (input == null || input.isEmpty()) {
            return "0.0";
        }

        input = input.replaceFirst("^0+", "");
        if (input.isEmpty()) {
            return "0.0";
        }

        if (input.endsWith("0") && input.length() > 0) {
            input = input.substring(0, input.length() - 1);
        }

        double result;
        try {
            result = Double.parseDouble(input) / 100.0;
        } catch (NumberFormatException e) {
            result = 0;
        }

        return String.valueOf(result);
    }

    private record ParserState(
            boolean weighingStarted,
            boolean startCandidateActive,
            long startCandidateTime,
            long stableWindowStartTime,
            double minStableWeight,
            double maxStableWeight,
            Double lastEmittedWeight,
            boolean lowWeightTimingStarted,
            long lowWeightStartTime,
            int rowIndex,
            boolean weightTransitionDetected,
            long lastRowIndexChangeTime) {

        static ParserState initial() {
            return new ParserState(false, false, 0L, 0L, 0.0, 0.0, null, false, 0L, 0, false, 0L);
        }

        ParserState withWeighingStarted(boolean value) {
            return new ParserState(value, startCandidateActive, startCandidateTime, stableWindowStartTime,
                    minStableWeight, maxStableWeight, lastEmittedWeight, lowWeightTimingStarted, lowWeightStartTime,
                    rowIndex, weightTransitionDetected, lastRowIndexChangeTime);
        }

        ParserState withStartCandidate(boolean active, long time) {
            return new ParserState(weighingStarted, active, time, stableWindowStartTime,
                    minStableWeight, maxStableWeight, lastEmittedWeight, lowWeightTimingStarted, lowWeightStartTime,
                    rowIndex, weightTransitionDetected, lastRowIndexChangeTime);
        }

        ParserState withStableWindow(long startTime, double minWeight, double maxWeight) {
            return new ParserState(weighingStarted, startCandidateActive, startCandidateTime, startTime,
                    minWeight, maxWeight, lastEmittedWeight, lowWeightTimingStarted, lowWeightStartTime,
                    rowIndex, weightTransitionDetected, lastRowIndexChangeTime);
        }

        ParserState withLastEmittedWeight(Double value) {
            return new ParserState(weighingStarted, startCandidateActive, startCandidateTime, stableWindowStartTime,
                    minStableWeight, maxStableWeight, value, lowWeightTimingStarted, lowWeightStartTime,
                    rowIndex, weightTransitionDetected, lastRowIndexChangeTime);
        }

        ParserState withLowWeightTiming(boolean active, long time) {
            return new ParserState(weighingStarted, startCandidateActive, startCandidateTime, stableWindowStartTime,
                    minStableWeight, maxStableWeight, lastEmittedWeight, active, time,
                    rowIndex, weightTransitionDetected, lastRowIndexChangeTime);
        }

        ParserState withRowIndex(int value) {
            return new ParserState(weighingStarted, startCandidateActive, startCandidateTime, stableWindowStartTime,
                    minStableWeight, maxStableWeight, lastEmittedWeight, lowWeightTimingStarted, lowWeightStartTime,
                    value, weightTransitionDetected, lastRowIndexChangeTime);
        }

        ParserState withWeightTransitionDetected(boolean value) {
            return new ParserState(weighingStarted, startCandidateActive, startCandidateTime, stableWindowStartTime,
                    minStableWeight, maxStableWeight, lastEmittedWeight, lowWeightTimingStarted, lowWeightStartTime,
                    rowIndex, value, lastRowIndexChangeTime);
        }

        ParserState withLastRowIndexChangeTime(long value) {
            return new ParserState(weighingStarted, startCandidateActive, startCandidateTime, stableWindowStartTime,
                    minStableWeight, maxStableWeight, lastEmittedWeight, lowWeightTimingStarted, lowWeightStartTime,
                    rowIndex, weightTransitionDetected, value);
        }
    }

    private record Transition(
            ParserState newState,
            boolean shouldEmitWeight,
            String weightToEmit,
            boolean sendStart,
            boolean sendEnd,
            boolean sendAbort,
            int rowIndexToEmit) {
    }
}
