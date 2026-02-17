package com.jaba.awr_3.core.process;

import com.fazecast.jSerialComm.SerialPort;
import com.jaba.awr_3.core.connectors.ComService;
import com.jaba.awr_3.core.parsers.Tsr4000Parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class ProcesCom4 {
    private final Tsr4000Parser tsr4000Parser;

    // === ComService ინფორმაცია (final, ინექცია) ===
    private final ComService comService;

    // === პორტის კონფიგურაცია (final, ინიციალიზაცია @PostConstruct-ში) ===
    private String portName;
    private String scaleName;
    private String instrument;
    private boolean active;
    private boolean automatic;
    private boolean rightToUpdateTare;
    private int baudRate;
    private int dataBits;
    private int stopBits;
    private int parity;
    private final int scaleIndex = 4; // ComService-ში პირველი პორტის ინდექსი (მუდმივი 4-ით)

    private static final Logger log = LoggerFactory.getLogger(ProcesCom0.class);
    private static final int BUFFER_SIZE = 1024;
    

    // === სერიული პორტი (volatile — reconnection-ისთვის) ===
    private volatile SerialPort serialPort;

    // === ბუფერი და ინდექსი (thread-safe) ===
    private final byte[] buffer = new byte[BUFFER_SIZE];
    private final AtomicInteger bufferIndex = new AtomicInteger(0);

    // === ნაკადის მართვა ===
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread readerThread;

    // ========================================================================
    // === Spring Lifecycle ===
    // ========================================================================
    @PostConstruct
    public void init() {
         
        var portConfig = comService.getPortByIndex(scaleIndex);
        if (portConfig == null) {
            log.error("Port configuration not found for index: {}", scaleIndex);
            return;
        }
        this.portName = portConfig.getComName();
        this.scaleName = portConfig.getScaleName();
        this.instrument = portConfig.getInstrument();
        this.active = portConfig.isActive();
        this.automatic = portConfig.isAutomatic();
        this.baudRate = portConfig.getBaudRate();
        this.dataBits = portConfig.getDataBits();
        this.stopBits = portConfig.getStopBit();
        this.parity = portConfig.getParity();
        this.rightToUpdateTare = portConfig.isRightToUpdateTare();

        if (active) {
            startProcess();
        } else {
            log.info("Port {} is disabled in configuration.", portName);
        }
    }

    @PreDestroy
    public void destroy() {
        stopProcess();
        if (readerThread != null && readerThread.isAlive()) {
            try {
                readerThread.join(5000);
                if (readerThread.isAlive()) {
                    log.warn("Serial reader thread did not terminate in 5s. Forcing interrupt.");
                    readerThread.interrupt();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for serial thread to stop", e);
            }
        }
    }

    // ========================================================================
    // === პროცესის დაწყება ===
    // ========================================================================
    public void startProcess() {
        if (running.compareAndSet(false, true)) {
            readerThread = new Thread(this::mainLoop, "SerialReader-" + portName);
            readerThread.setDaemon(true);
            readerThread.start();
            log.info("Serial listener STARTED on {} for instrument: {}", portName, instrument);
        }
    }

    // ========================================================================
    // === პროცესის გაჩერება ===
    // ========================================================================
    public void stopProcess() {
        if (running.compareAndSet(true, false)) {
            if (readerThread != null) {
                readerThread.interrupt();
            }
            closePort();
            log.info("Serial listener STOPPED for {}", portName);
        }
    }

    private void closePort() {
        SerialPort port = this.serialPort;
        if (port != null && port.isOpen()) {
            port.closePort();
            this.serialPort = null;
        }
    }

    // ========================================================================
    // === მთავარი ციკლი (reconnection logic) ===
    // ========================================================================
    private void mainLoop() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                resetBuffer();
                openAndConfigurePort();

                byte[] tempReadBuffer = new byte[256];
                long lastSendTime = System.currentTimeMillis(); // დროის თრექინგი გაგზავნისთვის

                while (running.get() && !Thread.currentThread().isInterrupted()) {
                    SerialPort currentPort = this.serialPort;
                    if (currentPort == null || !currentPort.isOpen()) {
                        log.warn("Port closed unexpectedly. Reconnecting...");
                        break;
                    }

                    int available = currentPort.bytesAvailable();
                    if (available < 0) {
                        log.warn("Port disconnected (bytesAvailable = -1). Reconnecting...");
                        break;
                    }

                    if (available > 0) {
                        int read = currentPort.readBytes(tempReadBuffer, Math.min(available, tempReadBuffer.length));
                        if (read <= 0) {
                            if (read == -1)
                                break;
                            continue;
                        }

                        appendToBuffer(tempReadBuffer, read);

                        // მონაცემების დამუშავება
                        if (processBuffer()) {
                            resetBuffer();
                        }
                    } else {
                        Thread.sleep(50);
                    }

                    // გაგზავნა 050D ჰექს ყოველ 3 წამში მხოლოდ TSR4000-ისთვის (ლოგირების გარეშე)
                    if ("TSR4000".equalsIgnoreCase(instrument)) {
                        long now = System.currentTimeMillis();
                        if (now - lastSendTime >= 2000) {
                            byte[] command = new byte[] { (byte) 0x05, (byte) 0x0D };
                            currentPort.writeBytes(command, command.length);
                            lastSendTime = now;
                        }
                    }
                }
            } catch (Exception e) {
                if (running.get()) {
                    log.error("Error in serial communication loop", e);
                }
            } finally {
                closePort();
            }

            // Reconnection delay
            if (running.get() && !Thread.currentThread().isInterrupted()) {
                log.info("Reconnecting to {} in 5 seconds...", portName);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        running.set(false);
        log.info("Serial reader thread terminated for {}", portName);
    }

    // ========================================================================
    // === ბუფერის მართვა ===
    // ========================================================================
    private void resetBuffer() {
        synchronized (buffer) {
            bufferIndex.set(0);
            Arrays.fill(buffer, (byte) 0);
        }
    }

    private void appendToBuffer(byte[] data, int length) {
        synchronized (buffer) {
            int index = bufferIndex.get();
            int spaceLeft = BUFFER_SIZE - index;
            int toCopy = Math.min(length, spaceLeft);

            System.arraycopy(data, 0, buffer, index, toCopy);
            bufferIndex.set(index + toCopy);

            if (toCopy < length) {
                log.warn("Buffer overflow! Clearing buffer to prevent corruption.");
                resetBuffer();
            }
        }
    }

    // ========================================================================
    // === მონაცემების დამუშავება ===
    // ========================================================================
    private boolean processBuffer() {
        synchronized (buffer) {
            int index = bufferIndex.get();
            if (index < 2)
                return false;

            return switch (instrument.toUpperCase()) {
                case "TSR4000" -> parseTSR4000();
                case "TUNAYLAR" -> parseTunaylar();
                default -> throw new IllegalStateException(
                        "Unsupported instrument: '" + instrument + "'. Supported: TSR4000, TUNAYLAR");
            };
        }
    }

    // ========================================================================
    // === TSR4000 Parser (CR+LF) === AND SEND ECHO
    // ========================================================================
    private boolean parseTSR4000() {
        synchronized (buffer) {
            int index = bufferIndex.get();
            for (int i = 0; i <= index - 2; i++) {
                if (buffer[i] == 0x0D && buffer[i + 1] == 0x0A) {
                    int packetLen = i + 2;
                    byte[] packet = Arrays.copyOf(buffer, packetLen);
                    String text = new String(packet, StandardCharsets.UTF_8).trim();

                    // პარსინგი და დაბეჭდვა ყოველთვის
                    tsr4000Parser.parseSectors(text, scaleName, portName, scaleIndex, automatic, rightToUpdateTare);
                    printPacket(packet, instrument);

                    // ექო მხოლოდ STX პაკეტებზე
                    if (packet.length >= 3 && packet[0] == 0x02) {
                        try {
                            String id = new String(new byte[] { packet[1], packet[2] }, StandardCharsets.US_ASCII);
                            sendEchoTSR4000(id);
                        } catch (Exception e) {
                            log.warn("Failed to extract or send echo for packet", e);
                        }
                    }

                    shiftBufferLeft(packetLen);
                    return true;
                }
            }
        }
        return false;
    }

    // ========================================================================
    // === TUNAYLAR Parser (STX ... CR) ===
    // ========================================================================
    private boolean parseTunaylar() {
        synchronized (buffer) {
            int index = bufferIndex.get();
            for (int i = 0; i < index; i++) {
                if (buffer[i] == 0x02) { // STX
                    for (int j = i + 20; j < index; j++) {
                        if (buffer[j] == 0x0D) { // CR
                            int packetLen = j - i + 1;
                            if (packetLen >= 21) {
                                byte[] packet = Arrays.copyOfRange(buffer, i, j + 1);
                                printPacket(packet, instrument);
                                shiftBufferLeft(packetLen);
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    // ========================================================================
    // === ბუფერის გადაწევა მარცხნივ (პაკეტის ამოშლა) ===
    // ========================================================================
    private void shiftBufferLeft(int removedLength) {
        synchronized (buffer) {
            int currentIndex = bufferIndex.get();
            System.arraycopy(buffer, removedLength, buffer, 0, currentIndex - removedLength);
            bufferIndex.set(currentIndex - removedLength);
            // დარჩენილი ნაწილი გაასუფთავე
            Arrays.fill(buffer, bufferIndex.get(), currentIndex, (byte) 0);
        }
    }

    // ========================================================================
    // === პაკეტის ბეჭდვა ===
    // ========================================================================
    private void printPacket(byte[] packet, String instrumentName) {
        String text = new String(packet, StandardCharsets.UTF_8).trim();
        System.out.println("[" + instrumentName + "] " + text);
    }

    // ========================================================================
    // === პორტის გახსნა და კონფიგურაცია ===
    // ========================================================================
    private void openAndConfigurePort() throws Exception {
        SerialPort port = SerialPort.getCommPort(portName);
        port.setBaudRate(baudRate);
        port.setNumStopBits(stopBits);
        port.setNumDataBits(dataBits);
        port.setParity(parity);
        configurePortTimeouts(port);

        if (!port.openPort()) {
            throw new RuntimeException("Failed to open port: " + portName);
        }

        this.serialPort = port;
        log.info("Port {} opened successfully.", portName);
    }

    private void configurePortTimeouts(SerialPort port) {
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);
    }

    // ========================================================================
    // === მონაცემების გაგზავნა ===
    // ========================================================================
    public void sendDataTSR4000(String data) {
        SerialPort port = this.serialPort;
        if (port != null && port.isOpen() && running.get()) {
            byte[] payload = data.getBytes(StandardCharsets.UTF_8);
            byte[] frame = new byte[payload.length + 2];
            frame[0] = 0x02; // STX
            System.arraycopy(payload, 0, frame, 1, payload.length);
            frame[frame.length - 1] = 0x0D; // CR

            int written = port.writeBytes(frame, frame.length);
            if (written == frame.length) {
                log.info("SENT to {}: STX + {} + CR", instrument, data);
            } else {
                log.warn("Partial send: {}/{} bytes", written, frame.length);
            }
        } else {
            log.warn("Cannot send data: port not open or process not running");
        }
    }

    // ========================================================================
    // === მონაცემების გაგზავნა ===
    // ========================================================================
    private void sendEchoTSR4000(String data) {
        SerialPort port = this.serialPort;
        if (port != null && port.isOpen() && running.get()) {
            byte[] payload = data.getBytes(StandardCharsets.UTF_8);
            byte[] frame = new byte[payload.length + 4];
            frame[0] = 0x06; // ?
            System.arraycopy(payload, 0, frame, 1, payload.length);
            frame[frame.length - 1] = 0x0D; // ?
            frame[frame.length - 2] = 0x05;
            frame[frame.length - 3] = 0x0D; // ?
            int written = port.writeBytes(frame, frame.length);
            if (written == frame.length) {
                log.info("SENT to {}: STX + {} + CR", instrument, data);
            } else {
                log.warn("Partial send: {}/{} bytes", written, frame.length);
            }
        } else {
            log.warn("Cannot send data: port not open or process not running");
        }
    }
}