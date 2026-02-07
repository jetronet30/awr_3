package com.jaba.awr_3.core.prodata.services;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jaba.awr_3.core.prodata.jparepo.TrainJpa;
import com.jaba.awr_3.core.prodata.jparepo.WagonJpa;
import com.jaba.awr_3.core.prodata.mod.TrainMod;
import com.jaba.awr_3.core.prodata.mod.WagonMod;
import com.jaba.awr_3.core.tare.WtareService;
import com.jaba.awr_3.core.units.UnitService;
import com.jaba.awr_3.servermanager.ServerManager;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TrainService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrainService.class);

    private final TrainJpa trainJpa;
    private final WagonJpa wagonJpa;
    private final WtareService wtareService;

    @Transactional
    public void addTrain(String conId, String instrument, String scaleName) {
        TrainMod train = new TrainMod();
        train.setConId(conId);
        train.setWeighingStartDateTime(ServerManager.getSystemDateTime());
        train.setInstrument(instrument);
        train.setScaleName(scaleName);
        train.setOpen(true);
        train.setDone(false);
        train.setCount(0);
        trainJpa.save(train);
        LOGGER.info("New train created with conId: {}", conId);
    }

    @Transactional
    public void addWagonToTrain(String conId, String processId, int rowNum, String weighString) {
        BigDecimal weight = getBigDecimal(weighString);
        TrainMod train = trainJpa.findByOpenTrueAndConId(conId).orElse(null);

        if (train == null) {
            LOGGER.warn("No open train found for conId: {}", conId);
            return;
        }

        // ვეძებთ არსებულ ვაგონს rowNum-ით
        WagonMod wagonToSave = train.getWagons().stream()
                .filter(w -> w.getRowNum() == rowNum)
                .findFirst()
                .orElseGet(() -> {
                    WagonMod newWagon = new WagonMod();
                    newWagon.setConnId(conId);
                    newWagon.setScaleName(train.getScaleName());
                    newWagon.setRowNum(rowNum);
                    newWagon.setTrain(train);
                    train.getWagons().add(newWagon);
                    LOGGER.info("Creating new wagon rowNum {} for train conId {}", rowNum, conId);
                    return newWagon;
                });

        if (wagonToSave.getId() != null) {
            LOGGER.info("Updating existing wagon rowNum {} in train conId {}", rowNum, conId);
        }

        wagonToSave.setProcessId(processId);
        wagonToSave.setValid(true);

        applyWeightAndTare(wagonToSave, weight);

        wagonJpa.save(wagonToSave);

        LOGGER.info("Wagon rowNum {} processed (conId: {})", rowNum, conId);
    }

    @Transactional
    public void addWagonToTrain(String conId, String wagonNumber, String product, int count) {
        int wcount = Math.min(Math.max(count, 1), 300);
        if (wcount > 1) wagonNumber = "";

        TrainMod train = trainJpa.findByOpenTrueAndConId(conId).orElse(null);
        if (train == null) {
            LOGGER.warn("No open train found for conId: {}", conId);
            return;
        }

        int currentSize = train.getWagons().size();
        if (currentSize + wcount > 300) {
            LOGGER.warn("Cannot add {} wagons — limit would be exceeded (current: {}, max: 300)", 
                        wcount, currentSize);
            return;
        }

        for (int i = 0; i < wcount; i++) {
            WagonMod wagon = new WagonMod();
            wagon.setConnId(conId);
            wagon.setScaleName(train.getScaleName());
            wagon.setWagonNumber(wagonNumber);
            wagon.setProduct(product);
            wagon.setRowNum(currentSize + i + 1);
            wagon.setValid(false);
            wagon.setTrain(train);
            wagonJpa.save(wagon);
        }

        LOGGER.info("Added {} wagon(s) with number {} to train conId {}", wcount, wagonNumber, conId);
    }

    @Transactional
    public Map<String, Object> updateWagonToTrain(Long id, String conId, String wagonNumber, String product) {
        Map<String, Object> response = new HashMap<>();

        WagonMod wagon = wagonJpa.findById(id).orElse(null);
        if (wagon == null) {
            response.put("success", false);
            response.put("message", "Wagon not found with id: " + id);
            LOGGER.warn("Wagon not found with id: {}", id);
            return response;
        }

        wagon.setWagonNumber(wagonNumber);
        wagon.setProduct(product);

        BigDecimal weight = wagon.getWeight();
        if (weight != null && weight.signum() > 0 && weight.compareTo(UnitService.TARE_LIMIT) < 0) {
            wagon.setTare(weight);
            String num = wagon.getWagonNumber();
            if (num != null && !num.isEmpty()) {
                wtareService.addOrUpdateWtare(weight.toString(), num);
            }
        } else {
            String num = wagon.getWagonNumber();
            wagon.setTare(num != null && !num.isEmpty() ? wtareService.getTareByNumber(num) : null);
        }

        wagonJpa.save(wagon);
        updateTrain(wagon.getTrain());

        response.put("success", true);
        response.put("message", "Wagon updated successfully");
        LOGGER.info("Wagon id {} updated successfully", id);

        return response;
    }

    @Transactional
    public void closeTrain(String conId) {
        TrainMod train = trainJpa.findByOpenTrueAndConId(conId).orElse(null);
        if (train == null) {
            LOGGER.warn("No open train found for conId: {}", conId);
            return;
        }

        // წავშალოთ ბაზიდან ყველა არავალიდური ვაგონი
        wagonJpa.deleteByTrainIdAndValidFalse(train.getId());

        // კოლექციის განახლება (რადგან ბაზაში უკვე წაიშალა)
        train.getWagons().removeIf(w -> !w.isValid());

        // გამოვთვალოთ და შევინახოთ ტოტალები
        recalculateTrainTotals(train);

        train.setWeighingStopDateTime(ServerManager.getSystemDateTime());
        train.setOpen(false);
        trainJpa.save(train);

        LOGGER.info("Train conId {} closed successfully", conId);
    }

    @Transactional
    private void updateTrain(String conId, String processId, String maxSpeed, String minSpeed) {
        TrainMod train = trainJpa.findByOpenTrueAndConId(conId).orElse(null);
        if (train == null) {
            LOGGER.warn("No open train found for conId: {}", conId);
            return;
        }

        wagonJpa.deleteByTrainIdAndValidFalse(train.getId());
        train.getWagons().removeIf(w -> !w.isValid());

        train.setProcessId(processId);
        train.setMaxSpeed(maxSpeed);
        train.setMinSpeed(minSpeed);
        trainJpa.save(train);

        LOGGER.info("Train conId {} updated (processId, speeds)", conId);
    }

    @Transactional
    public void updateTrainAndWagons(String conId, String direction) {
        TrainMod train = trainJpa.findByOpenTrueAndConId(conId).orElse(null);
        if (train == null) {
            LOGGER.warn("No open train found for conId: {}", conId);
            return;
        }

        train.setDirection(direction);
        train.setWeighingStopDateTime(ServerManager.getSystemDateTime());

        recalculateTrainTotals(train);

        train.setDone(true);
        trainJpa.save(train);

        LOGGER.info("Train conId {} updated with direction {}, done=true", conId, direction);
    }

    @Transactional
    public void updateTrain(TrainMod train) {
        if (train == null || !train.isOpen() || !train.isDone()) {
            return;
        }

        recalculateTrainTotals(train);
        trainJpa.save(train);
    }

    // ────────────────────────────────────────────────
    // დამხმარე მეთოდები
    // ────────────────────────────────────────────────

    private void applyWeightAndTare(WagonMod wagon, BigDecimal weight) {
        if (weight == null) {
            wagon.setWeight(null);
            wagon.setTare(null);
            return;
        }

        wagon.setWeight(weight);

        if (weight.signum() > 0 && weight.compareTo(UnitService.TARE_LIMIT) <= 0) {
            wagon.setTare(weight);
            String num = wagon.getWagonNumber();
            if (num != null && !num.isEmpty()) {
                wtareService.addOrUpdateWtare(weight.toString(), num);
            }
        } else {
            String num = wagon.getWagonNumber();
            wagon.setTare(num != null && !num.isEmpty() ? wtareService.getTareByNumber(num) : null);
        }
    }

    private void recalculateTrainTotals(TrainMod train) {
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal tareSum = BigDecimal.ZERO;
        boolean allValidHaveTare = !train.getWagons().isEmpty();

        for (WagonMod w : train.getWagons()) {
            if (!w.isValid()) continue;

            BigDecimal weight = w.getWeight() != null ? w.getWeight() : BigDecimal.ZERO;
            gross = gross.add(weight);

            BigDecimal wagonTare = w.getTare();
            if (wagonTare == null || wagonTare.compareTo(BigDecimal.ZERO) <= 0) {
                allValidHaveTare = false;
            } else {
                tareSum = tareSum.add(wagonTare);
            }
        }

        train.setGross(gross);

        if (allValidHaveTare && gross.compareTo(BigDecimal.ZERO) > 0) {
            train.setTare(tareSum);
            train.setNeto(gross.subtract(tareSum));
        } else {
            train.setTare(null);
            train.setNeto(null);
        }
    }

    private BigDecimal getBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            LOGGER.debug("Invalid weight string: '{}'", value, e);
            return BigDecimal.ZERO;
        }
    }
}