package com.jaba.awr_3.core.units;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jaba.awr_3.inits.repo.RepoInit;


@Service
public class UnitService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnitService.class);
    private static final File UNITS_JSON = new File(RepoInit.SERVER_SETTINGS_REPO, "units.json");
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    public static String SPEED_UNIT;
    public static String WEIGHT_UNIT;
    public static int W_NUM_LEN;
    public static BigDecimal TARE_LIMIT;
    public static BigDecimal WEIGHT_LIMIT;
    public static BigDecimal SPEED_LIMIT;

    public static void initUnitS() {
        if (!UNITS_JSON.exists()) {
            try {
                UNITS_JSON.createNewFile();
                UnitMod uMod = new UnitMod();
                uMod.setSpeedUnit("km/h");
                SPEED_UNIT = "km/h";
                uMod.setWeightUnit("tonne");
                WEIGHT_UNIT = "tonne";
                uMod.setWagonNumLenUnit(8);
                W_NUM_LEN = 8;
                uMod.setTarLimit(new BigDecimal("29.0"));
                TARE_LIMIT = new BigDecimal("29.0");
                uMod.setWeightLimit(new BigDecimal("120.0"));
                WEIGHT_LIMIT = new BigDecimal("120.0");
                uMod.setSpeedLimit(new BigDecimal("5.0"));
                SPEED_LIMIT = new BigDecimal("5.0");
                MAPPER.writeValue(UNITS_JSON, uMod);
                LOGGER.info(" write UNITS to JSON COMPLECTE ");
            } catch (Exception e) {
                LOGGER.error("Error write UNITS to JSON: {}", e.getMessage(), e);
            }
        } else {
            try {
                UnitMod uMod = MAPPER.readValue(UNITS_JSON, UnitMod.class);
                SPEED_UNIT = uMod.getSpeedUnit();
                WEIGHT_UNIT = uMod.getWeightUnit();
                W_NUM_LEN = uMod.getWagonNumLenUnit();
                TARE_LIMIT = uMod.getTarLimit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public UnitMod getUnit() {
        UnitMod uMod;
        try {
            uMod = MAPPER.readValue(UNITS_JSON, UnitMod.class);
        } catch (Exception e) {
            uMod = null;
        }
        return uMod;
    }

    public Map<String, Object> updateUnits(String speedUnit, String weightUnit, int wagonLen, String tareLimit) {
        Map<String, Object> respons = new HashMap<>();
        try {
            UnitMod uMod = new UnitMod();
            uMod.setSpeedUnit(speedUnit);
            SPEED_UNIT = speedUnit;
            uMod.setWeightUnit(weightUnit);
            WEIGHT_UNIT = weightUnit;
            uMod.setWagonNumLenUnit(wagonLen);
            W_NUM_LEN = wagonLen;
            uMod.setTarLimit(new BigDecimal(tareLimit));
            TARE_LIMIT = new BigDecimal(tareLimit);
            MAPPER.writeValue(UNITS_JSON, uMod);
            respons.put("success", true);
        } catch (Exception e) {
            LOGGER.error("Error UPDATE UNITS {}", e.getMessage(), e);
            respons.put("success", false);
        }
        return respons;
    }

}
