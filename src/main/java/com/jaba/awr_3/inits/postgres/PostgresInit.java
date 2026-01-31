package com.jaba.awr_3.inits.postgres;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PostgresInit {

     private static final Logger logger = LoggerFactory.getLogger(PostgresInit.class);

    private static final String CHECK_PSQL_VERSION = "psql --version";
    private static final String UPDATE_APT = "apt update";
    private static final String INSTALL_POSTGRES = "apt install -y postgresql postgresql-contrib";
    private static final String START_POSTGRES = "systemctl start postgresql";

    public static void init() {
        logger.info("Initializing PostgreSQL...");

        try {
            if (isPostgresInstalled()) {
                logger.info("PostgreSQL is already installed. Skipping installation.");
                startPostgresIfNeeded();
                initWithPsqlScript();
                logger.info("PostgreSQL setup completed successfully.");
                return;
            }

            logger.warn("PostgreSQL is not installed. Attempting to install...");

            // სცადე apt update
            if (!runCommandWithRetry(UPDATE_APT, "Failed to update package list")) {
                logger.warn("Could not update package list. Continuing without PostgreSQL installation.");
                return;
            }

            // სცადე ინსტალაცია
            if (!runCommandWithRetry(INSTALL_POSTGRES, "Failed to install PostgreSQL")) {
                logger.error("PostgreSQL installation failed. Application will run without local PostgreSQL.");
                return;
            }

            // თუ წარმატებით დაინსტალდა
            startPostgresIfNeeded();
            initWithPsqlScript();
            logger.info("PostgreSQL installed and configured successfully.");

        } catch (Exception e) {
            logger.error("Unexpected error during PostgreSQL initialization. Continuing without PostgreSQL.", e);
            // არ აგდებს exception — აპლიკაცია გრძელდება
        }
    }

    private static boolean isPostgresInstalled() {
        try {
            Process process = new ProcessBuilder("bash", "-c", CHECK_PSQL_VERSION).start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            logger.debug("Error checking PostgreSQL version: {}", e.getMessage());
            return false;
        }
    }

    private static void startPostgresIfNeeded() {
        try {
            runCommand(START_POSTGRES);
            logger.info("PostgreSQL service started.");
        } catch (Exception e) {
            logger.warn("Could not start PostgreSQL service: {}", e.getMessage());
        }
    }

    private static boolean runCommandWithRetry(String command, String errorMsg) {
        int maxRetries = 2;
        for (int i = 0; i <= maxRetries; i++) {
            try {
                logger.debug("Executing (attempt {}): {}", i + 1, command);
                ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                // ლოგი გამოტანა
                try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.debug("[{}] {}", command, line);
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    return true;
                } else {
                    logger.warn("{} (exit code: {}, attempt {}/{})", errorMsg, exitCode, i + 1, maxRetries + 1);
                    if (i == maxRetries) {
                        return false;
                    }
                    // მცირე დაყოვნება
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                }
            } catch (Exception e) {
                logger.warn("{}: {}", errorMsg, e.getMessage());
                if (i == maxRetries) return false;
            }
        }
        return false;
    }

    private static void runCommand(String command) {
        runCommandWithRetry(command, "Command failed: " + command);
    }

    private static void initWithPsqlScript() {
        String dbUser = System.getenv().getOrDefault("JETRONET_USER", DataService.getDataSettingsStatic().getDataUser());
        String dbPass = System.getenv().getOrDefault("JETRONET_PASS", DataService.getDataSettingsStatic().getDataPassword());
        String dbName = System.getenv().getOrDefault("JETRONET_DB", DataService.getDataSettingsStatic().getDataName());

        File tempFile = null;
        try {
            String sql = String.format("""
                    DO $$
                    BEGIN
                        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '%s') THEN
                            CREATE USER %s WITH PASSWORD '%s';
                        END IF;
                    END
                    $$;

                    DO $$
                    BEGIN
                        IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '%s') THEN
                            ALTER USER %s WITH SUPERUSER;
                        END IF;
                    END
                    $$;

                    SELECT 'CREATE DATABASE %s OWNER %s'
                    WHERE NOT EXISTS (
                        SELECT FROM pg_database WHERE datname = '%s'
                    )\\gexec
                    """, dbUser, dbUser, dbPass, dbUser, dbUser, dbName, dbUser, dbName);

            tempFile = File.createTempFile("initdb-", ".sql");
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile))) {
                bw.write(sql);
            }

            String command = "sudo -u postgres psql -f " + tempFile.getAbsolutePath();
            if (runCommandWithRetry(command, "Failed to execute SQL init script")) {
                logger.info("PostgreSQL database and user configured successfully.");
            } else {
                logger.warn("Could not configure PostgreSQL user/database. Check if PostgreSQL is running.");
            }

        } catch (Exception e) {
            logger.warn("Error in SQL initialization script: {}", e.getMessage());
        } finally {
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.delete(tempFile.toPath());
                } catch (IOException e) {
                    logger.debug("Failed to delete temp SQL file: {}", e.getMessage());
                }
            }
        }
    }

}
