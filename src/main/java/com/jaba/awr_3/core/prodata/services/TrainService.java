package com.jaba.awr_3.core.prodata.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jaba.awr_3.controllers.emitter.EmitterServic;
import com.jaba.awr_3.core.pdf.PdfCreator;
import com.jaba.awr_3.core.prodata.jparepo.TrainJpa;
import com.jaba.awr_3.core.prodata.jparepo.WagonJpa;
import com.jaba.awr_3.core.prodata.mod.TrainMod;
import com.jaba.awr_3.core.prodata.mod.WagonMod;
import com.jaba.awr_3.core.tare.WtareService;
import com.jaba.awr_3.core.units.UnitService;
import com.jaba.awr_3.seversettings.basic.BasicService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TrainService {
    private final EmitterServic emitterServic;
    private static final Logger LOGGER = LoggerFactory.getLogger(TrainService.class);

    private final TrainJpa trainJpa;
    private final WagonJpa wagonJpa;
    private final WtareService wtareService;
    private final PdfCreator pdfCreator;

    private void addTrain(String conId, String scaleName, int scaleIndex) {
        TrainMod train = new TrainMod();
        train.setConId(conId);
        train.setWeighingStartDateTime(BasicService.getDateTime());
        train.setOpen(true);
        train.setDone(false);
        train.setCount(0);
        train.setScaleName(scaleName);
        train.setScaleIndex(scaleIndex);
        train.setNormalWeight(false);
        train.setNormalSpeed(false);
        train.setAllwagonsNumbered(false);
        train.setBlocked(false);
        trainJpa.save(train);
        LOGGER.info("New train created with conId: {}, scaleIndex: {}", conId, scaleIndex);
    }

    @Transactional
    public void addWagonToTrain(String conId, String processId, int rowNum, String weighString, String weighingDateTime,
            String speed, int axle, boolean updateTare) {
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
        wagonToSave.setWeighingDateTime(weighingDateTime);
        wagonToSave.setSpeed(speed);
        wagonToSave.setAxle(axle);
        wagonToSave.setProcessId(processId);
        wagonToSave.setValid(true);

        applyWeightAndTare(wagonToSave, weight, updateTare);

        wagonJpa.save(wagonToSave);

        LOGGER.info("Wagon rowNum {} processed (conId: {})", rowNum, conId);
    }

    @Transactional
    public void addWagonToTrain(String conId, String wagonNumber, String product, int count) {
        int wcount = Math.min(Math.max(count, 1), 300);

        // თუ ერთზე მეტი ვაგონი → ნომერი ცარიელი უნდა იყოს
        String numberToUse = (wcount > 1) ? "" : wagonNumber;

        TrainMod train = trainJpa.findByOpenTrueAndConId(conId).orElse(null);
        if (train == null) {
            LOGGER.warn("No open train found for conId: {}", conId);
            return;
        }
        if (train.isDone()) {
            LOGGER.warn("Cannot add wagon to a done train with conId: {}", conId);
            return;
        }

        int currentSize = train.getWagons().size();
        if (currentSize + wcount > 300) {
            LOGGER.warn("Cannot add {} wagons — limit would be exceeded (current: {}, max: 300)",
                    wcount, currentSize);
            return;
        }

        // დუბლიკატის შემოწმება (მხოლოდ თუ numberToUse არ არის ცარიელი)
        if (numberToUse != null && !numberToUse.isEmpty()) {
            boolean duplicateExists = train.getWagons().stream()
                    .anyMatch(w -> numberToUse.equals(w.getWagonNumber()));
            if (duplicateExists) {
                LOGGER.warn("Cannot add wagon — duplicate wagon number {} in train conId {}", numberToUse, conId);
                return;
            }
        }

        for (int i = 0; i < wcount; i++) {
            WagonMod wagon = new WagonMod();
            wagon.setConnId(conId);
            wagon.setScaleName(train.getScaleName());
            wagon.setWagonNumber(numberToUse); // ← აქ numberToUse
            wagon.setProduct(product);
            wagon.setRowNum(currentSize + i + 1);
            wagon.setValid(false);
            wagon.setTrain(train);
            train.getWagons().add(wagon);
            wagonJpa.save(wagon);
        }

        LOGGER.info("Added {} wagon(s) with number {} to train conId {}", wcount, numberToUse, conId);
    }

    @Transactional
    public Map<String, Object> updateWagonToTrain(Long id, String conId, String wagonNumber, String product,
            boolean updateTare) {
        Map<String, Object> response = new HashMap<>();

        WagonMod wagon = wagonJpa.findById(id).orElse(null);
        if (wagon == null) {
            response.put("success", false);
            response.put("message", "Wagon not found with id: " + id);
            LOGGER.warn("Wagon not found with id: {}", id);
            return response;
        }

        // Check for duplicate wagon number if new number is provided and non-empty
        if (wagonNumber != null && !wagonNumber.isEmpty()) {
            boolean duplicateExists = wagon.getTrain().getWagons().stream()
                    .filter(w -> !w.getId().equals(id))
                    .anyMatch(w -> wagonNumber.equals(w.getWagonNumber()));
            if (duplicateExists) {
                response.put("success", false);
                response.put("message", "Cannot update wagon — duplicate wagon number: " + wagonNumber);
                LOGGER.warn("Duplicate wagon number {} for wagon id {} in train conId {}", wagonNumber, id, conId);
                return response;
            }
        }

        wagon.setWagonNumber(wagonNumber);
        wagon.setProduct(product);

        BigDecimal weight = wagon.getWeight();

        applyWeightAndTare(wagon, weight, updateTare);

        wagonJpa.save(wagon);
        recalculateTrainTotals(wagon.getTrain());
        trainJpa.save(wagon.getTrain());
        pdfCreator.createPdf(wagon.getTrain());

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
        if (!train.isDone()) {
            LOGGER.warn("Train is not done for conId: {}", conId);
            return;
        }
        recalculateTrainTotals(train);
        updateValidationFlags(train);
        train.setOpen(false);
        train.setWeighingStopDateTime(BasicService.getDateTime());
        trainJpa.save(train);
        pdfCreator.createPdf(train);
        LOGGER.info("Train conId {} closed successfully", conId);
    }

    public Map<String, Object> saveAndSetBlocked(Long id) {

        Map<String, Object> response = new HashMap<>();

        TrainMod train = trainJpa.findById(id).orElse(null);
        if (train == null) {
            response.put("success", false);
            response.put("message", "train not found with id: " + id);
            LOGGER.warn("tarin not found with id: {}", id);
            return response;
        }

        if (!train.isDone() && train.getWagons().size() == 0 && train.isOpen()) {
            response.put("success", false);
            response.put("message", "train is not done");
            LOGGER.warn("train is not done");
            return response;
        }

        recalculateTrainTotals(train);
        updateValidationFlags(train);
        trainJpa.save(train);
        LOGGER.info("train conId {} closed successfully", id);

        response.put("success", true);
        response.put("message", "train closed successfully");
        return response;

    }

    @Transactional
    public void closeTrainAndOpenNewTrain(String conId, String scaleName, int scaleIndex) {
        TrainMod train = trainJpa.findByOpenTrueAndConId(conId).orElse(null);
        if (train == null) {
            LOGGER.warn("No open train found for conId: {}", conId);
            addTrain(conId, scaleName, scaleIndex);
            return;
        }
        if (train.isDone()) {
            train.setOpen(false);

            recalculateTrainTotals(train);
            updateValidationFlags(train);
            trainJpa.save(train);
        } else {
            trainJpa.delete(train);
        }
        addTrain(conId, scaleName, scaleIndex);
        LOGGER.info("Train conId {} closed successfully", conId);
    }

    @Transactional
    public void updateTrain(String conId, String processId, String weighString, String sysDateTime, String maxSpeed,
            String minSpeed,
            int count) {
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
        train.setSysGross(getBigDecimal(weighString));
        train.setSysDateTime(sysDateTime);
        train.setCount(count);
        recalculateTrainTotals(train);
        updateValidationFlags(train);
        trainJpa.save(train);
        LOGGER.info("Train conId {} updated (processId, speeds)", conId);

    }

    @Transactional
    public void updateTrainAndWagons(String conId, String direction, String wighingMethod) {
        TrainMod train = trainJpa.findByOpenTrueAndConId(conId).orElse(null);
        if (train == null) {
            LOGGER.warn("No open train found for conId: {}", conId);
            return;
        }
        train.getWagons().removeIf(w -> !w.isValid());

        train.setDirection(direction);
        train.setWeighingMethod(wighingMethod);
        train.setWeighingStopDateTime(BasicService.getDateTime());
        recalculateTrainTotals(train);
        updateValidationFlags(train);
        train.setDone(true);
        trainJpa.save(train);
        pdfCreator.createPdf(train);

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

    @Transactional(readOnly = true)
    public List<TrainMod> getAllTrainsSortedByDateCreation() {
        return trainJpa.findAllByDoneTrueAndOpenFalseOrderByWeighingStartDateTimeDesc();
    }

    @Transactional(readOnly = true)
    public List<WagonMod> getWagonsOpenAndByConIdAndSortedRow(String conId) {
        return trainJpa.findByOpenTrueAndConId(conId)
                .map(TrainMod::getWagons)
                .orElse(List.of())
                .stream()
                .sorted((w1, w2) -> Integer.compare(w1.getRowNum(), w2.getRowNum()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WagonMod> getWagonsOpenAndByConIdAndSortedRow(Long id) {
        return trainJpa.findById(id)
                .map(TrainMod::getWagons)
                .orElse(List.of())
                .stream()
                .sorted((w1, w2) -> Integer.compare(w1.getRowNum(), w2.getRowNum()))
                .toList();
    }

    public boolean isWorkInProgress(String conId) {
        TrainMod train = trainJpa.findByOpenTrueAndConId(conId).orElse(null);
        return train != null && !train.isDone();
    }

    @Transactional
    public void deleteTrainByConId(String conId) {
        TrainMod train = trainJpa.findByOpenTrueAndConId(conId).orElse(null);
        if (train != null) {
            trainJpa.delete(train);
            LOGGER.info("Train with conId {} deleted successfully", conId);
        } else {
            LOGGER.warn("No open train found to delete with conId: {}", conId);
        }
    }

    @Transactional
    public Long getIdOpenTrain(String conId) {
        return trainJpa.findByOpenTrueAndConId(conId).map(TrainMod::getId).orElse(null);
    }

    public TrainMod getTrainById(Long id) {
        return trainJpa.findById(id).orElse(null);
    }

    @Transactional
    public void processOcrResult(Long trainId, Integer totalWagons, List<Map<String, Object>> wagonsData) {
        BigDecimal minQuality = new BigDecimal("50.0"); // უკეთესია String-ით ინიციალიზაცია

        TrainMod train = trainJpa.findById(trainId).orElse(null);
        if (train == null) {
            LOGGER.warn("Train not found for OCR processing: trainId={}", trainId);
            return;
        }

        LOGGER.info("OCR processing started | trainId={}, wagons in DB={}, reported total={}",
                trainId, train.getWagons().size(), totalWagons);

        if (!Objects.equals(train.getWagons().size(), totalWagons)) {
            LOGGER.warn("Wagon count mismatch → DB: {}, OCR reports: {}. Processing continues anyway.",
                    train.getWagons().size(), totalWagons);
            return;
        }

        int updatedCount = 0;
        int skippedDueToQuality = 0;
        int skippedAlreadyNumbered = 0;

        for (Map<String, Object> wagonData : wagonsData) {
            Integer row = (Integer) wagonData.get("row");
            String number = (String) wagonData.get("number");
            String qualityStr = (String) wagonData.get("quality");

            if (row == null || number == null || number.trim().isEmpty() || qualityStr == null) {
                LOGGER.warn("Invalid wagon data skipped | row={}, number='{}', quality='{}'",
                        row, number, qualityStr);
                continue;
            }

            BigDecimal qualityValue;
            try {
                qualityValue = new BigDecimal(qualityStr.trim());
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid quality format for row {}: '{}' → skipping", row, qualityStr);
                continue;
            }

            boolean found = false;
            for (WagonMod wm : train.getWagons()) {
                if (wm.getRowNum() == row.intValue()) {
                    String oldNumber = wm.getWagonNumber();

                    if (oldNumber != null && !oldNumber.trim().isEmpty()) {
                        LOGGER.debug("Row {} already has wagon number → skipping | number={}", row, oldNumber);
                        skippedAlreadyNumbered++;
                        found = true;
                        break;
                    }

                    String newNum = number.trim();

                    if (qualityValue.compareTo(minQuality) >= 0) {
                        wm.setWagonNumber(newNum);
                        applyWeightAndTare(wm, wm.getWeight(), true);
                        wagonJpa.save(wm);

                        LOGGER.info("OCR updated wagon | row={}, old='{}' → new='{}', quality={} (>= {})",
                                row, oldNumber, newNum, qualityValue, minQuality);
                        updatedCount++;
                    } else {
                        LOGGER.info("Row {} skipped due to low quality | quality={} < min={}",
                                row, qualityValue, minQuality);
                        skippedDueToQuality++;
                    }

                    found = true;
                    break;
                }
            }

            if (!found) {
                LOGGER.warn("Row {} not found in train {} | available rows: {}",
                        row, trainId, train.getWagons().stream()
                                .map(WagonMod::getRowNum)
                                .sorted()
                                .toList());
            }
        }

        // საბოლოო შედეგის ლოგირება
        LOGGER.info(
                "OCR processing completed for train {} | updated: {}, skipped (already numbered): {}, skipped (low quality): {}",
                trainId, updatedCount, skippedAlreadyNumbered, skippedDueToQuality);

        if (updatedCount > 0) {
            recalculateTrainTotals(train);
            updateValidationFlags(train);
            trainJpa.save(train);
            LOGGER.info("Train {} updated and saved after OCR processing", trainId);
        } else {
            LOGGER.info("No changes applied after OCR processing for train {}", trainId);
        }

        try {
            emitterServic.sendToScale(train.getConId(), "update-data-container");
            LOGGER.debug("Update signal sent to scale for conId={}", train.getConId());
        } catch (Exception e) {
            LOGGER.error("Failed to send update signal to scale for conId={}", train.getConId(), e);
        }
    }
    // ────────────────────────────────────────────────
    // დამხმარე მეთოდები
    // ────────────────────────────────────────────────

    private void updateValidationFlags(TrainMod train) {
        if (train.getWagons().isEmpty()) {
            train.setNormalWeight(true);
            train.setNormalSpeed(true);
            train.setAllwagonsNumbered(true);
            train.setTareOnly(false);
            train.setMatched(true);
            train.setBlocked(false);
            return;
        }

        boolean allValid = train.getWagons().stream().allMatch(WagonMod::isValid);
        if (!allValid) {
            // შეიძლება განსხვავებული ლოგიკა, თუ არ გჭირდება valid-ების იგნორირება
        }

        train.setNormalWeight(
                train.getWagons().stream()
                        .allMatch(w -> w.getWeight() != null && w.getWeight().compareTo(UnitService.WEIGHT_LIMIT) < 0));

        train.setNormalSpeed(
                train.getWagons().stream().allMatch(w -> w.getSpeed() != null
                        && new BigDecimal(w.getSpeed()).compareTo(UnitService.SPEED_LIMIT) < 0));

        train.setAllwagonsNumbered(
                train.getWagons().stream().allMatch(
                        w -> w.getWagonNumber() != null && w.getWagonNumber().length() == UnitService.W_NUM_LEN));

        train.setTareOnly(
                train.getWagons().stream()
                        .allMatch(w -> w.getWeight() != null && w.getWeight().compareTo(UnitService.TARE_LIMIT) <= 0));

        train.setMatched(
                train.getWagons().stream()
                        .allMatch(w -> w.getNeto() != null && w.getNeto().compareTo(BigDecimal.ZERO) > 0)
                        || train.isTareOnly());

        train.setBlocked(
                train.isAllwagonsNumbered());
    }

    private void applyWeightAndTare(WagonMod wagon, BigDecimal weight, boolean updateTare) {
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
                updateLast30DaysTrains(num, weight);
            }
        } else {
            String num = wagon.getWagonNumber();
            wagon.setTare(num != null && !num.isEmpty() ? wtareService.getTareByNumber(num) : null);
        }
    }

    private void updateLast30DaysTrains(String wagonNumber, BigDecimal tare) {
        List<TrainMod> trainsToCheck = trainJpa.findAllByTareOnlyFalseAndAllwagonsNumberedTrueAndOpenFalse();

        for (TrainMod train : trainsToCheck) {
            if (isWeighingWithinLast30Days(train.getWeighingStopDateTime())) {
                List<WagonMod> wagons = new ArrayList<>(train.getWagons());
                for (WagonMod wagon : wagons) {
                    if (wagon.getWagonNumber().equals(wagonNumber)) {
                        wagon.setTare(tare);
                        wagonJpa.save(wagon);
                        recalculateTrainTotals(train);
                        updateValidationFlags(train);
                        trainJpa.save(train);
                        LOGGER.info("Train with Id {} UPDATED successfully", train.getId());
                    }
                }
            }
        }
    }

    private void recalculateTrainTotals(TrainMod train) {
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal tareSum = BigDecimal.ZERO;
        boolean allValidHaveTare = !train.getWagons().isEmpty();

        for (WagonMod w : train.getWagons()) {
            if (!w.isValid())
                continue;

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
        for (WagonMod w : train.getWagons()) {
            if (!w.isValid())
                continue;

            BigDecimal weight = w.getWeight() != null ? w.getWeight() : BigDecimal.ZERO;
            BigDecimal wagonTare = w.getTare();
            if (wagonTare != null && wagonTare.compareTo(BigDecimal.ZERO) > 0) {
                w.setNeto(weight.subtract(wagonTare).max(BigDecimal.ZERO));
            } else {
                w.setNeto(null);
            }
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

    /**
     * ამოწმებს, არის თუ არა აწონვის დრო ბოლო 30 დღის განმავლობაში
     * 
     * @param systemDateTime   სისტემის მიმდინარე დრო (String) მაგ:
     *                         BasicService.getDateTime()
     * @param weighingDateTime მატარებლის აწონვის დრო (String) მაგ:
     *                         train.getWeighingStopDateTime()
     * @return true თუ აწონვა მოხდა ბოლო 30 დღის (ან ნაკლები) განმავლობაში
     *         false თუ 30+ დღეა გასული ან თუ თარიღები არასწორია
     */
    private boolean isWeighingWithinLast30Days(String weighingDateTime) {
        DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        try {
            LocalDateTime now = LocalDateTime.parse(BasicService.getDateTime(), FORMATTER);
            LocalDateTime weighing = LocalDateTime.parse(weighingDateTime, FORMATTER);

            // თუ აწონვის დრო მომავალშია → ჩვეულებრივ false (შეგიძლია შეცვალო ლოგიკა)
            if (weighing.isAfter(now)) {
                return false;
            }

            // დღეების სხვაობა (ჩათვლით)
            long daysBetween = ChronoUnit.DAYS.between(weighing, now);

            // 30 დღე = 30 დღე + დღევანდელი დღე → daysBetween <= 30
            return daysBetween <= UnitService.UPDATE_DAYS_LIMIT;

        } catch (Exception e) {
            // თუ ფორმატი არასწორია ან სხვა პრობლემა
            // შეგიძლია log-ზე გადააგდო ან განსხვავებული ლოგიკა გამოიყენო
            LOGGER.error("Failed to check if weighing is within last 30 days", e);
            return false;
        }
    }

}