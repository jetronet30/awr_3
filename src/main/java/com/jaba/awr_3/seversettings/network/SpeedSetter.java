package com.jaba.awr_3.seversettings.network;


import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


@Service
@RequiredArgsConstructor
public class SpeedSetter {
    private final NetService netService;

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedSetter.class);

    @PostConstruct
    public void setSpeed() {
        for (String lan : netService.getLanNames()) {
            if (!isLinkDetected(lan)) {
                LOGGER.warn("🔌 Skipping {} — link not detected!!!", lan);
                continue;
            }

            String speed = readInterfaceSpeed(lan);
            String command = String.format("ethtool -s %s speed %s duplex full autoneg off", lan, speed);

            try {
                ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", command);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                String output = new String(process.getInputStream().readAllBytes());
                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    LOGGER.info(" Applied: {} | Output: {}", command, output.trim());
                } else {
                    LOGGER.warn(" Command failed: {} | Output: {}", command, output.trim());
                }

            } catch (IOException | InterruptedException e) {
                LOGGER.error(" Error executing: {}", command, e);
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean isLinkDetected(String iface) {
        try {
            ProcessBuilder pb = new ProcessBuilder("ethtool", iface);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            return output.contains("Link detected: yes");
        } catch (IOException e) {
            LOGGER.warn("Unable to determine link status for {}", iface);
            return false;
        }
    }

    private String readInterfaceSpeed(String iface) {
        String speedPath = "/sys/class/net/" + iface + "/speed";
        try {
            String speed = Files.readString(Paths.get(speedPath)).trim();
            if (speed.matches("\\d+")) return speed;
        } catch (IOException e) {
            LOGGER.warn("Couldn't read speed from {}. Using default 1000.", iface);
        }
        return "1000";
    }
}
