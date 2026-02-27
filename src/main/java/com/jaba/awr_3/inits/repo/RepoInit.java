package com.jaba.awr_3.inits.repo;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepoInit {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepoInit.class);

    public static final File MAIN_REPO = new File("./MAINREPO");
    public static final File SERVER_SETTINGS_REPO = new File(MAIN_REPO, "serversettings");
    public static final File BACKUP_REPO_TEMP = new File(MAIN_REPO, "backuptemp");
    public static final File BACKUP_REPO_MVC = new File(MAIN_REPO, "backupmvc");
    public static final File BACKUP_UPLOAD_REPO = new File(MAIN_REPO, "backupupload");
    public static final File UTYL_REPO = new File(MAIN_REPO, "utils");
    public static final File LOGO_REPO = new File(MAIN_REPO, "logos");
    public static final File USR_SRC = new File("/usr/local/src");
    public static final File VIDEO_ARCHIVE = new File(MAIN_REPO,"videoarchive");
    public static final File STREAM_ALL_REPO = new File(MAIN_REPO, "streams");
    public static final File CAM_0_REPO = new File(STREAM_ALL_REPO, "cam0");
    public static final File CAM_1_REPO = new File(STREAM_ALL_REPO, "cam1");
    public static final File CAM_2_REPO = new File(STREAM_ALL_REPO, "cam2");
    public static final File CAM_3_REPO = new File(STREAM_ALL_REPO, "cam3");
    public static final File CAM_4_REPO = new File(STREAM_ALL_REPO, "cam4");
    public static final File CAM_5_REPO = new File(STREAM_ALL_REPO, "cam5");
    public static final File CAM_6_REPO = new File(STREAM_ALL_REPO, "cam6");
    public static final File CAM_7_REPO = new File(STREAM_ALL_REPO, "cam7");
    public static final File CAM_8_REPO = new File(STREAM_ALL_REPO, "cam8");
    public static final File CAM_9_REPO = new File(STREAM_ALL_REPO, "cam9");

    public static final File PDF_REPOSITOR_FULL = new File(MAIN_REPO, "pdf/full");
    public static final File PDF_REPOSITOR_LAST_0 = new File(MAIN_REPO, "pdf/last/0");
    public static final File PDF_REPOSITOR_LAST_1 = new File(MAIN_REPO, "pdf/last/1");
    public static final File PDF_REPOSITOR_LAST_2 = new File(MAIN_REPO, "pdf/last/2");
    public static final File PDF_REPOSITOR_LAST_3 = new File(MAIN_REPO, "pdf/last/3");
    public static final File PDF_REPOSITOR_LAST_4 = new File(MAIN_REPO, "pdf/last/4");
    public static final File PDF_REPOSITOR_LAST_5 = new File(MAIN_REPO, "pdf/last/5");
    public static final File PDF_REPOSITOR_LAST_6 = new File(MAIN_REPO, "pdf/last/6");
    public static final File PDF_REPOSITOR_LAST_7 = new File(MAIN_REPO, "pdf/last/7");
    public static final File PDF_REPOSITOR_LAST_8 = new File(MAIN_REPO, "pdf/last/8");
    public static final File PDF_REPOSITOR_LAST_9 = new File(MAIN_REPO, "pdf/last/9");

    public static void initRepos() {
        createDirectory(MAIN_REPO);
        createDirectory(SERVER_SETTINGS_REPO);
        createDirectory(UTYL_REPO);
        createDirectory(LOGO_REPO);
        createDirectory(USR_SRC);
        createDirectory(BACKUP_REPO_TEMP);
        createDirectory(BACKUP_REPO_MVC);
        createDirectory(BACKUP_UPLOAD_REPO);
        createDirectory(VIDEO_ARCHIVE);
        createDirectory(STREAM_ALL_REPO);
        createDirectory(PDF_REPOSITOR_FULL);
        createDirectory(PDF_REPOSITOR_LAST_0);
        createDirectory(PDF_REPOSITOR_LAST_1);
        createDirectory(PDF_REPOSITOR_LAST_2);
        createDirectory(PDF_REPOSITOR_LAST_3);
        createDirectory(PDF_REPOSITOR_LAST_4);
        createDirectory(PDF_REPOSITOR_LAST_5);
        createDirectory(PDF_REPOSITOR_LAST_6);
        createDirectory(PDF_REPOSITOR_LAST_7);
        createDirectory(PDF_REPOSITOR_LAST_8);
        createDirectory(PDF_REPOSITOR_LAST_9);

        useRam();

        createDirectory(CAM_0_REPO);
        createDirectory(CAM_1_REPO);
        createDirectory(CAM_2_REPO);
        createDirectory(CAM_3_REPO);
        createDirectory(CAM_4_REPO);
        createDirectory(CAM_5_REPO);
        createDirectory(CAM_6_REPO);
        createDirectory(CAM_7_REPO);
        createDirectory(CAM_8_REPO);
        createDirectory(CAM_9_REPO);

    }

    private static void createDirectory(File dir) {
        try {
            if (!dir.exists()) {
                dir.mkdirs();
                LOGGER.info("Directory created: " + dir.getAbsolutePath());
            } else {
                LOGGER.info("Directory already exists: " + dir.getAbsolutePath());
            }
        } catch (Exception e) {
            LOGGER.error("" + e);
        }

    }

    /**
     * Mounts a RAM disk (tmpfs) on STREAM_ALL_REPO if not already mounted.
     */
    public static void useRam() {
        String mountPath = STREAM_ALL_REPO.getAbsolutePath();

        if (isRamDiskMounted(mountPath)) {
            LOGGER.info("RAM disk is already mounted at: {}", mountPath);
            return;
        }

        LOGGER.info("Attempting to mount RAM disk (tmpfs) at: {} (size=10G)", mountPath);

        String command = "mount -t tmpfs -o size=10G tmpfs " + mountPath;
        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", command);
            pb.redirectErrorStream(true); // გააერთიანებს stdout და stderr
            Process process = pb.start();

            // ლოგირება პროცესის გამომავალის
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOGGER.debug("mount output: {}", line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                LOGGER.info("RAM disk mounted successfully at: {}", mountPath);
            } else {
                LOGGER.error("Failed to mount RAM disk. Command exited with code: {}", exitCode);
                throw new RuntimeException("RAM disk mount failed with exit code: " + exitCode);
            }
        } catch (Exception e) {
            LOGGER.error("Exception while mounting RAM disk: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to mount RAM disk", e);
        }
    }

    /**
     * Checks if a tmpfs is already mounted at the specified path.
     */
    private static boolean isRamDiskMounted(String path) {
        try {
            ProcessBuilder pb = new ProcessBuilder("mount");
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(path) && line.contains("tmpfs")) {
                        LOGGER.debug("RAM disk found in mount list: {}", line.trim());
                        return true;
                    }
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                LOGGER.warn("Command 'mount' exited with code: {}", exitCode);
            }
        } catch (Exception e) {
            LOGGER.error("Error checking mount status for RAM disk: {}", e.getMessage(), e);
        }
        LOGGER.debug("No tmpfs mount found for path: {}", path);
        return false;
    }

}
