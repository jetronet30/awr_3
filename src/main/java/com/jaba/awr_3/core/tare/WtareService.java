package com.jaba.awr_3.core.tare;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.jaba.awr_3.core.units.UnitService;
import com.jaba.awr_3.seversettings.basic.BasicService;

import lombok.RequiredArgsConstructor;

/**
 * Service class for managing wagon tare weights.
 * Provides CRUD operations with validation, logging, and transaction safety.
 */
@Service
@RequiredArgsConstructor
public class WtareService {

    private final WtareRepo wRepo;
    private static final Logger LOGGER = LoggerFactory.getLogger(WtareService.class);

    // Regex: allows alphanumeric, hyphens, up to 20 characters (adjust as needed)
    private static final Pattern WAGON_NUMBER_PATTERN = Pattern.compile("^[A-Za-z0-9-]{1,20}$");
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    // ========================================================================
    // === ADD OR UPDATE WAGON TARE (AUTO TIMESTAMP) WITH LIMIT & ZERO CHECK ===
    // ========================================================================
    @Transactional
    public void addOrUpdateWtare(String tare, String wagonNumber, boolean rightToUpdate) {
        LOGGER.info("Conditional tare update request → wagon: '{}', rightToUpdate: {}", wagonNumber, rightToUpdate);

        // === 1. Validate wagon number ===
        if (!isValidWagonNumber(wagonNumber)) {
            LOGGER.warn("Invalid or missing wagon number: '{}'", wagonNumber);
            return;
        }
        String trimmedWagonNumber = wagonNumber.trim();

        // === 2. Validate tare string ===
        if (!StringUtils.hasText(tare)) {
            LOGGER.warn("Tare weight is null or empty for wagon: '{}'", trimmedWagonNumber);
            return;
        }
        String trimmedTare = tare.trim();

        // === 3. Parse tare weight safely ===
        BigDecimal tareWeight = parseTareWeight(trimmedTare);
        if (tareWeight == null) {
            LOGGER.error("Invalid tare format: '{}'. Update skipped for wagon: '{}'", trimmedTare, trimmedWagonNumber);
            return;
        }

        // === 4. ZERO CHECK: DO NOT ALLOW tareWeight == 0 ===
        if (tareWeight.compareTo(BigDecimal.ZERO) == 0) {
            LOGGER.info("Tare weight is zero (0.0) → update skipped for wagon: '{}'", trimmedWagonNumber);
            return;
        }

        // === 5. Get limit ===
        BigDecimal tareLimit = UnitService.TARE_LIMIT;
        if (tareLimit == null) {
            LOGGER.error("UnitService.TARE_LIMIT is null. Update skipped for wagon: '{}'", trimmedWagonNumber);
            return;
        }

        // === 6. Business logic: rightToUpdate + below limit ===
        boolean shouldUpdate = rightToUpdate && tareWeight.compareTo(tareLimit) < 0;

        if (shouldUpdate) {
            String currentDateTime = BasicService.getDateTime();
            addOrUpdateWtareInternal(trimmedTare, trimmedWagonNumber, currentDateTime);
            LOGGER.info("Tare updated (valid & below limit) → wagon: '{}', tare: {} < {}",
                    trimmedWagonNumber, tareWeight, tareLimit);
        } else {
            LOGGER.info("Update skipped → wagon: '{}', tare: {} {}, limit: {}, rightToUpdate: {}",
                    trimmedWagonNumber, tareWeight,
                    rightToUpdate ? ">=" : "N/A",
                    tareLimit, rightToUpdate);
        }
    }

    // ========================================================================
    // === ADD OR UPDATE WAGON TARE (AUTO TIMESTAMP) ===
    // ========================================================================
    @Transactional
    public void addOrUpdateWtare(String tare, String wagonNumber) {
        String currentDateTime = BasicService.getDateTime();
        addOrUpdateWtareInternal(tare, wagonNumber, currentDateTime);
    }

    // ========================================================================
    // === ADD OR UPDATE WAGON TARE (CUSTOM TIMESTAMP) ===
    // ========================================================================
    @Transactional
    public void addOrUpdateWtare(String tare, String wagonNumber, String upDate) {
        addOrUpdateWtareInternal(tare, wagonNumber, upDate);
    }

