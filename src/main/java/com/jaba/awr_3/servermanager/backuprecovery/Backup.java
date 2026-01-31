package com.jaba.awr_3.servermanager.backuprecovery;


import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.jaba.awr_3.inits.postgres.DataService;
import com.jaba.awr_3.inits.repo.RepoInit;
import com.jaba.awr_3.seversettings.basic.BasicService;
import com.jaba.awr_3.seversettings.owner.OwnerService;

import java.io.*;
import java.nio.file.Path;

@Service
public class Backup {

    private static final Logger LOGGER = LoggerFactory.getLogger(Backup.class);

    private static final File BACKUP_TEMP = RepoInit.BACKUP_REPO_TEMP;
    private static final File BACKUP_MVC = RepoInit.BACKUP_REPO_MVC;

    private static final String BACKUP_DATA_PATH = BACKUP_TEMP.getAbsolutePath() + "/"
            + sanitarizeName(OwnerService.NAME) +"_"+"databackup.sql";
    private static final String BACKUP_DATA_MVC = BACKUP_MVC.getAbsolutePath() + "/"
            + sanitarizeName(OwnerService.NAME) +"_"+ "databackup.sql";

    private static final String DB_USER = DataService.getDataSettingsStatic().getDataUser();
    private static final String DB_PASSWORD = DataService.getDataSettingsStatic().getDataPassword();
    private static final String DB_NAME = DataService.getDataSettingsStatic().getDataName();
    private static final String DB_HOST = DataService.getDataSettingsStatic().getDataHost();
    private static final int DB_PORT = DataService.getDataSettingsStatic().getDataPort();

    /**
     * Creates a full system + database backup.
     */
    public static void backup() {
        try {
            validateDirectories();

            FileUtils.cleanDirectory(BACKUP_TEMP);
            FileUtils.cleanDirectory(BACKUP_MVC);

            // settings backup
            FileUtils.copyDirectory(RepoInit.SERVER_SETTINGS_REPO, BACKUP_TEMP);

            // database backup
            backupDatabase();
            backupDatabaseForMvc();

            // compress
            createTarArchive();

            // cleanup
            FileUtils.cleanDirectory(BACKUP_TEMP);

            LOGGER.info("✅ Backup completed successfully.");
        } catch (Exception e) {
            LOGGER.error("❌ Backup process failed", e);
            throw new RuntimeException("Backup process failed", e);
        }
    }

    private static void validateDirectories() throws IOException {
        if (!BACKUP_TEMP.exists() && !BACKUP_TEMP.mkdirs()) {
            throw new IOException("Cannot create temp backup directory: " + BACKUP_TEMP);
        }
        if (!BACKUP_MVC.exists() && !BACKUP_MVC.mkdirs()) {
            throw new IOException("Cannot create mvc backup directory: " + BACKUP_MVC);
        }
    }

    private static void backupDatabase() throws IOException, InterruptedException {
        if (!"localhost".equals(DB_HOST)) {
            LOGGER.warn("⚠️ Remote database backup skipped for security reasons (host = {}).", DB_HOST);
            return;
        }

        LOGGER.info("Starting PostgreSQL backup to: {}", BACKUP_DATA_PATH);
        String command = String.format("pg_dump -h %s -p %d -U %s -d %s -f %s",
                DB_HOST, DB_PORT, DB_USER, DB_NAME, BACKUP_DATA_PATH);

        ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
        pb.environment().put("PGPASSWORD", DB_PASSWORD);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            reader.lines().forEach(line -> LOGGER.debug("[pg_dump] {}", line));
        }

        int exitCode = process.waitFor();
        if (exitCode == 0) {
            LOGGER.info("✅ Database backup completed successfully.");
        } else {
            throw new IOException("pg_dump failed with exit code: " + exitCode);
        }
    }

    private static void backupDatabaseForMvc() throws IOException, InterruptedException {
        if (!"localhost".equals(DB_HOST)) {
            LOGGER.warn("⚠️ Remote database backup skipped for security reasons (host = {}).", DB_HOST);
            return;
        }

        LOGGER.info("Starting PostgreSQL backup to: {}", BACKUP_DATA_MVC);
        String command = String.format("pg_dump -h %s -p %d -U %s -d %s -f %s",
                DB_HOST, DB_PORT, DB_USER, DB_NAME, BACKUP_DATA_MVC);

        ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
        pb.environment().put("PGPASSWORD", DB_PASSWORD);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            reader.lines().forEach(line -> LOGGER.debug("[pg_dump] {}", line));
        }

        int exitCode = process.waitFor();
        if (exitCode == 0) {
            LOGGER.info("✅ Database backup completed successfully.");
        } else {
            throw new IOException("pg_dump failed with exit code: " + exitCode);
        }
    }

    private static void createTarArchive() throws IOException {

        String fileName = "backup" + "_" + sanitarizeName(OwnerService.NAME) + BasicService.getDateTimeForFileName()
                + ".tar.gz";
        Path outputPath = BACKUP_MVC.toPath().resolve(fileName);

        try (FileOutputStream fos = new FileOutputStream(outputPath.toFile());
                GzipCompressorOutputStream gcos = new GzipCompressorOutputStream(fos);
                TarArchiveOutputStream taos = new TarArchiveOutputStream(gcos)) {

            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            addDirectoryToArchive(taos, BACKUP_TEMP, "");

            LOGGER.info("✅ Backup archive created: {}", outputPath);
        }
    }

    private static void addDirectoryToArchive(TarArchiveOutputStream taos, File dir, String parent) throws IOException {
        File[] files = dir.listFiles();
        if (files == null)
            return;

        for (File file : files) {
            String entryName = parent.isEmpty() ? file.getName() : parent + "/" + file.getName();
            TarArchiveEntry entry = new TarArchiveEntry(file, entryName);
            taos.putArchiveEntry(entry);

            if (file.isFile()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    fis.transferTo(taos);
                }
                taos.closeArchiveEntry();
            } else if (file.isDirectory()) {
                taos.closeArchiveEntry();
                addDirectoryToArchive(taos, file, entryName);
            } else {
                LOGGER.warn("Skipping non-regular file: {}", file);
                taos.closeArchiveEntry();
            }
        }
    }

    private static String sanitarizeName(String input) {
        if (input == null || input.equals("")) {
            return "";
        }
        String sanitized = input.trim().toLowerCase();
        sanitized = sanitized.replaceAll("\\s+", "_");
        sanitized = sanitized.replaceAll("[^a-z0-9_.-]", "");
        if (sanitized.isEmpty()) {
            return "";
        }
        return sanitized;
    }
}
