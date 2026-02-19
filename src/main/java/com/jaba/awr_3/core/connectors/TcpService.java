package com.jaba.awr_3.core.connectors;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.jaba.awr_3.inits.repo.RepoInit;

@Service
public class TcpService {

    private static final File TCP_SETTINGS = new File(RepoInit.SERVER_SETTINGS_REPO, "tcpsettings.json");
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final Logger LOGGER = LoggerFactory.getLogger(TcpService.class);

    // Thread-safe file access
    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();

    // === Initialization (static, thread-safe) ===
    public static void initTcp() {
        LOCK.writeLock().lock();
        try {
            if (TCP_SETTINGS.exists()) {
                LOGGER.info("tcpsettings.json already exists: {}", TCP_SETTINGS.getAbsolutePath());
                return;
            }

            List<TcpMod> tcpMods = new ArrayList<>();

            TcpMod tcp1 = createTcp(5, "Scale 6", "TCP_0", "TCP_0", "TSR4000", "0.0.0.0", 5501, false, false, true);
            TcpMod tcp2 = createTcp(6, "Scale 7", "TCP_1", "TCP_1", "TSR4000", "0.0.0.0", 5502, false, false, true);
            TcpMod tcp3 = createTcp(7, "Scale 8", "TCP_2", "TCP_2", "TSR4000", "0.0.0.0", 5503, false, false, true);
            TcpMod tcp4 = createTcp(8, "Scale 9", "TCP_3", "TCP_3", "TSR4000", "0.0.0.0", 5504, false, false, true);
            TcpMod tcp5 = createTcp(9, "Scale 10", "TCP_4", "TCP_4", "TSR4000", "0.0.0.0", 5505, false, false, true);

            tcpMods.addAll(Arrays.asList(tcp1, tcp2, tcp3, tcp4, tcp5));

            MAPPER.writeValue(TCP_SETTINGS, tcpMods);
            LOGGER.info("tcpsettings.json created successfully: {} entries, path: {}", tcpMods.size(),
                    TCP_SETTINGS.getAbsolutePath());

        } catch (IOException e) {
            LOGGER.error("Failed to create tcpsettings.json", e);
        } catch (Exception ex) {
            LOGGER.error("Unexpected error during TCP initialization", ex);
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    private static TcpMod createTcp(int index, String scaleName, String tcpName, String tcpNik, String instrument,
            String ipAddress, int port, boolean isActive, boolean automatic, boolean rightToUpdateTare) {
        TcpMod tcp = new TcpMod();
        tcp.setIndex(index);
        tcp.setScaleName(scaleName);
        tcp.setTcpName(tcpName);
        tcp.setTcpNik(tcpNik);
        tcp.setInstrument(instrument);
        tcp.setIpAddress(ipAddress);
        tcp.setPort(port);
        tcp.setActive(isActive);
        tcp.setAutomatic(automatic);
        tcp.setRightToUpdateTare(rightToUpdateTare);
        return tcp;
    }

    // === Safe file read ===
    private List<TcpMod> readTcps() throws IOException {
        LOCK.readLock().lock();
        try {
            if (!TCP_SETTINGS.exists()) {
                LOGGER.warn("tcpsettings.json does not exist");
                return new ArrayList<>();
            }
            return MAPPER.readValue(TCP_SETTINGS, new TypeReference<List<TcpMod>>() {
            });
        } finally {
            LOCK.readLock().unlock();
        }
    }

    // === Safe file write ===
    private void writeTcps(List<TcpMod> tcpMods) throws IOException {
        LOCK.writeLock().lock();
        try {
            MAPPER.writeValue(TCP_SETTINGS, tcpMods);
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    // === List all TCP connections ===
    public List<TcpMod> listTcps() {
        try {
            List<TcpMod> tcpMods = readTcps();
            LOGGER.debug("TCP list requested: {} entries", tcpMods.size());
            return tcpMods;
        } catch (IOException e) {
            LOGGER.error("Failed to read TCP list", e);
            return new ArrayList<>();
        }
    }

    public TcpMod getTcpByIndex(int index) {
        for (TcpMod cm : listTcps()) {
            if (cm.getIndex() == index) {
                LOGGER.debug("პორტი ნაპოვნია getTcpByIndex-ით: {}", index);
                return cm;
            }
        }
        LOGGER.debug("პორტი ვერ მოიძებნა getTcpByIndex-ით: {}", index);
        return null;
    }

    // === Validation methods ===
    private boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty() && name.matches("[A-Za-z0-9_\\-]+");
    }

    private boolean isValidScaleName(String name) {
        return name != null && !name.trim().isEmpty() && name.matches("[\\p{L}0-9_\\- ]+");
    }

    private boolean isValidIp(String ip) {
        if (ip == null || ip.trim().isEmpty())
            return false;
        String[] parts = ip.trim().split("\\.");
        if (parts.length != 4)
            return false;
        try {
            for (String s : parts) {
                int i = Integer.parseInt(s);
                if (i < 0 || i > 255)
                    return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidPort(int port) {
        return port >= 1 && port <= 65535;
    }

    private boolean isValidInstrument(String instrument) {
        return instrument != null && !instrument.trim().isEmpty();
    }

    // === Update TCP settings by tcpName ===
    public Map<String, Object> updateTcpSettingsByName(
            String tcpName,
            String scaleName, String tcpNik, String instrument,
            String ipAddress, int port, boolean isActive, boolean automatic, boolean rightToUpdateTare) {

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);

        // Validate tcpName
        if (!isValidName(tcpName)) {
            response.put("error", "Invalid TCP name: " + tcpName);
            LOGGER.warn("Update attempt with invalid TCP name: {}", tcpName);
            return response;
        }

        // Validate scaleName
        if (!isValidScaleName(scaleName)) {
            response.put("error", "Invalid scale name: " + scaleName);
            return response;
        }

        // Validate tcpNik
        if (!isValidName(tcpNik)) {
            response.put("error", "Invalid TCP nickname: " + tcpNik);
            return response;
        }

        // Validate instrument
        if (!isValidInstrument(instrument)) {
            response.put("error", "Invalid instrument type: " + instrument);
            return response;
        }

        // Validate IP
        if (!isValidIp(ipAddress)) {
            response.put("error", "Invalid IP address: " + ipAddress);
            LOGGER.warn("Invalid IP: {}", ipAddress);
            return response;
        }

        // Validate port
        if (!isValidPort(port)) {
            response.put("error", "Invalid port: " + port + " (must be 1-65535)");
            LOGGER.warn("Invalid port: {}", port);
            return response;
        }

        try {
            List<TcpMod> tcpMods = readTcps();
            boolean found = false;

            for (TcpMod tcp : tcpMods) {
                if (tcp.getTcpName().equals(tcpName)) {
                    tcp.setScaleName(scaleName.trim());
                    tcp.setTcpNik(tcpNik.trim());
                    tcp.setInstrument(instrument.trim());
                    tcp.setIpAddress(ipAddress.trim());
                    tcp.setPort(port);
                    tcp.setActive(isActive);
                    tcp.setAutomatic(automatic);
                    tcp.setRightToUpdateTare(rightToUpdateTare);
                    found = true;
                    break;
                }
            }

            if (!found) {
                response.put("error", "TCP connection not found: " + tcpName);
                LOGGER.warn("Update attempted on non-existent TCP: {}", tcpName);
                return response;
            }

            writeTcps(tcpMods);
            response.put("success", true);
            response.put("message", "TCP settings updated successfully");

        } catch (IOException e) {
            LOGGER.error("Failed to update TCP: {}", tcpName, e);
            response.put("error", "File write error");
        }

        return response;
    }

    // === Enable/Disable TCP connection ===
    public Map<String, Object> setTcpActive(String tcpName, boolean active) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);

        if (!isValidName(tcpName)) {
            response.put("error", "Invalid TCP name");
            return response;
        }

        try {
            List<TcpMod> tcpMods = readTcps();
            boolean found = false;

            for (TcpMod tcp : tcpMods) {
                if (tcp.getTcpName().equals(tcpName)) {
                    tcp.setActive(active);
                    found = true;
                    break;
                }
            }

            if (!found) {
                response.put("error", "TCP connection not found: " + tcpName);
                return response;
            }

            writeTcps(tcpMods);
            response.put("success", true);
            response.put("message", active ? "TCP connection enabled" : "TCP connection disabled");

        } catch (IOException e) {
            LOGGER.error("Failed to toggle TCP active state: {}", tcpName, e);
            response.put("error", "Update failed");
        }

        return response;
    }

    // === Get TCP by name ===
    public TcpMod getTcpByName(String tcpName) {
        if (!isValidName(tcpName)) {
            LOGGER.warn("getTcpByName called with invalid name: {}", tcpName);
            return null;
        }

        try {
            List<TcpMod> tcpMods = readTcps();
            for (TcpMod tcp : tcpMods) {
                if (tcpName.equals(tcp.getTcpName())) {
                    LOGGER.debug("TCP found by name: {}", tcpName);
                    return tcp;
                }
            }
            LOGGER.debug("TCP not found by name: {}", tcpName);
            return null;
        } catch (IOException e) {
            LOGGER.error("Error finding TCP by name: {}", tcpName, e);
            return null;
        }
    }

    // === Get TCP by IP and Port ===
    public TcpMod getTcpByIpAndPort(String ip, int port) {
        if (!isValidIp(ip) || !isValidPort(port)) {
            LOGGER.warn("getTcpByIpAndPort called with invalid IP/port: {}/{}", ip, port);
            return null;
        }

        try {
            List<TcpMod> tcpMods = readTcps();
            for (TcpMod tcp : tcpMods) {
                if (ip.equals(tcp.getIpAddress()) && port == tcp.getPort()) {
                    LOGGER.debug("TCP found by IP:port: {}:{}", ip, port);
                    return tcp;
                }
            }
            return null;
        } catch (IOException e) {
            LOGGER.error("Error finding TCP by IP:port: {}:{}", ip, port, e);
            return null;
        }
    }

    // === Check if TCP is active ===
    public boolean tcpIsActive(String tcpName) {
        TcpMod tcp = getTcpByName(tcpName);
        return tcp != null && tcp.isActive();
    }
}