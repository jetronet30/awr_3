package com.jaba.awr_3.core.numberdetection.ocr;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaba.awr_3.core.prodata.services.TrainService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class OcrLis {
    private final TrainService trainService;
    private static final int PORT = 45000;
    private ExecutorService serverExecutor;
    private ExecutorService clientPool;
    private ServerSocket serverSocket;
    private volatile boolean running = true;
    private final ConcurrentHashMap<String, ClientConnection> clients = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Logger LOGGER = LoggerFactory.getLogger(OcrLis.class);

    @PostConstruct
    public void init() {
        serverExecutor = Executors.newSingleThreadExecutor();
        clientPool = Executors.newCachedThreadPool();
        serverExecutor.submit(this::startTcpServer);
        LOGGER.info("TCP OCR Server started on port {}", PORT);
    }

    public void sendStart(int conId, Long trainId) {
        broadcast(conId + "_START/id=" + trainId);
    }

    public void sendStop(int conId, Long trainId) {
        broadcast(conId + "_STOP/id=" + trainId);
    }

    public void sendAbort(int conId, Long trainId) {
        broadcast(conId + "_ABORT/id=" + trainId);
    }

    private void broadcast(String message) {

        if (clients.isEmpty()) {
            LOGGER.info("No clients connected → broadcast skipped: {}", message);
            return;
        }

        int success = 0;
        List<String> sent = new ArrayList<>();

        for (var entry : clients.entrySet()) {

            String id = entry.getKey();
            ClientConnection c = entry.getValue();

            if (c.socket.isClosed())
                continue;

            try {

                c.writer.println(message);
                c.writer.flush();

                success++;
                sent.add(id);

            } catch (Exception e) {

                LOGGER.warn("Send failed to {} → {}", id, e.toString());

            }
        }

        LOGGER.info("Broadcast '{}' → success: {} / total: {}  sent to: {}",
                message, success, clients.size(), sent);
    }

    private void startTcpServer() {

        try {

            serverSocket = new ServerSocket(PORT);

            LOGGER.info("Listening on 0.0.0.0:{}", PORT);

            while (running) {

                Socket socket = serverSocket.accept();

                LOGGER.info("Socket accepted: {}", socket.getRemoteSocketAddress());

                clientPool.submit(() -> handleClient(socket));
            }

        } catch (IOException e) {

            if (running)
                LOGGER.error("Server socket error", e);

        }
    }

    private static class ClientConnection {

        final Socket socket;

        PrintWriter writer;

        String clientId;

        ClientConnection(Socket s) {

            this.socket = s;

            try {

                this.writer = new PrintWriter(
                        new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);

            } catch (IOException e) {

                LOGGER.warn("Writer failed for {}", s.getRemoteSocketAddress(), e);

            }
        }
    }

    private void handleClient(Socket socket) {

        String remote = socket.getRemoteSocketAddress().toString();

        LOGGER.info("handleClient started for {}", remote);

        ClientConnection conn = null;

        try {

            conn = new ClientConnection(socket);

            String clientId = "client_" + remote.replaceAll("[^0-9]", "_") + "_" + System.currentTimeMillis();

            conn.clientId = clientId;

            clients.put(clientId, conn);

            LOGGER.info("Client added: {} | active: {}", clientId, clients.size());

            InputStream in = socket.getInputStream();

            byte[] buffer = new byte[4096];

            while (running && !socket.isClosed()) {

                int len = in.read(buffer);

                if (len == -1) {

                    log.info("Client closed connection {}", clientId);

                    break;
                }

                String message = new String(buffer, 0, len, StandardCharsets.UTF_8).trim();

                if (message.isEmpty())
                    continue;

                LOGGER.info("RAW RECEIVED from {}: {}", clientId, message);

                try {

                    if (message.startsWith("ID=")) {

                        processResultsMessage(message);

                    } else {

                        LOGGER.info("TEXT from {} → {}", clientId, message);

                    }

                } catch (Exception e) {

                    LOGGER.error("Message processing error from {}: {}",
                            clientId, e.getMessage(), e);

                }
            }

        } catch (Throwable t) {

            LOGGER.error("Critical error for {}: {}", remote, t.toString(), t);

        } finally {

            if (conn != null && conn.clientId != null) {

                clients.remove(conn.clientId, conn);

                LOGGER.info("Client disconnected: {} | remaining: {}",
                        conn.clientId, clients.size());
            }

            try {

                socket.close();

            } catch (Exception ignored) {
            }
        }
    }

    @Transactional
    private void processResultsMessage(String message) {

        try {

            int pipeIndex = message.indexOf('|');
            if (pipeIndex == -1)
                return;

            String idPart = message.substring(0, pipeIndex);
            Long id = Long.valueOf(idPart.replace("ID=", ""));

            String jsonPart = message.substring(pipeIndex + 1).trim();

            Map<String, Object> data = mapper.readValue(jsonPart, new TypeReference<>() {
            });

            Integer totalWagons = (Integer) data.get("total_wagons");

            List<Map<String, Object>> wagons = (List<Map<String, Object>>) data.get("wagons");

            System.out.println("ID=" + id + "   TOTAL WAGONS=" + totalWagons);

            trainService.processOcrResult(id, totalWagons, wagons);
           

        } catch (Exception e) {

            LOGGER.error("Parse error: {}", e.getMessage(), e);

        }
    }

    @PreDestroy
    public void shutdown() {

        running = false;

        try {

            if (serverSocket != null)
                serverSocket.close();

        } catch (Exception ignored) {
        }

        serverExecutor.shutdownNow();
        clientPool.shutdownNow();

        LOGGER.info("TCP server stopped");
    }
}
