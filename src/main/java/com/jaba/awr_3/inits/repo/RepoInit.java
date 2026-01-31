package com.jaba.awr_3.inits.repo;

import java.io.File;

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
    public static final File USR_SRC = new File("/usr/local/src");

    public static void initRepos() {
        createDirectory(MAIN_REPO);
        createDirectory(SERVER_SETTINGS_REPO);
        createDirectory(UTYL_REPO);
        createDirectory(USR_SRC);
        createDirectory(BACKUP_REPO_TEMP);
        createDirectory(BACKUP_REPO_MVC);
        createDirectory(BACKUP_UPLOAD_REPO);
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

}
