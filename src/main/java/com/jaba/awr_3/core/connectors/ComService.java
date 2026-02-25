package com.jaba.awr_3.core.connectors;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
import com.fazecast.jSerialComm.SerialPort;
import com.jaba.awr_3.inits.repo.RepoInit;

@Service
public class ComService {

    private static final File COM_PORT_SETTINGS = new File(RepoInit.SERVER_SETTINGS_REPO, "comportsettings.json");
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final Logger LOGGER = LoggerFactory.getLogger(ComService.class);

    // Thread-safe file access
    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();

    // === Initialization (static, thread-safe) ===
    public static void initComPorts() {
        LOCK.writeLock().lock();
        try {
            if (COM_PORT_SETTINGS.exists()) {
                LOGGER.info("comportsettings.json already exists: {}", COM_PORT_SETTINGS.getAbsolutePath());
                return;
            }

            SerialPort[] comPorts = SerialPort.getCommPorts();
            if (comPorts.length == 0) {
                LOGGER.warn("No COM ports detected. Skipping file creation.");
                return;
            }

            Arrays.sort(comPorts, Comparator.comparing(SerialPort::getSystemPortName));
            List<ComMod> ports = new ArrayList<>();

            for (int i = 0; i < comPorts.length; i++) {
                SerialPort port = comPorts[i];
                ComMod cMod = new ComMod();
                cMod.setIndex(i);
                cMod.setComName(port.getSystemPortName());
                cMod.setComNick("COM" + i);
                cMod.setScaleName("Scale " + i);
                cMod.setInstrument("TSR4000");
                cMod.setBaudRate(9600);
                cMod.setDataBits(8);
                cMod.setStopBit(1);
                cMod.setParity(0);
                cMod.setInProcess(false);
                cMod.setActive(false);
                cMod.setAutomatic(false);
                cMod.setRightToUpdateTare(true);
                ports.add(cMod);
            }

            MAPPER.writeValue(COM_PORT_SETTINGS, ports);
            LOGGER.info("comportsettings.json created successfully: {} ports, path: {}", ports.size(),
                    COM_PORT_SETTINGS.getAbsolutePath());

        } catch (IOException e) {
            LOGGER.error("Failed to create comportsettings.json", e);
        } catch (Exception ex) {
            LOGGER.error("Unexpected error during COM port initialization", ex);
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    // === Safe file read ===
    private List<ComMod> readComPorts() throws IOException {
        LOCK.readLock().lock();
        try {
            if (!COM_PORT_SETTINGS.exists()) {
                LOGGER.warn("comportsettings.json does not exist");
                return new ArrayList<>();
            }
            return MAPPER.readValue(COM_PORT_SETTINGS, new TypeReference<List<ComMod>>() {
            });
        } finally {
            LOCK.readLock().unlock();
        }
    }

    // === Safe file write ===
    private void writeComPorts(List<ComMod> ports) throws IOException {
        LOCK.writeLock().lock();
        try {
            MAPPER.writeValue(COM_PORT_SETTINGS, ports);
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    // === List COM ports ===
    public List<ComMod> listComPorts() {
        try {
            List<ComMod> ports = readComPorts();
            LOGGER.debug("COM port list requested: {} entries", ports.size());
            return ports;
        } catch (IOException e) {
            LOGGER.error("Failed to read COM port list", e);
            return new ArrayList<>();
        }
    }

    // === Validation methods (as requested) ===
    private boolean isValidBaudRate(int baud) {
        int[] valid = { 300, 600, 1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200 };
        return Arrays.stream(valid).anyMatch(v -> v == baud);
    }

    private boolean isValidDataBits(int bits) {
        return bits >= 5 && bits <= 8;
    }

    private boolean isValidStopBits(int bits) {
        return bits == 1 || bits == 2 || bits == 3; // 1, 1.5, 2
    }

    private boolean isValidParity(int parity) {
        return parity >= 0 && parity <= 4; // NONE, ODD, EVEN, MARK, SPACE
    }

    // === Update port settings with validation ===
    public Map<String, Object> updateComPort(String name, String scaleName, boolean active, boolean automatic,
            String instrument, int parity, int baudRate, int dataBits, int stopBit, boolean rightToUpdate) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);

        // Validate name
        if (name == null || name.trim().isEmpty()) {
            response.put("error", "Port name is empty");
            LOGGER.warn("Update attempt with empty port name");
            return response;
        }

        // Validate parameters
        if (!isValidBaudRate(baudRate)) {
            response.put("error", "Invalid Baud Rate: " + baudRate);
            LOGGER.warn("Invalid Baud Rate: {}", baudRate);
            return response;
        }

        if (!isValidDataBits(dataBits)) {
            response.put("error", "Invalid Data Bits: " + dataBits + " (must be 5-8)");
            LOGGER.warn("Invalid Data Bits: {}", dataBits);
            return response;
        }

        if (!isValidStopBits(stopBit)) {
            response.put("error", "Invalid Stop Bits: " + stopBit + " (must be 1, 2 or 3)");
            LOGGER.warn("Invalid Stop Bits: {}", stopBit);
            return response;
        }

        if (!isValidParity(parity)) {
            response.put("error", "Invalid Parity: " + parity + " (must be 0-4)");
            LOGGER.warn("Invalid Parity: {}", parity);
            return response;
        }

        // Update port
        try {
            List<ComMod> ports = readComPorts();
            boolean found = false;

            for (ComMod cm : ports) {
                if (cm.getComName().equals(name)) {
                    cm.setParity(parity);
                    cm.setBaudRate(baudRate);
                    cm.setDataBits(dataBits);
                    cm.setStopBit(stopBit);
                    cm.setActive(active);
                    cm.setAutomatic(automatic);
                    cm.setScaleName(scaleName);
                    cm.setInstrument(instrument);
                    cm.setRightToUpdateTare(rightToUpdate);
                    found = true;
                    break;
                }
            }

            if (!found) {
                response.put("error", "Port not found: " + name);
                LOGGER.warn("Update attempted on non-existent port: {}", name);
                return response;
            }

            writeComPorts(ports);
            response.put("success", true);
            response.put("message", "Port settings updated successfully");

        } catch (IOException e) {
            LOGGER.error("Failed to update port: {}", name, e);
            response.put("error", "File write error");
        }

        return response;
    }

    public ComMod getPortByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            LOGGER.warn("getPortByName გამოძახებულია ცარიელი სახელით");
            return null;
        }

        try {
            List<ComMod> ports = readComPorts();
            for (ComMod cm : ports) {
                if (name.equals(cm.getComName())) {
                    LOGGER.debug("პორტი ნაპოვნია getPortByName-ით: {}", name);
                    return cm;
                }
            }
            LOGGER.debug("პორტი ვერ მოიძებნა getPortByName-ით: {}", name);
            return null;
        } catch (IOException e) {
            LOGGER.error("შეცდომა პორტის მოძებნისას სახელით: {}", name, e);
            return null;
        }
    }

