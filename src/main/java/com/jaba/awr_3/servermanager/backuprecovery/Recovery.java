package com.jaba.awr_3.servermanager.backuprecovery;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jaba.awr_3.inits.postgres.DataService;
import com.jaba.awr_3.inits.repo.RepoInit;




public final class Recovery {

    private static final Logger LOGGER = LoggerFactory.getLogger(Recovery.class);

    private static final String DB_USER = DataService.getDataSettingsStatic().getDataUser();
    private static final String DB_PASSWORD = DataService.getDataSettingsStatic().getDataPassword();
    private static final String DB_NAME = DataService.getDataSettingsStatic().getDataName();
    private static final String DB_HOST = DataService.getDataSettingsStatic().getDataHost();
    private static final String DB_PORT = String.valueOf(DataService.getDataSettingsStatic().getDataPort());
    private static final String BACKUP_DATA_PATH = RepoInit.BACKUP_REPO_TEMP.getAbsolutePath() + "/databackup.sql";
    private static final File BACKUP_TAR = RepoInit.BACKUP_UPLOAD_REPO;
    private static final File SETTINGS_REPO = RepoInit.SERVER_SETTINGS_REPO;
    private static final File BACKUP_TEMP_PATH = RepoInit.BACKUP_REPO_TEMP;

    // Prevent instantiation (utility class)
    private Recovery() {
        throw new UnsupportedOperationException("Recovery is a utility class and cannot be instantiated");
    }

    /**
     * Main recovery process: extracts tar.gz, restores DB, copies JSON configs, and cleans up.
     * @throws RuntimeException if any step fails
     */
    public static void recovery() {
        validateConfig();
        try {
            unTarBackup();
            recoveryData();
            copyJsonFiles();
            cleanTemp();
            LOGGER.info("Recovery process completed successfully");
        } catch (Exception e) {
            LOGGER.error("Recovery process failed: {}", e.getMessage(), e);
            throw new RuntimeException("Recovery failed", e);
        }
    }

    /**
     * Validates configuration parameters to ensure they are not null or empty.
     * @throws IllegalStateException if any configuration is invalid
     */
    private static void validateConfig() {
        if (DB_USER == null || DB_USER.trim().isEmpty()) {
            throw new IllegalStateException("Database user is not configured");
        }
        if (DB_PASSWORD == null || DB_PASSWORD.trim().isEmpty()) {
            throw new IllegalStateException("Database password is not configured");
        }
        if (DB_NAME == null || DB_NAME.trim().isEmpty()) {
            throw new IllegalStateException("Database name is not configured");
        }
        if (DB_HOST == null || DB_HOST.trim().isEmpty()) {
            throw new IllegalStateException("Database host is not configured");
        }
        if (DB_PORT == null || DB_PORT.trim().isEmpty()) {
            throw new IllegalStateException("Database port is not configured");
        }
        if (BACKUP_TAR == null || !BACKUP_TAR.isDirectory()) {
            throw new IllegalStateException("Backup tar directory is invalid: " + (BACKUP_TAR != null ? BACKUP_TAR.getAbsolutePath() : "null"));
        }
        if (SETTINGS_REPO == null || !SETTINGS_REPO.isDirectory()) {
            throw new IllegalStateException("Settings repository is invalid: " + (SETTINGS_REPO != null ? SETTINGS_REPO.getAbsolutePath() : "null"));
        }
        if (BACKUP_TEMP_PATH == null || !BACKUP_TEMP_PATH.isDirectory()) {
            throw new IllegalStateException("Backup temp directory is invalid: " + (BACKUP_TEMP_PATH != null ? BACKUP_TEMP_PATH.getAbsolutePath() : "null"));
        }
    }

