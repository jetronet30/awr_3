package com.jaba.awr_3.core.parsers;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;

import com.jaba.awr_3.controllers.emitter.EmitterServic;
import com.jaba.awr_3.core.prodata.services.TrainService;
import com.jaba.awr_3.seversettings.basic.BasicService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TunaylarParser {
    private final TrainService trainService;

    private static final double WEIGHT_DEVIATION_LIMIT = 0.2;
    private static final double NEW_WAGON_TRANSITION_THRESHOLD = 0.5;
    private static final double START_WEIGHT_THRESHOLD = 10.0;
    private static final long START_DURATION_MS = 2_000L;
    private static final long STABLE_DURATION_MS = 20_000L;
    private static final long STOP_DURATION_MS = 40_000L;

    private final EmitterServic emitterServic;
    private final ConcurrentHashMap<String, AtomicReference<ParserState>> states = new ConcurrentHashMap<>();

    public void parseSectors(String text, String scaleName, String conId, int scaleIndex,
            boolean automatic, boolean rightToUpdateTare) {

        String[] lines = text.split(" ");
        if (lines.length < 2) {
            return;
        }

        String normalizedWeight = getWeight(lines[1]);
        double currentWeight;
        try {
            currentWeight = Double.parseDouble(normalizedWeight);
        } catch (NumberFormatException e) {
            return;
        }

        long now = System.currentTimeMillis();
        AtomicReference<ParserState> stateRef = states.computeIfAbsent(
                conId, key -> new AtomicReference<>(ParserState.initial()));

        while (true) {
            ParserState currentState = stateRef.get();
            Transition transition = nextState(currentState, currentWeight, now, normalizedWeight);

            if (stateRef.compareAndSet(currentState, transition.newState())) {
                emitTransition(conId, transition, scaleName, scaleIndex, automatic, rightToUpdateTare);
                return;
            }
        }
    }

    private Transition nextState(ParserState state, double currentWeight, long now, String normalizedWeight) {

        if (!state.weighingStarted()) {
            if (currentWeight < START_WEIGHT_THRESHOLD) {
                return new Transition(
                        state.withStartCandidate(false, 0L),
                        false, null, false, false, 0);
            }

            if (!state.startCandidateActive()) {
                return new Transition(
                        state.withStartCandidate(true, now)
                                .withStableWindow(now, currentWeight, currentWeight),
                        false, null, false, false, 0);
            }

            if ((now - state.startCandidateTime()) >= START_DURATION_MS) {
                ParserState startedState = state.withWeighingStarted(true)
                        .withStartCandidate(false, 0L)
                        .withLowWeightTiming(false, 0L)
                        .withStableWindow(now, currentWeight, currentWeight)
                        .withLastEmittedWeight(null)
                        .withRowIndex(0)
                        .withWeightTransitionDetected(false);

                return new Transition(startedState, false, null, true, false, 0);
            }

            return new Transition(
                    state.withStableWindow(state.stableWindowStartTime(), currentWeight, currentWeight),
                    false, null, false, false, 0);
        }

        if (currentWeight < START_WEIGHT_THRESHOLD) {
            if (!state.lowWeightTimingStarted()) {
                ParserState lowState = state.withLowWeightTiming(true, now);
                return new Transition(lowState, false, null, false, false, 0);
            }

            if ((now - state.lowWeightStartTime()) >= STOP_DURATION_MS) {
                return new Transition(ParserState.initial(), false, null, false, true, 0);
            }

            return new Transition(state, false, null, false, false, 0);
        }

        ParserState activeState = state.lowWeightTimingStarted()
                ? state.withLowWeightTiming(false, 0L)
                : state;

        double newMin = Math.min(activeState.minStableWeight(), currentWeight);
        double newMax = Math.max(activeState.maxStableWeight(), currentWeight);

        if ((newMax - newMin) <= (WEIGHT_DEVIATION_LIMIT * 2)) {
            ParserState updatedState = activeState.withStableWindow(
                    activeState.stableWindowStartTime(), newMin, newMax);

            if ((now - activeState.stableWindowStartTime()) >= STABLE_DURATION_MS) {
                boolean sameAsLast = activeState.lastEmittedWeight() != null
                        && Math.abs(activeState.lastEmittedWeight() - currentWeight) <= WEIGHT_DEVIATION_LIMIT;

                boolean shouldEmit = activeState.lastEmittedWeight() == null
                        || !sameAsLast
                        || activeState.weightTransitionDetected();

                if (shouldEmit) {
                    int nextRowIndex = activeState.rowIndex() + 1;

                    ParserState emittedState = updatedState
                            .withStableWindow(now, currentWeight, currentWeight)
                            .withLastEmittedWeight(currentWeight)
                            .withRowIndex(nextRowIndex)
                            .withWeightTransitionDetected(false);

                    return new Transition(emittedState, true, normalizedWeight, false, false, nextRowIndex);
                }
            }

            return new Transition(updatedState, false, null, false, false, 0);
        }

        boolean significantTransition = activeState.lastEmittedWeight() != null
                && Math.abs(activeState.lastEmittedWeight() - currentWeight) > NEW_WAGON_TRANSITION_THRESHOLD;

        ParserState resetWindowState = activeState
                .withStableWindow(now, currentWeight, currentWeight)
                .withWeightTransitionDetected(activeState.weightTransitionDetected() || significantTransition);

        return new Transition(resetWindowState, false, null, false, false, 0);
    }

    private void emitTransition(String conId, Transition transition, String scaleName, int scaleIndex,
            boolean automatic, boolean rightToUpdateTare) {
        if (transition.sendStart()) {
            trainService.closeTrainAndOpenNewTrain(conId, scaleName, scaleIndex);
            emitterServic.sendToScale(conId, "update-data-container");
            emitterServic.sendToScale(conId, "update-data-works-start");
            if (automatic) {
                // Assuming ocrLis is available in this context, otherwise this needs to be
                // refactored to include it
            }
            System.out.println("ConId: " + conId + ", STARTING WEIGHING PROCESS");
        }

        if (transition.shouldEmitWeight()) {
            trainService.addWagonToTrain(conId, "", transition.rowIndexToEmit(), transition.weightToEmit(),
                    BasicService.getDateTime(),
                    "0.0", 0, rightToUpdateTare);

            emitterServic.sendToScale(conId, transition.weightToEmit());
            if (automatic) {
                emitterServic.sendToScale(conId, "update-data-container");
            }
            System.out.println("ConId: " + conId + ", ROW_INDEX: " + transition.rowIndexToEmit() + ", WEIGHT: "
                    + transition.weightToEmit());
        }

        if (transition.sendEnd()) {
            trainService.updateTrain(conId, "", "0.0", BasicService.getDateTime(), "0.0", "0.0", transition.rowIndexToEmit());
            trainService.updateTrainAndWagons(conId, "NO", "static");
            emitterServic.sendToScale(conId, "update-data-container");
            emitterServic.sendToScale(conId, "update-data-works-stop");
            if (automatic) {
                // ocrLis.sendStop(scaleIndex, trainService.getIdOpenTrain(conId));
            }

            System.out.println("ConId: " + conId + ", ENDING WEIGHING PROCESS");
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
            boolean weightTransitionDetected) {

        static ParserState initial() {
            return new ParserState(false, false, 0L, 0L, 0.0, 0.0, null, false, 0L, 0, false);
        }

        ParserState withWeighingStarted(boolean value) {
            return new ParserState(value, startCandidateActive, startCandidateTime, stableWindowStartTime,
                    minStableWeight, maxStableWeight, lastEmittedWeight, lowWeightTimingStarted, lowWeightStartTime,
                    rowIndex, weightTransitionDetected);
        }

        ParserState withStartCandidate(boolean active, long time) {
            return new ParserState(weighingStarted, active, time, stableWindowStartTime,
                    minStableWeight, maxStableWeight, lastEmittedWeight, lowWeightTimingStarted, lowWeightStartTime,
                    rowIndex, weightTransitionDetected);
        }

        ParserState withStableWindow(long startTime, double minWeight, double maxWeight) {
            return new ParserState(weighingStarted, startCandidateActive, startCandidateTime, startTime,
                    minWeight, maxWeight, lastEmittedWeight, lowWeightTimingStarted, lowWeightStartTime,
                    rowIndex, weightTransitionDetected);
        }

        ParserState withLastEmittedWeight(Double value) {
            return new ParserState(weighingStarted, startCandidateActive, startCandidateTime, stableWindowStartTime,
                    minStableWeight, maxStableWeight, value, lowWeightTimingStarted, lowWeightStartTime,
                    rowIndex, weightTransitionDetected);
        }

        ParserState withLowWeightTiming(boolean active, long time) {
            return new ParserState(weighingStarted, startCandidateActive, startCandidateTime, stableWindowStartTime,
                    minStableWeight, maxStableWeight, lastEmittedWeight, active, time,
                    rowIndex, weightTransitionDetected);
        }

        ParserState withRowIndex(int value) {
            return new ParserState(weighingStarted, startCandidateActive, startCandidateTime, stableWindowStartTime,
                    minStableWeight, maxStableWeight, lastEmittedWeight, lowWeightTimingStarted, lowWeightStartTime,
                    value, weightTransitionDetected);
        }

        ParserState withWeightTransitionDetected(boolean value) {
            return new ParserState(weighingStarted, startCandidateActive, startCandidateTime, stableWindowStartTime,
                    minStableWeight, maxStableWeight, lastEmittedWeight, lowWeightTimingStarted, lowWeightStartTime,
                    rowIndex, value);
        }
    }

    private record Transition(
            ParserState newState,
            boolean shouldEmitWeight,
            String weightToEmit,
            boolean sendStart,
            boolean sendEnd,
            int rowIndexToEmit) {
    }
}