    public ComMod getPortByNick(String nickName) {
        if (nickName == null || nickName.trim().isEmpty()) {
            LOGGER.warn("getPortByName გამოძახებულია ცარიელი სახელით");
            return null;
        }

        try {
            List<ComMod> ports = readComPorts();
            for (ComMod cm : ports) {
                if (nickName.equals(cm.getComNick())) {
                    LOGGER.debug("პორტი ნაპოვნია getPortByNick-ით: {}", nickName);
                    return cm;
                }
            }
            LOGGER.debug("პორტი ვერ მოიძებნა getPortByNick-ით: {}", nickName);
            return null;
        } catch (IOException e) {
            LOGGER.error("შეცდომა პორტის მოძებნისას სახელით: {}", nickName, e);
            return null;
        }
    }

    public ComMod getPortByIndex(int index) {
        try {
            List<ComMod> ports = readComPorts();
            for (ComMod cm : ports) {
                if (cm.getIndex() == index) {
                    LOGGER.debug("პორტი ნაპოვნია getPortByIndex-ით: {}", index);
                    return cm;
                }
            }
            LOGGER.debug("პორტი ვერ მოიძებნა getPortByIndex-ით: {}", index);
            return null;
        } catch (IOException e) {
            LOGGER.error("შეცდომა პორტის მოძებნისას ინდექსით: {}", index, e);
            return null;
        }
    }

}