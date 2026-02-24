package com.jaba.awr_3.core.numberdetection.ocr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jaba.awr_3.inits.repo.RepoInit;

@Service
public class OcrService {

    private static final File OCR_SETTINGS = new File(RepoInit.SERVER_SETTINGS_REPO, "ocrsettings.json");
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final Logger LOGGER = LoggerFactory.getLogger(OcrService.class);

    // Thread-safe file access
    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();

    // === Initialization (static, thread-safe) ===
    public static void initOcr() {
        LOCK.writeLock().lock();
        try {
            if (OCR_SETTINGS.exists()) {
                LOGGER.info("ocrsettings.json already exists: {}", OCR_SETTINGS.getAbsolutePath());
                return;
            }

            List<OcrMod> ocrModels = new ArrayList<>();

            for (int i = 0; i < 20; i++) {
                OcrMod model = createDefaultOcr(
                    i,
                    "admin", "admin",
                    "password123", "password123",
                    554, 554,
                    String.format("rtsp://192.168.1.%d:554/stream1", 100 + i),
                    String.format("rtsp://192.168.1.%d:554/stream1", 120 + i),
                    0.1, 0.1, 0.9, 0.9,
                    false,          // activeDetection
                    false           // activeStream
                );
                ocrModels.add(model);
            }

            MAPPER.writeValue(OCR_SETTINGS, ocrModels);
            LOGGER.info("ocrsettings.json created successfully: {} entries, path: {}", 
                    ocrModels.size(), OCR_SETTINGS.getAbsolutePath());

        } catch (IOException e) {
            LOGGER.error("Failed to create ocrsettings.json", e);
        } catch (Exception ex) {
            LOGGER.error("Unexpected error during OCR initialization", ex);
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    private static OcrMod createDefaultOcr(int index,
            String cam1Usr, String cam2Usr,
            String cam1Passwd, String cam2Passwd,
            int cam1Port, int cam2Port,
            String rtspUrl1, String rtspUrl2,
            double roiX1, double roiY1, double roiX2, double roiY2,
            boolean activeDetection, boolean activeStream) {
        
        OcrMod mod = new OcrMod();
        mod.setIndex(index);
        mod.setCam1Usr(cam1Usr);
        mod.setCam2Usr(cam2Usr);
        mod.setCam1Passwd(cam1Passwd);
        mod.setCam2Passwd(cam2Passwd);
        mod.setCam1Port(cam1Port);
        mod.setCam2Port(cam2Port);
        mod.setRtspUrl_1(rtspUrl1);
        mod.setRtspUrl_2(rtspUrl2);
        mod.setRoiX1(roiX1);
        mod.setRoiY1(roiY1);
        mod.setRoiX2(roiX2);
        mod.setRoiY2(roiY2);
        mod.setActiveDetection(activeDetection);   
        mod.setActiveStream(activeStream);
        return mod;
    }

    // === Safe file read ===
    private List<OcrMod> readOcrModels() throws IOException {
        LOCK.readLock().lock();
        try {
            if (!OCR_SETTINGS.exists()) {
                LOGGER.warn("ocrsettings.json does not exist");
                return new ArrayList<>();
            }
            return MAPPER.readValue(OCR_SETTINGS, new TypeReference<List<OcrMod>>() {});
        } finally {
            LOCK.readLock().unlock();
        }
    }

    // === Safe file write ===
    private void writeOcrModels(List<OcrMod> models) throws IOException {
        LOCK.writeLock().lock();
        try {
            MAPPER.writeValue(OCR_SETTINGS, models);
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    // === List all ===
    public List<OcrMod> listOcrModels() {
        try {
            List<OcrMod> models = readOcrModels();
            LOGGER.debug("OCR models list requested: {} entries", models.size());
            return models;
        } catch (IOException e) {
            LOGGER.error("Failed to read OCR models list", e);
            return new ArrayList<>();
        }
    }

    public OcrMod getOcrByIndex(int index) {
        for (OcrMod mod : listOcrModels()) {
            if (mod.getIndex() == index) {
                LOGGER.debug("OCR model found by index: {}", index);
                return mod;
            }
        }
        LOGGER.debug("OCR model not found by index: {}", index);
        return null;
    }

    // === Full update by index ===
    public Map<String, Object> updateOcrByIndex(
            int index,
            String cam1Usr, String cam2Usr,
            String cam1Passwd, String cam2Passwd,
            int cam1Port, int cam2Port,
            String rtspUrl1, String rtspUrl2,
            double roiX1, double roiY1, double roiX2, double roiY2,
            boolean activeDetection, boolean activeStream) {

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);

        if (index < 0 || index > 19) {
            response.put("error", "Index must be between 0 and 19");
            return response;
        }
        if (cam1Port < 1 || cam1Port > 65535 || cam2Port < 1 || cam2Port > 65535) {
            response.put("error", "Invalid port number(s)");
            return response;
        }
        if (roiX1 < 0 || roiX1 > 1 || roiY1 < 0 || roiY1 > 1 ||
            roiX2 < 0 || roiX2 > 1 || roiY2 < 0 || roiY2 > 1 ||
            roiX1 >= roiX2 || roiY1 >= roiY2) {
            response.put("error", "Invalid ROI coordinates");
            return response;
        }

        try {
            List<OcrMod> models = readOcrModels();
            boolean found = false;

            for (OcrMod mod : models) {
                if (mod.getIndex() == index) {
                    mod.setCam1Usr(cam1Usr.trim());
                    mod.setCam2Usr(cam2Usr.trim());
                    mod.setCam1Passwd(cam1Passwd.trim());
                    mod.setCam2Passwd(cam2Passwd.trim());
                    mod.setCam1Port(cam1Port);
                    mod.setCam2Port(cam2Port);
                    mod.setRtspUrl_1(rtspUrl1.trim());
                    mod.setRtspUrl_2(rtspUrl2.trim());
                    mod.setRoiX1(roiX1);
                    mod.setRoiY1(roiY1);
                    mod.setRoiX2(roiX2);
                    mod.setRoiY2(roiY2);
                    mod.setActiveDetection(activeDetection);
                    mod.setActiveStream(activeStream);
                    found = true;
                    break;
                }
            }

            if (!found) {
                response.put("error", "OCR model not found with index: " + index);
                return response;
            }

            writeOcrModels(models);
            response.put("success", true);
            response.put("message", "OCR settings updated successfully for index " + index);

        } catch (IOException e) {
            LOGGER.error("Failed to update OCR model index {}", index, e);
            response.put("error", "File write error");
        }

        return response;
    }

    // === Toggle activeDetection ===
    public Map<String, Object> setActiveDetection(int index, boolean active) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);

        try {
            List<OcrMod> models = readOcrModels();
            boolean found = false;

            for (OcrMod mod : models) {
                if (mod.getIndex() == index) {
                    mod.setActiveDetection(active);
                    found = true;
                    break;
                }
            }

            if (!found) {
                response.put("error", "OCR model not found: index " + index);
                return response;
            }

            writeOcrModels(models);
            response.put("success", true);
            response.put("message", active ? "Detection enabled" : "Detection disabled");

        } catch (IOException e) {
            LOGGER.error("Failed to toggle activeDetection for index {}", index, e);
            response.put("error", "Update failed");
        }

        return response;
    }

    // === Toggle activeStream ===
    public Map<String, Object> setActiveStream(int index, boolean active) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);

        try {
            List<OcrMod> models = readOcrModels();
            boolean found = false;

            for (OcrMod mod : models) {
                if (mod.getIndex() == index) {
                    mod.setActiveStream(active);
                    found = true;
                    break;
                }
            }

            if (!found) {
                response.put("error", "OCR model not found: index " + index);
                return response;
            }

            writeOcrModels(models);
            response.put("success", true);
            response.put("message", active ? "Stream enabled" : "Stream disabled");

        } catch (IOException e) {
            LOGGER.error("Failed to toggle activeStream for index {}", index, e);
            response.put("error", "Update failed");
        }

        return response;
    }

    // === Check status ===
    public boolean isDetectionActive(int index) {
        OcrMod mod = getOcrByIndex(index);
        return mod != null && mod.isActiveDetection();
    }

    public boolean isStreamActive(int index) {
        OcrMod mod = getOcrByIndex(index);
        return mod != null && mod.isActiveStream();
    }
}