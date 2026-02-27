package com.jaba.awr_3.seversettings.owner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.awt.image.BufferedImage;
import java.nio.file.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jaba.awr_3.inits.repo.RepoInit;

@Service
public class OwnerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OwnerService.class);
    private static final File OWNER_SETTINGS_JSON = new File(RepoInit.SERVER_SETTINGS_REPO, "ownersettings.json");
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public static String NAME;
    public static String EMAIL;
    public static String ADDRESS;
    public static String SERIAL;
    public static String LICENZI;

    public static void initOwnerSettings() {
        if (!OWNER_SETTINGS_JSON.exists()) {
            try {
                OWNER_SETTINGS_JSON.createNewFile();
                OwnerMod oMod = new OwnerMod();
                oMod.setName("");
                NAME = "";
                oMod.setAddress("");
                ADDRESS = "";
                oMod.setEmail("");
                EMAIL = "";
                oMod.setLicenzi("DEMO");
                LICENZI = "DEMO";
                oMod.setSerial("");
                MAPPER.writeValue(OWNER_SETTINGS_JSON, oMod);
                LOGGER.info("Default OWNER settings created at {}", OWNER_SETTINGS_JSON.getAbsolutePath());
            } catch (Exception e) {
                LOGGER.error("Failed to create OWNER SETTINGS {}", e);
            }
        } else {
            try {
                OwnerMod oMod = MAPPER.readValue(OWNER_SETTINGS_JSON, OwnerMod.class);
                NAME = oMod.getName();
                ADDRESS = oMod.getAddress();
                EMAIL = oMod.getEmail();
                SERIAL = oMod.getSerial();
                LICENZI = oMod.getLicenzi();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public Map<String, Object> updateOwner(String name, String email, String address, String serial, String licenzi) {
        Map<String, Object> respons = new HashMap<>();
        OwnerMod oMod = new OwnerMod();
        oMod.setName(name);
        NAME = name;
        oMod.setEmail(email);
        EMAIL = email;
        oMod.setAddress(address);
        ADDRESS = address;
        oMod.setSerial(serial);
        SERIAL = serial;
        oMod.setLicenzi(licenzi);
        LICENZI = licenzi;
        try {
            MAPPER.writeValue(OWNER_SETTINGS_JSON, oMod);
            respons.put("success", true);
        } catch (Exception e) {
            respons.put("success", false);
            LOGGER.error("Failed to create OWNER SETTINGS {}", e);
        }
        return respons;
    }

    public OwnerMod getOwner() {
        OwnerMod oMod;
        try {
            oMod = MAPPER.readValue(OWNER_SETTINGS_JSON, OwnerMod.class);
        } catch (Exception e) {
            oMod = null;
            LOGGER.error("Failed to Find OWNER {}", e);
        }
        return oMod;
    }

    public void uploadOwnerLogo(MultipartFile uploadFile) {

        if (uploadFile == null || uploadFile.isEmpty()) {
            throw new IllegalArgumentException("File is empty.");
        }

        // მაქსიმალური ზომა (მაგ: 5MB)
        long maxSize = 5 * 1024 * 1024;
        if (uploadFile.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds 5MB.");
        }

        try {

            // 1️⃣ ვამოწმებთ რომ რეალური image იყოს
            BufferedImage originalImage = ImageIO.read(uploadFile.getInputStream());
            if (originalImage == null) {
                throw new IllegalArgumentException("Invalid image file.");
            }


            // 3️⃣ დირექტორიის შექმნა თუ არ არსებობს
            Path uploadDir = Paths.get(RepoInit.LOGO_REPO.getAbsolutePath());
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            Path filePath = uploadDir.resolve("logo.png");

            // 4️⃣ ყოველთვის ვინახავთ როგორც PNG
            ImageIO.write(originalImage, "png", filePath.toFile());

        } catch (IOException e) {
            throw new RuntimeException("Logo upload failed.", e);
        }
    }

}
