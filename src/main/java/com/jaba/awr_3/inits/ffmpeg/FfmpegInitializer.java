package com.jaba.awr_3.inits.ffmpeg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Service
public class FfmpegInitializer {

    private static final Logger logger = LoggerFactory.getLogger(FfmpegInitializer.class);

    private static final String FFMPEG_CHECK_COMMAND = "which ffmpeg";
    private static final String FFMPEG_INSTALL_COMMAND = "apt update && apt install -y ffmpeg";


    public static void init() {
        logger.info("Initializing FFmpegManager...");
        if (!isFfmpegInstalled()) {
            logger.warn("FFmpeg is not installed. Attempting to install...");
            if (!installFfmpeg()) {
                logger.error("Failed to install FFmpeg. Application may not function as expected.");
            } else {
                logger.info("FFmpeg installed successfully.");
            }
        } else {
            logger.info("FFmpeg is already installed.");
        }
    }

    private static boolean isFfmpegInstalled() {
        try {
            Process process = new ProcessBuilder(FFMPEG_CHECK_COMMAND.split("\\s+")).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String output = reader.readLine();
                boolean installed = output != null && !output.isBlank();
                logger.debug("FFmpeg installation check result: {}", installed);
                return installed;
            }
        } catch (IOException e) {
            logger.error("Error checking FFmpeg installation", e);
            return false;
        }
    }

    private static boolean installFfmpeg() {
        try {
            ProcessBuilder builder = new ProcessBuilder("/bin/bash", "-c", FFMPEG_INSTALL_COMMAND);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.debug("FFmpeg install output: {}", line);
                }
            }
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logger.info("FFmpeg installation completed successfully.");
                return true;
            } else {
                logger.error("FFmpeg installation failed with exit code: {}", exitCode);
                return false;
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error during FFmpeg installation", e);
            return false;
        }
    }
}