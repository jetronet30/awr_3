package com.jaba.awr_3.core.tare;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jaba.awr_3.inits.repo.RepoInit;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MDBreader {

    private static final File MDB_FILE = new File(RepoInit.BACKUP_UPLOAD_REPO, "MainDB.MDB");
    private static final String TABLE_NAME = "WagonTares";
    private final WtareService wService;

    // ========================================================================
    // === UPLOAD AND PROCESS .MDB FILE ===
    // ========================================================================
    public void uploadBackup(MultipartFile uploadFile) {
        if (uploadFile == null || uploadFile.isEmpty()) {
            log.warn("Uploaded file is null or empty");
            return;
        }

        try {
            // Clean and prepare directory
            FileUtils.cleanDirectory(RepoInit.BACKUP_UPLOAD_REPO);
            Files.createDirectories(Paths.get(RepoInit.BACKUP_UPLOAD_REPO.toURI()));

            File outputFile = new File(RepoInit.BACKUP_UPLOAD_REPO, "MainDB.MDB");
            if (outputFile.exists()) {
                log.info("MDB file already exists, overwriting: {}", outputFile.getName());
            }

            // Save uploaded file
            try (InputStream in = uploadFile.getInputStream();
                 OutputStream out = new FileOutputStream(outputFile)) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            log.info("MDB file saved successfully: {}", outputFile.getAbsolutePath());
            readAndAddTares();

        } catch (Exception e) {
            log.error("Failed to process uploaded MDB file", e);
        }
    }

    // ========================================================================
    // === READ .MDB AND SYNC TO POSTGRES ===
    // ========================================================================
    private void readAndAddTares() {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            String url = "jdbc:ucanaccess://" + MDB_FILE.getAbsolutePath();
            Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
            conn = DriverManager.getConnection(url);
            stmt = conn.createStatement();

            String query = "SELECT Veh_Number, TareWeight, START_DATE_TIME FROM " + TABLE_NAME;
            rs = stmt.executeQuery(query);

            int count = 0;
            int skipped = 0;

            while (rs.next()) {
                String rawWagonNumber = rs.getString("Veh_Number");
                String tare = rs.getString("TareWeight");
                String upDate = rs.getString("START_DATE_TIME");

                // === CLEAN WAGON NUMBER ===
                String wagonNumber = cleanWagonNumber(rawWagonNumber);

                if (wagonNumber == null || tare == null || tare.trim().isEmpty()) {
                    log.warn("Skipping record due to missing data → Veh_Number: '{}', TareWeight: '{}'", rawWagonNumber, tare);
                    skipped++;
                    continue;
                }

                try {
                    wService.addOrUpdateWtare(tare.trim(), wagonNumber, upDate);
                    count++;
                    log.info("Synced → wagon: {}, tare: {}", wagonNumber, tare.trim());
                } catch (IllegalArgumentException e) {
                    log.warn("Validation failed for wagon: '{}' (original: '{}') | Error: {}", wagonNumber, rawWagonNumber, e.getMessage());
                    skipped++;
                }
            }

            log.info("MDB import completed | Processed: {}, Skipped: {}", count, skipped);

        } catch (Exception e) {
            log.error("Error reading MDB file: {}", e.getMessage(), e);
        } finally {
            closeQuietly(rs);
            closeQuietly(stmt);
            closeQuietly(conn);
        }
    }

    // ========================================================================
    // === HELPER: CLEAN WAGON NUMBER (remove dots, trim, etc.) ===
    // ========================================================================
    private String cleanWagonNumber(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        // Remove trailing dots, trim, and sanitize
        String cleaned = input.trim().replaceAll("\\.$", "");
        return cleaned.isEmpty() ? null : cleaned;
    }

    // ========================================================================
    // === HELPER: CLOSE RESOURCES SAFELY ===
    // ========================================================================
    private void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                log.debug("Error closing resource", e);
            }
        }
    }
}