    /**
     * Internal method to handle both add and update logic with full validation.
     */
    private void addOrUpdateWtareInternal(String tare, String wagonNumber, String upDate) {
        LOGGER.info("Attempting to add or update tare for wagon number: {}", wagonNumber);

        // Validate wagonNumber
        if (!isValidWagonNumber(wagonNumber)) {
            LOGGER.warn("Invalid wagon number format: {}", wagonNumber);
            throw new IllegalArgumentException("Wagon number must be 1-20 alphanumeric characters or hyphens.");
        }

        String trimmedWagonNumber = wagonNumber.trim();
        BigDecimal tareWeight = parseTareWeight(tare);

        // Validate upDate (basic non-empty check)
        if (!StringUtils.hasText(upDate)) {
            LOGGER.warn("Update date is empty for wagon: {}", trimmedWagonNumber);
            throw new IllegalArgumentException("Update date cannot be empty.");
        }

        // === NEW: BLOCK NEGATIVE TARE VALUES FROM BEING SAVED ===
        if (tareWeight != null && tareWeight.compareTo(BigDecimal.ZERO) < 0) {
            LOGGER.warn("Negative tare weight provided: {} – saving blocked for wagon: {}", tareWeight,
                    trimmedWagonNumber);
            return; // Do not save or update
        }

        Optional<WtareMod> existingOpt = wRepo.findByWagonNumber(trimmedWagonNumber);

        if (existingOpt.isPresent()) {
            WtareMod existing = existingOpt.get();
            LOGGER.info("Updating existing tare for wagon: {}", trimmedWagonNumber);
            existing.setTareWeight(tareWeight);
            existing.setUpDate(upDate);
            wRepo.save(existing);
            LOGGER.info("Successfully updated tare for wagon: {}", trimmedWagonNumber);
        } else {
            LOGGER.info("Creating new tare record for wagon: {}", trimmedWagonNumber);
            WtareMod newWtare = new WtareMod();
            newWtare.setWagonNumber(trimmedWagonNumber);
            newWtare.setTareWeight(tareWeight);
            newWtare.setUpDate(upDate);
            wRepo.save(newWtare);
            LOGGER.info("Successfully created new tare for wagon: {}", trimmedWagonNumber);
        }
    }

    // ========================================================================
    // === GET ALL TARES SORTED BY WAGON NUMBER ===
    // ========================================================================
    @Transactional(readOnly = true)
    public List<WtareMod> getTaresSortedByNumber() {
        LOGGER.info("Fetching all tare records sorted by wagon number");
        List<WtareMod> tares = wRepo.findAllByOrderByWagonNumberAsc();
        LOGGER.info("Retrieved {} tare records", tares.size());
        return tares;
    }

    // ========================================================================
    // === GET TARE WEIGHT BY WAGON NUMBER ===
    // ========================================================================
    @Transactional(readOnly = true)
    public BigDecimal getTareByNumber(String wagonNumber) {
        if (!isValidWagonNumber(wagonNumber)) {
            LOGGER.warn("Invalid wagon number in getTareByNumber: {}", wagonNumber);
            return ZERO;
        }

        String trimmedWagonNumber = wagonNumber.trim();
        LOGGER.info("Looking up tare weight for wagon: {}", trimmedWagonNumber);

        return wRepo.findByWagonNumber(trimmedWagonNumber)
                .map(wtare -> {
                    LOGGER.info("Tare weight found: {} for wagon: {}", wtare.getTareWeight(), trimmedWagonNumber);
                    return wtare.getTareWeight() != null ? wtare.getTareWeight() : ZERO;
                })
                .orElseGet(() -> {
                    LOGGER.info("No tare weight found for wagon: {}", trimmedWagonNumber);
                    return ZERO;
                });
    }

    // ========================================================================
    // === HELPER: VALIDATE WAGON NUMBER ===
    // ========================================================================
    private boolean isValidWagonNumber(String wagonNumber) {
        return StringUtils.hasText(wagonNumber)
                && WAGON_NUMBER_PATTERN.matcher(wagonNumber.trim()).matches();
    }

    // ========================================================================
    // === HELPER: PARSE TARE WEIGHT SAFELY ===
    // ========================================================================
    private BigDecimal parseTareWeight(String tare) {
        if (!StringUtils.hasText(tare)) {
            LOGGER.warn("Tare weight is null or empty – setting to NULL in DB");
            return null; // Allow NULL in DB for "not set"
        }

        try {
            BigDecimal weight = new BigDecimal(tare.trim());
            return weight;
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid tare weight format: '{}'. Storing as NULL.", tare);
            return null;
        }
    }
}