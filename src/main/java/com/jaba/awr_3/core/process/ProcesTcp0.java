package com.jaba.awr_3.core.process;


import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.jaba.awr_3.core.connectors.TcpService;
import com.jaba.awr_3.core.parsers.Tsr4000Parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class ProcesTcp0 {

    private final Tsr4000Parser tsr4000Parser;
    private final TcpService tcpService;

    // === TCP კონფიგურაცია (final, ინიციალიზაცია @PostConstruct-ში) ===
    private String tcpName;
    private String scaleName;
    private String instrument;
    private boolean active;
    private boolean automatic;
    private boolean rightToUpdateTare;
    private String ipAddress;
    private int port;
    private final int scaleIndex = 5; // TcpService-ში პირველი TCP კონფიგურაციის ინდექსი (მუდმივი 5-ით)

    private static final Logger log = LoggerFactory.getLogger(ProcesTcp0.class);
    private static final int BUFFER_SIZE = 1024;

    // === TCP სოკეტები (volatile — reconnection-ისთვის) ===
    private volatile ServerSocket serverSocket;
    private volatile Socket clientSocket;

    // === ბუფერი და ინდექსი (thread-safe) ===
    private final byte[] buffer = new byte[BUFFER_SIZE];
    private final AtomicInteger bufferIndex = new AtomicInteger(0);

    // === ნაკადის მართვა ===
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread acceptorThread;
    private Thread readerThread;

    // ========================================================================
    // === Spring Lifecycle ===
    // ========================================================================
    @PostConstruct
    public void init() {
        
        var tcpConfig = tcpService.getTcpModByIndex(scaleIndex);
        if (tcpConfig == null) {
            log.error("TCP configuration not found for: {}", tcpName);
            return;
        }
        this.tcpName = tcpConfig.getTcpName();
        this.scaleName = tcpConfig.getScaleName();
        this.instrument = tcpConfig.getInstrument();
        this.active = tcpConfig.isActive();
        this.automatic = tcpConfig.isAutomatic();
        this.rightToUpdateTare = tcpConfig.isRightToUpdateTare();
        this.ipAddress = tcpConfig.getIpAddress();
        this.port = tcpConfig.getPort();

        if (active) {
            startProcess();
        } else {
            log.info("TCP {} is disabled in configuration.", tcpName);
        }
    }

    @PreDestroy
    public void destroy() {
        stopProcess();
        awaitThreadTermination(acceptorThread, "Acceptor");
        awaitThreadTermination(readerThread, "Reader");
    }

    private void awaitThreadTermination(Thread thread, String name) {
        if (thread != null && thread.isAlive()) {
            try {
                thread.join(5000);
                if (thread.isAlive()) {
                    log.warn("{} thread did not terminate in 5s. Forcing interrupt.", name);
                    thread.interrupt();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for {} thread to stop", name, e);
            }
        }
    }

    // ========================================================================
    // === პროცესის დაწყება ===
    // ========================================================================
    public void startProcess() {
        if (running.compareAndSet(false, true)) {
            acceptorThread = new Thread(this::acceptorLoop, "TCP-Acceptor-" + tcpName);
            acceptorThread.setDaemon(true);
            acceptorThread.start();
            log.info("TCP Server listener STARTED on {}:{} for instrument: {}", ipAddress, port, instrument);
        }
    }

    // ========================================================================
    // === პროცესის გაჩერება ===
    // ========================================================================
    public void stopProcess() {
        if (running.compareAndSet(true, false)) {
            closeClientSocket();
            closeServerSocket();
            interruptThread(acceptorThread);
            interruptThread(readerThread);
            log.info("TCP Server listener STOPPED for {}", tcpName);
        }
    }

    private void interruptThread(Thread thread) {
        if (thread != null) {
            thread.interrupt();
        }
    }

    private void closeServerSocket() {
        ServerSocket ss = this.serverSocket;
        if (ss != null && !ss.isClosed()) {
            try {
                ss.close();
            } catch (IOException e) {
                log.warn("Error closing server socket", e);
            }
            this.serverSocket = null;
        }
    }

    private void closeClientSocket() {
        Socket cs = this.clientSocket;
        if (cs != null && !cs.isClosed()) {
            try {
                cs.shutdownInput();
                cs.shutdownOutput();
                cs.close();
            } catch (IOException e) {
                log.warn("Error closing client socket", e);
            }
            this.clientSocket = null;
        }
    }

    // ========================================================================
    // === Acceptor Loop (ServerSocket) ===
    // ========================================================================
    private void acceptorLoop() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                resetBuffer();
                openServerSocket();

                while (running.get() && !Thread.currentThread().isInterrupted()) {
                    ServerSocket currentServer = this.serverSocket;
                    if (currentServer == null || currentServer.isClosed()) {
                        log.warn("Server socket closed unexpectedly. Reopening...");
                        break;
                    }

                    try {
                        currentServer.setSoTimeout(1000); // accept timeout
                        Socket incoming = currentServer.accept();
                        handleNewClient(incoming);
                        break; // ერთი კლიენტი — ერთი კავშირი (COM-ის ანალოგია)
                    } catch (SocketTimeoutException ignored) {
                        // normal, loop continues
                    } catch (IOException e) {
                        if (running.get()) {
                            log.warn("Accept error. Reopening server socket...", e);
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                if (running.get()) {
                    log.error("Error in TCP acceptor loop", e);
                }
            } finally {
                closeServerSocket();
            }

            // Reconnection delay
            if (running.get() && !Thread.currentThread().isInterrupted()) {
                log.info("Reopening TCP server {}:{} in 5 seconds...", ipAddress, port);
                sleepIgnoreInterrupt(5000);
            }
        }

        running.set(false);
        log.info("TCP acceptor thread terminated for {}", tcpName);
    }

    private void openServerSocket() throws IOException {
        ServerSocket ss = new ServerSocket();
        ss.bind(new InetSocketAddress(ipAddress, port));
        ss.setSoTimeout(1000);
        this.serverSocket = ss;
        log.info("TCP Server bound to {}:{}", ipAddress, port);
    }

    private void handleNewClient(Socket socket) {
        closeClientSocket(); // close previous
        this.clientSocket = socket;

        try {
            // არ ვაყენებთ SoTimeout-ს read()-ზე
            // socket.setSoTimeout(100); ← წაშალე!
            socket.setSoTimeout(0); // ბლოკირებადი read()
        } catch (Exception e) {
            log.warn("Failed to configure client socket timeout", e);
        }

        log.info("Client connected from: {}", socket.getRemoteSocketAddress());

        readerThread = new Thread(this::readerLoop, "TCP-Reader-" + tcpName);
        readerThread.setDaemon(true);
        readerThread.start();
    }

    // ========================================================================
    // === Reader Loop (Client Socket) ===
    // ========================================================================
    private void readerLoop() {
        InputStream in = null;
        long lastSendTime = System.currentTimeMillis(); // დროის თრექინგი გაგზავნისთვის

        try {
            in = clientSocket.getInputStream();
            byte[] tempReadBuffer = new byte[256];

            while (running.get() && !Thread.currentThread().isInterrupted()) {
                Socket currentClient = this.clientSocket;
                if (currentClient == null || currentClient.isClosed()) {
                    log.warn("Client disconnected. Waiting for new connection...");
                    break;
                }

                try {
                    int available = in.available();
                    if (available > 0) {
                        int read = in.read(tempReadBuffer, 0, Math.min(available, tempReadBuffer.length));
                        if (read == -1) {
                            log.info("Client EOF. Closing connection.");
                            break;
                        }
                        if (read > 0) {
                            appendToBuffer(tempReadBuffer, read);

                            // მონაცემების დამუშავება
                            if (processBuffer()) {
                                resetBuffer();
                            }
                        }
                    } else {
                        Thread.sleep(50);
                    }

                    // გაგზავნა 050D ჰექს ყოველ 2 წამში მხოლოდ TSR4000-ისთვის (ლოგირების გარეშე)
                    if ("TSR4000".equalsIgnoreCase(instrument)) {
                        long now = System.currentTimeMillis();
                        if (now - lastSendTime >= 2000) { // 2000 ms = 2 წამი
                            sendPollingCommand(); // 05 0D გაგზავნა
                            lastSendTime = now;
                        }
                    }

                } catch (SocketTimeoutException ignored) {
                    // normal (თუ SoTimeout გაქვს ჩართული)
                } catch (IOException e) {
                    if (running.get()) {
                        log.warn("Read error. Client may have disconnected.", e);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            if (running.get()) {
                log.error("Error in TCP reader loop", e);
            }
        } finally {
            closeClientSocket();
            log.info("TCP reader thread terminated for {}", tcpName);
        }
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
    // === TSR4000 Parser (CR+LF) ===
    // ========================================================================
    private boolean parseTSR4000() {
        synchronized (buffer) {
            int index = bufferIndex.get();
            for (int i = 0; i <= index - 2; i++) {
                if (buffer[i] == 0x0D && buffer[i + 1] == 0x0A) {
                    int packetLen = i + 2;
                    byte[] packet = Arrays.copyOf(buffer, packetLen);
                    String text = new String(packet, StandardCharsets.UTF_8).trim();
                    tsr4000Parser.parseSectors(text, scaleName, tcpName, scaleIndex,automatic, rightToUpdateTare);
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
    // === ბუფერის გადაწევა მარცხნივ ===
    // ========================================================================
    private void shiftBufferLeft(int removedLength) {
        synchronized (buffer) {
            int currentIndex = bufferIndex.get();
            System.arraycopy(buffer, removedLength, buffer, 0, currentIndex - removedLength);
            bufferIndex.set(currentIndex - removedLength);
            Arrays.fill(buffer, bufferIndex.get(), currentIndex, (byte) 0);
        }
    }

    // ========================================================================
    // === პაკეტის ბეჭდვა ===
    // ========================================================================
    private void printPacket(byte[] packet, String instrumentName) {
        String text = new String(packet, StandardCharsets.UTF_8).trim();
        System.out.println("[" + instrumentName + " TCP] " + text);
    }

    // ========================================================================
    // === მონაცემების გაგზავნა (TCP Client-ზე) ===
    // ========================================================================
    public void sendDataTSR4000(String data) {
        Socket cs = this.clientSocket;
        if (cs != null && !cs.isClosed() && running.get()) {
            try {
                OutputStream out = cs.getOutputStream();
                byte[] payload = data.getBytes(StandardCharsets.UTF_8);
                byte[] frame = new byte[payload.length + 2];
                frame[0] = 0x02; // STX
                System.arraycopy(payload, 0, frame, 1, payload.length);
                frame[frame.length - 1] = 0x0D; // CR

                out.write(frame);
                out.flush();
                log.info("SENT to TCP {}:{}: STX + {} + CR", ipAddress, port, data);
            } catch (IOException e) {
                log.warn("Failed to send data over TCP", e);
            }
        } else {
            log.warn("Cannot send data: no active TCP client connection");
        }
    }

    // ========================================================================
    // === მონაცემების გაგზავნა (TCP Client-ზე) ===
    // ========================================================================
    private void sendEchoTSR4000(String data) {
        Socket cs = this.clientSocket;
        if (cs != null && !cs.isClosed() && running.get()) {
            try {
                OutputStream out = cs.getOutputStream();
                byte[] payload = data.getBytes(StandardCharsets.UTF_8);
                byte[] frame = new byte[payload.length + 4];
                frame[0] = 0x06;
                System.arraycopy(payload, 0, frame, 1, payload.length);
                frame[frame.length - 1] = 0x0D; // ?
                frame[frame.length - 2] = 0x05;
                frame[frame.length - 3] = 0x0D; // ?

                out.write(frame);
                out.flush();
                log.info("SENT to TCP {}:{}: STX + {} + CR", ipAddress, port, data);
            } catch (IOException e) {
                log.warn("Failed to send data over TCP", e);
            }
        } else {
            log.warn("Cannot send data: no active TCP client connection");
        }
    }

    private void sendPollingCommand() {
        Socket cs = this.clientSocket;
        if (cs != null && !cs.isClosed() && running.get()) {
            try {
                OutputStream out = cs.getOutputStream();
                byte[] command = new byte[] { (byte) 0x05, (byte) 0x0D };
                out.write(command);
                out.flush();
                // ლოგირება არაა — როგორც Serial ვერსიაში
            } catch (IOException e) {
                log.warn("Failed to send polling command (050D) over TCP", e);
            }
        }
    }

    // ========================================================================
    // === Utility: Sleep ignoring interrupt ===
    // ========================================================================
    private void sleepIgnoreInterrupt(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}