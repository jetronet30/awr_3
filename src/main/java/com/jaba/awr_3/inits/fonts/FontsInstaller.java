package com.jaba.awr_3.inits.fonts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
public class FontsInstaller {

    private static final Logger logger = LoggerFactory.getLogger(FontsInstaller.class);

    private static final String CHECK_DEJAVU = "fc-list | grep -i 'DejaVu' | head -1";
    private static final String CHECK_NOTO = "fc-list | grep -i 'Noto' | head -1";
    private static final String UPDATE_APT = "apt update";
    private static final String INSTALL_FONTS = "apt install -y fonts-dejavu-core fonts-noto-core";

    public static void init() {
        logger.info("Initializing system fonts (DejaVu & Noto)...");

        try {
            if (areFontsInstalled()) {
                logger.info("Required fonts (DejaVu/Noto) are already installed.");
                return;
            }

            logger.warn("Required fonts not found. Attempting to install...");

            // სცადე apt update
            if (!runCommandWithRetry(UPDATE_APT, "Failed to update package list for fonts")) {
                logger.warn("Could not update package list. Skipping font installation.");
                return;
            }

            // სცადე ფონტების ინსტალაცია
            if (!runCommandWithRetry(INSTALL_FONTS, "Failed to install fonts-dejavu-core and fonts-noto-core")) {
                logger.error("Font installation failed. Application will continue without system fonts.");
                return;
            }

            // გადაამოწმე ინსტალაციის შემდეგ
            if (areFontsInstalled()) {
                logger.info("Fonts installed successfully: DejaVu & Noto");
            } else {
                logger.warn("Fonts were installed but not detected in fc-list. Cache may need update.");
                runCommandWithRetry("fc-cache -fv", "Failed to update font cache");
            }

        } catch (Exception e) {
            logger.error("Unexpected error during font initialization. Continuing without fonts.", e);
            // არ აგდებს exception — აპლიკაცია გრძელდება
        }
    }

    private static boolean areFontsInstalled() {
        boolean hasDejaVu = commandHasOutput(CHECK_DEJAVU);
        boolean hasNoto = commandHasOutput(CHECK_NOTO);

        if (hasDejaVu && hasNoto) {
            logger.debug("Fonts detected: DejaVu and Noto");
            return true;
        } else if (hasDejaVu) {
            logger.debug("DejaVu fonts found, but Noto missing.");
        } else if (hasNoto) {
            logger.debug("Noto fonts found, but DejaVu missing.");
        } else {
            logger.debug("Neither DejaVu nor Noto fonts detected.");
        }
        return false;
    }

    private static boolean commandHasOutput(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String output = reader.readLine();
                int exitCode = process.waitFor();
                return exitCode == 0 && output != null && !output.trim().isEmpty();
            }
        } catch (Exception e) {
            logger.debug("Error checking font with command: {}", command, e);
            return false;
        }
    }

    private static boolean runCommandWithRetry(String command, String errorMsg) {
        int maxRetries = 2;
        for (int i = 0; i <= maxRetries; i++) {
            try {
                logger.debug("Executing font command (attempt {}): {}", i + 1, command);
                ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                // გამოიტანე output ლოგში
                try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.debug("[FONTS] {}", line);
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    return true;
                } else {
                    logger.warn("{} (exit code: {}, attempt {}/{})", errorMsg, exitCode, i + 1, maxRetries + 1);
                    if (i == maxRetries) return false;
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                }
            } catch (Exception e) {
                logger.warn("{}: {}", errorMsg, e.getMessage());
                if (i == maxRetries) return false;
            }
        }
        return false;
    }
}