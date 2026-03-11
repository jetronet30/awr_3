
package com.jaba.awr_3.core.numberdetection.ocr;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class OcrLis {

    private static final int PORT = 45000;

    private ExecutorService serverExecutor;
    private ExecutorService clientPool;

    private ServerSocket serverSocket;
    private volatile boolean running = true;

    private final ConcurrentHashMap<String, ClientConnection> clients = new ConcurrentHashMap<>();

    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void init() {

        serverExecutor = Executors.newSingleThreadExecutor();
        clientPool = Executors.newCachedThreadPool();

        serverExecutor.submit(this::startTcpServer);

        log.info("TCP Server started on port {}", PORT);
    }

    // -----------------------------
    // SEND COMMANDS
    // -----------------------------

    public void sendStart(String clientId, Long trainId) {
        sendCommand(clientId, clientId + "_START/id=" + trainId);
    }

    public void sendStop(String clientId, Long trainId, int wagonCount) {
        sendCommand(clientId, clientId + "_STOP/id=" + trainId + "/w_c=" + wagonCount);
    }

    private void sendCommand(String clientId, String command) {

        ClientConnection conn = clients.get(clientId);

        if (conn == null || conn.socket.isClosed()) {
            log.warn("Client {} not connected", clientId);
            return;
        }

        try {
            conn.writer.println(command);
            conn.writer.flush();

            log.info("Sent to {} → {}", clientId, command);

        } catch (Exception e) {

            log.error("Command send failed {}", clientId, e);
        }
    }

    private void broadcastCommand(String command) {

        clients.forEach((clientId, conn) -> {

            if (conn.socket.isClosed()) {
                return;
            }

            try {

                conn.writer.println(command);
                conn.writer.flush();

                log.info("Sent → {} : {}", clientId, command);

            } catch (Exception e) {

                log.error("Send failed for {}", clientId, e);
            }
        });
    }

    // -----------------------------
    // TCP SERVER
    // -----------------------------

    private void startTcpServer() {

        try {

            serverSocket = new ServerSocket(PORT);

            log.info("Listening 0.0.0.0:{}", PORT);

            while (running) {

                Socket socket = serverSocket.accept();

                clientPool.submit(() -> handleClient(socket));
            }

        } catch (IOException e) {

            if (running) {
                log.error("Server error", e);
            }
        }
    }

    // -----------------------------
    // CLIENT CONNECTION
    // -----------------------------

    private static class ClientConnection {

        final Socket socket;
        final PrintWriter writer;
        final BufferedReader reader;

        String clientId;

        ClientConnection(Socket s) throws IOException {

            this.socket = s;

            this.writer = new PrintWriter(
                    new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);

            this.reader = new BufferedReader(
                    new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
        }
    }

    // -----------------------------
    // CLIENT HANDLER
    // -----------------------------

    private void handleClient(Socket socket) {

        String clientInfo = socket.getRemoteSocketAddress().toString();

        ClientConnection conn = null;

        try {

            conn = new ClientConnection(socket);

            // პირველი ხაზი უნდა იყოს clientId
            String clientId = conn.reader.readLine();

            if (clientId == null || clientId.isBlank()) {
                socket.close();
                return;
            }

            conn.clientId = clientId;

            clients.put(clientId, conn);

            log.info("Client connected {} ({})", clientId, clientInfo);

            String line;

            while (running && (line = conn.reader.readLine()) != null) {

                String trimmed = line.trim();

                if (trimmed.isEmpty())
                    continue;

                if (trimmed.startsWith("[")) {

                    processJsonMessage(trimmed, clientId);
                }
            }

        } catch (Exception e) {

            log.error("Client error {}", clientInfo, e);

        } finally {

            if (conn != null && conn.clientId != null) {

                clients.remove(conn.clientId);

                log.info("Client disconnected {}", conn.clientId);
            }

            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }
    }

    // -----------------------------
    // JSON PROCESS
    // -----------------------------

    private void processJsonMessage(String jsonText, String clientId) {

        try {

            List<Map<String, Object>> wagons = mapper.readValue(jsonText, new TypeReference<>() {
            });

            for (Map<String, Object> wagon : wagons) {

                Integer id = (Integer) wagon.get("id");
                String number = (String) wagon.get("number");

                if (id == null || number == null)
                    continue;

            }

        } catch (Exception e) {

            log.error("JSON parse error from {}", clientId, e);
        }
    }

    // -----------------------------
    // SHUTDOWN
    // -----------------------------

    @PreDestroy
    public void shutdown() {

        running = false;

        try {
            serverSocket.close();
        } catch (Exception ignored) {
        }

        serverExecutor.shutdownNow();
        clientPool.shutdownNow();

        log.info("TCP server stopped");
    }
}