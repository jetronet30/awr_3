package com.jaba.awr_3.seversettings.basic;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jaba.awr_3.inits.repo.RepoInit;



@Service
public class BasicService {
    private static final Logger logger = LoggerFactory.getLogger(BasicService.class);
    private static final File BASIC_SETTINGS_JSON = new File(RepoInit.SERVER_SETTINGS_REPO, "basicsettings.json");
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final DateTimeFormatter FORMATTER_FOR_FILE_NAME = DateTimeFormatter.ofPattern("_yyyy_MM_dd_HH_mm");
    private static final DateTimeFormatter FORMATTER_DATE = DateTimeFormatter.ofPattern(" yyyy/MM/dd");
    private static final DateTimeFormatter FORMATTER_DATE_TIME = DateTimeFormatter.ofPattern(" yyyy/MM/dd HH:mm:ss");

    public static String LANGUAGE;
    public static String DOCLANGUAGE;
    public static String TIMEZONE;
    public static int LISTING_PORT;
    public static String LISTING_ADDRESS;

    public static void initBasicSettings() {
        if (!BASIC_SETTINGS_JSON.exists()) {
            try {
                BasicMod bMod = new BasicMod();
                bMod.setLanguage("en");
                LANGUAGE = "en";
                bMod.setDocLanguage("en");
                DOCLANGUAGE = "en";
                bMod.setTimeZone("Asia/Tbilisi");
                TIMEZONE = "Asia/Tbilisi";
                bMod.setListingPort(8087);
                LISTING_PORT = 8087;
                bMod.setListingAddress("0.0.0.0");
                LISTING_ADDRESS = "0.0.0.0";
                MAPPER.writeValue(BASIC_SETTINGS_JSON, bMod);
                setTimeZoneInSys("Asia/Tbilisi");
                logger.info("Error write to JSON: {}");
            } catch (Exception e) {
                logger.error("Error write to JSON: {}", e.getMessage(), e);
            }
        } else {
            try {
                BasicMod bMod = MAPPER.readValue(BASIC_SETTINGS_JSON, BasicMod.class);
                LANGUAGE = bMod.getLanguage();
                DOCLANGUAGE = bMod.getDocLanguage();
                TIMEZONE = bMod.getTimeZone();
                LISTING_PORT = bMod.getListingPort();
                LISTING_ADDRESS = bMod.getListingAddress();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Map<String, Object> updateBasic(String language, String docLanguage , String timeZone, int port, String ip) {
        Map<String, Object> respons = new HashMap<>();
        try {
            BasicMod bMod = new BasicMod();
            bMod.setLanguage(language);
            LANGUAGE = language;
            bMod.setDocLanguage(docLanguage);
            DOCLANGUAGE = docLanguage;
            bMod.setTimeZone(timeZone);
            TIMEZONE = timeZone;
            bMod.setListingPort(port);
            LISTING_PORT = port;
            bMod.setListingAddress(ip);
            LISTING_ADDRESS = ip;
            MAPPER.writeValue(BASIC_SETTINGS_JSON, bMod);
            respons.put("success", true);
            setTimeZoneInSys(timeZone);
        } catch (Exception e) {
            respons.put("success", true);
            logger.error("Error UPDATE BASIC SETTINGS : {}", e.getMessage(), e);
        }
        return respons;
    }

    public static String getDateTimeForFileName() {
        ZonedDateTime zDateTime = ZonedDateTime.now(ZoneId.of(TIMEZONE));
        return zDateTime.format(FORMATTER_FOR_FILE_NAME);
    }

    public static String getDateTime() {
        ZonedDateTime zDateTime = ZonedDateTime.now(ZoneId.of(TIMEZONE));
        return zDateTime.format(FORMATTER_DATE_TIME);
    }

    public static String getDate() {
        ZonedDateTime zDateTime = ZonedDateTime.now(ZoneId.of(TIMEZONE));
        return zDateTime.format(FORMATTER_DATE);
    }

    public BasicMod getBasicSettings() {
        BasicMod bMod;
        try {
            bMod = MAPPER.readValue(BASIC_SETTINGS_JSON, BasicMod.class);
        } catch (Exception e) {
            bMod = null;
        }
        return bMod;
    }

    private static void setTimeZoneInSys(String timezone) {
        try {
            String[] cmd = { "timedatectl", "set-timezone", timezone };
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Time zone changed to: " + timezone);
            } else {
                System.err.println("Failed to change time zone. Exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}
