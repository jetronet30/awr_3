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
import java.util.*;
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
        log.info("TCP OCR Server started on port {}", PORT);
    }

    public void sendStart(String conId, Long trainId) {
        broadcast(conId + "_START/id=" + trainId);
    }

    public void sendStop(String conId, Long trainId) {
        broadcast(conId + "_STOP/id=" + trainId);
    }

    public void broadcastStart() {
        broadcast("START");
    }

    public void broadcastStop() {
        broadcast("STOP");
    }

    private void broadcast(String message) {
        if (clients.isEmpty()) {
            log.info("No clients connected → broadcast skipped: {}", message);
            return;
        }

        int success = 0;
        List<String> sent = new ArrayList<>();

        for (var entry : clients.entrySet()) {
            String id = entry.getKey();
            ClientConnection c = entry.getValue();

            if (c.socket.isClosed()) continue;

            try {
                c.writer.println(message);
                c.writer.flush();
                success++;
                sent.add(id);
                log.info("Sent to {}: {}", id, message);
            } catch (Exception e) {
                log.warn("Send failed to {} → {}  {}", id, message, e.toString());
            }
        }

        log.info("Broadcast '{}' → success: {} / total: {}  sent to: {}", 
                 message, success, clients.size(), sent);
    }

    private void startTcpServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            log.info("Listening on 0.0.0.0:{}", PORT);

            while (running) {
                Socket socket = serverSocket.accept();
                log.info("Socket accepted: {}", socket.getRemoteSocketAddress());
                clientPool.submit(() -> handleClient(socket));
            }
        } catch (IOException e) {
            if (running) log.error("Server socket error", e);
        }
    }

    private static class ClientConnection {
        final Socket socket;
        PrintWriter writer;
        BufferedReader reader;
        String clientId;

        ClientConnection(Socket s) {
            this.socket = s;
            try {
                this.writer = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
                this.reader = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                log.warn("Reader/writer creation failed for {}", s.getRemoteSocketAddress(), e);
            }
        }
    }

    private void handleClient(Socket socket) {
        String remote = socket.getRemoteSocketAddress().toString();
        log.info("handleClient started for {}", remote);

        ClientConnection conn = null;

        try {
            conn = new ClientConnection(socket);

            // clientId გენერირება მაშინვე (არ ველოდებით readLine-ს)
            String clientId = "client_" + remote.replaceAll("[^0-9]", "_") + "_" + System.currentTimeMillis();
            conn.clientId = clientId;

            // კავშირის დამატება **დაუყოვნებლივ**
            clients.put(clientId, conn);
            log.info("Client added immediately: {} | active: {}", clientId, clients.size());

            // optional: ცდილობთ წაიკითხოთ clientId, მაგრამ არ ველოდებით
            if (conn.reader != null) {
                log.debug("Trying to read optional clientId from {}", remote);
                try {
                    String line = conn.reader.readLine();
                    if (line != null) {
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty()) {
                            clients.remove(clientId, conn);
                            clientId = trimmed;
                            conn.clientId = clientId;
                            clients.put(clientId, conn);
                            log.info("ClientId updated to '{}' from {}", clientId, remote);
                        }
                    }
                } catch (Exception e) {
                    log.debug("Optional clientId read failed from {}: {}", remote, e.getMessage());
                }
            }

            // მუდმივი მოსმენა
            while (running) {
                try {
                    if (conn.reader == null) {
                        Thread.sleep(5000);
                        continue;
                    }

                    String line = conn.reader.readLine();
                    if (line == null) {
                        log.info("EOF from {}", clientId);
                        break;
                    }

                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) continue;

                    log.debug("From {}: {}", clientId, trimmed);

                    if (trimmed.startsWith("[")) {
                        processJsonMessage(trimmed, clientId);
                    }

                } catch (InterruptedException ignored) {
                    break;
                } catch (IOException e) {
                    log.info("Connection lost {}: {}", clientId, e.getMessage());
                    break;
                }
            }

        } catch (Throwable t) {
            log.error("Critical error for {}: {}", remote, t.toString(), t);
        } finally {
            if (conn != null && conn.clientId != null) {
                clients.remove(conn.clientId, conn);
                log.info("Cleaned up {} | remaining: {}", conn.clientId, clients.size());
            }
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    private void processJsonMessage(String jsonText, String clientId) {
        try {
            List<Map<String, Object>> wagons = mapper.readValue(jsonText, new TypeReference<>() {});
            for (Map<String, Object> wagon : wagons) {
                Integer id = (Integer) wagon.get("id");
                String number = (String) wagon.get("number");
                if (id == null || number == null) continue;
                log.info("OCR result from {} → wagon {}: {}", clientId, id, number);
            }
        } catch (Exception e) {
            log.error("JSON error from {}: {}", clientId, e.toString());
        }
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (Exception ignored) {}
        serverExecutor.shutdownNow();
        clientPool.shutdownNow();
        log.info("TCP server stopped");
    }
}