    /**
     * Extracts the first .tar.gz file from BACKUP_TAR to BACKUP_TEMP_PATH.
     * @throws RuntimeException if extraction fails
     */
    public static void unTarBackup() {
        File backupDir = BACKUP_TAR;
        File outDir = BACKUP_TEMP_PATH;

        try {
            // Clean and recreate output directory for idempotency
            FileUtils.cleanDirectory(outDir);
            FileUtils.forceMkdir(outDir);
        } catch (IOException e) {
            LOGGER.error("Failed to prepare output directory: {}", outDir.getAbsolutePath(), e);
            throw new RuntimeException("Failed to prepare output directory", e);
        }

        File[] tarGzFiles = FileUtils.listFiles(backupDir, new String[]{"tar.gz"}, false).toArray(new File[0]);
        if (tarGzFiles.length == 0) {
            String msg = "No tar.gz file found in directory: " + backupDir.getAbsolutePath();
            LOGGER.error(msg);
            throw new RuntimeException(msg);
        }

        File tarGzFile = tarGzFiles[0];
        LOGGER.info("Found tar.gz file: {}", tarGzFile.getAbsolutePath());

        // Normalize output directory path to handle ./ or ../
        String normalizedOutDir = outDir.getAbsolutePath().replace("/./", "/");

        try (FileInputStream fis = new FileInputStream(tarGzFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             GzipCompressorInputStream gzis = new GzipCompressorInputStream(bis);
             TarArchiveInputStream tais = new TarArchiveInputStream(gzis)) {

            TarArchiveEntry entry;
            while ((entry = tais.getNextTarEntry()) != null) {
                String entryName = entry.getName();
                // Validate entry name to prevent malicious paths
                if (entryName.startsWith("/") || entryName.contains("..") || entryName.contains("|") || entryName.contains(";")) {
                    LOGGER.error("Invalid or potentially malicious tar entry: {}", entryName);
                    throw new RuntimeException("Invalid tar entry: " + entryName);
                }

                // Construct and normalize output path
                Path outputPath = Paths.get(outDir.getAbsolutePath(), entryName).normalize();
                String normalizedOutputPath = outputPath.toString().replace("/./", "/");
                if (!normalizedOutputPath.startsWith(normalizedOutDir)) {
                    LOGGER.error("Path traversal detected in tar entry: {}. Expected prefix: {}, Got: {}", 
                        entryName, normalizedOutDir, normalizedOutputPath);
                    throw new RuntimeException("Path traversal detected: " + entryName);
                }

                File outputFile = outputPath.toFile();
                if (entry.isDirectory()) {
                    FileUtils.forceMkdir(outputFile);
                    LOGGER.info("Created directory: {}", outputFile.getAbsolutePath());
                    continue;
                }

                // Check file size
                if (entry.getSize() > 1_000_000_000) { // 10GB limit
                    LOGGER.error("File too large: {}", entryName);
                    throw new RuntimeException("File too large: " + entryName);
                }

                File parent = outputFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    FileUtils.forceMkdir(parent);
                }

                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[8192];
                    long written = 0;
                    int len;
                    while ((len = tais.read(buffer)) != -1) {
                        written += len;
                        if (written > 1_000_000_000) { // 1GB limit
                            LOGGER.error("File size limit exceeded during extraction: {}", entryName);
                            throw new RuntimeException("File size limit exceeded: " + entryName);
                        }
                        fos.write(buffer, 0, len);
                    }
                    LOGGER.info("Extracted: {}", outputFile.getAbsolutePath());
                }
            }
            LOGGER.info("Successfully extracted backup: {}", tarGzFile.getName());
        } catch (IOException e) {
            LOGGER.error("Error while extracting tar.gz file: {}", tarGzFile.getAbsolutePath(), e);
            throw new RuntimeException("Failed to extract backup", e);
        }
    }

    /**
     * Restores PostgreSQL database from the SQL file at BACKUP_DATA_PATH.
     * @throws RuntimeException if restoration fails
     */
    public static void recoveryData() {
        File backupDir = BACKUP_TEMP_PATH;

        File[] tarSql = FileUtils.listFiles(backupDir, new String[]{".sql"}, false).toArray(new File[0]);
        if (tarSql.length == 0) {
            String msg = "No tar.gz file found in directory: " + backupDir.getAbsolutePath();
            LOGGER.error(msg);
            throw new RuntimeException(msg);
        }

        File tarSqlF = tarSql[0];
        LOGGER.info("Found tar.gz file: {}", tarSqlF.getAbsolutePath());
        if (!DB_HOST.equalsIgnoreCase("localhost")) {
            LOGGER.warn("Remote DB restore skipped. Host is not localhost: {}", DB_HOST);
            return;
        }

        File backupFile = new File(tarSqlF.getAbsolutePath());
        if (!backupFile.exists() || !backupFile.isFile()) {
            LOGGER.error("Backup SQL file does not exist: {}", tarSqlF.getAbsolutePath());
            throw new RuntimeException("Backup SQL file does not exist: " + tarSqlF.getAbsolutePath());
        }

        String[] safeArgs = new String[]{"psql", "-U", DB_USER, "-h", DB_HOST, "-p", DB_PORT, "-d", DB_NAME, "-f", tarSqlF.getAbsolutePath()};
        for (String arg : safeArgs) {
            if (arg.contains("'") || arg.contains("\"") || arg.contains(";")) {
                LOGGER.error("Invalid characters detected in database parameters: {}", arg);
                throw new RuntimeException("Invalid characters in database parameters");
            }
        }

        LOGGER.info("Starting database restore from: {}", BACKUP_DATA_PATH);
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(safeArgs);
            pb.environment().put("PGPASSWORD", DB_PASSWORD);
            pb.redirectErrorStream(true);
            process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOGGER.debug("psql output: {}", line);
                }
            }

            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                LOGGER.error("Database restore timed out");
                throw new RuntimeException("Database restore timed out");
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                LOGGER.info("Database restore completed successfully.");
            } else {
                LOGGER.error("Database restore failed with exit code: {}", exitCode);
                throw new RuntimeException("Database restore failed (exit code " + exitCode + ")");
            }
        } catch (IOException e) {
            LOGGER.error("Database restore failed: {}", BACKUP_DATA_PATH, e);
            throw new RuntimeException("Database restore failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Database restore interrupted: {}", BACKUP_DATA_PATH, e);
            throw new RuntimeException("Database restore interrupted", e);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    /**
     * Copies all JSON configuration files from BACKUP_TEMP_PATH to SETTINGS_REPO.
     * @throws RuntimeException if copying fails
     */
    public static void copyJsonFiles() {
        File sourceDir = BACKUP_TEMP_PATH;
        File destDir = SETTINGS_REPO;

        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            LOGGER.error("Invalid source directory: {}", sourceDir.getAbsolutePath());
            throw new RuntimeException("Invalid source directory: " + sourceDir.getAbsolutePath());
        }

        try {
            FileUtils.forceMkdir(destDir);
            LOGGER.info("Ensured destination directory exists: {}", destDir.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to create destination directory: {}", destDir.getAbsolutePath(), e);
            throw new RuntimeException("Failed to create destination directory", e);
        }

        // Normalize destination directory path to handle ./ or ../
        String normalizedDestDir = destDir.getAbsolutePath().replace("/./", "/");

        try {
            Collection<File> jsonFiles = FileUtils.listFiles(sourceDir, new String[]{"json"}, false);
            if (jsonFiles.isEmpty()) {
                LOGGER.warn("No .json files found in {}", sourceDir.getAbsolutePath());
                return;
            }

            for (File jsonFile : jsonFiles) {
                String fileName = jsonFile.getName();
                // Validate file name to prevent malicious paths
                if (fileName.contains("..") || fileName.contains("|") || fileName.contains(";")) {
                    LOGGER.error("Invalid or potentially malicious JSON file name: {}", fileName);
                    throw new RuntimeException("Invalid JSON file name: " + fileName);
                }

                Path destPath = Paths.get(destDir.getAbsolutePath(), fileName).normalize();
                String normalizedDestPath = destPath.toString().replace("/./", "/");
                if (!normalizedDestPath.startsWith(normalizedDestDir)) {
                    LOGGER.error("Path traversal detected in JSON file copy: {}. Expected prefix: {}, Got: {}", 
                        fileName, normalizedDestDir, normalizedDestPath);
                    throw new RuntimeException("Path traversal detected: " + fileName);
                }

                // Basic JSON format validation
                String contentType = Files.probeContentType(jsonFile.toPath());
                if (contentType == null || !contentType.equals("application/json")) {
                    LOGGER.warn("File {} may not be a valid JSON file (content type: {})", fileName, contentType);
                }

                File destFile = destPath.toFile();
                FileUtils.copyFile(jsonFile, destFile);
                LOGGER.info("Copied: {} → {}", fileName, destDir.getAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.error("Error copying JSON files from {} to {}", sourceDir.getAbsolutePath(), destDir.getAbsolutePath(), e);
            throw new RuntimeException("Failed to copy JSON files", e);
        }
    }

    /**
     * Cleans the temporary backup directory at BACKUP_TEMP_PATH.
     * @throws RuntimeException if cleanup fails
     */
    public static void cleanTemp() {
        try {
            FileUtils.cleanDirectory(BACKUP_TEMP_PATH);
            LOGGER.info("Temporary directory cleaned: {}", BACKUP_TEMP_PATH.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to clean temporary directory: {}", BACKUP_TEMP_PATH.getAbsolutePath(), e);
            throw new RuntimeException("Failed to clean temporary directory", e);
        }
    }
}