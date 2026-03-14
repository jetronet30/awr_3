package com.jaba.awr_3.core.numberdetection.ocr;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaba.awr_3.core.prodata.jparepo.WagonJpa;
import com.jaba.awr_3.core.prodata.mod.TrainMod;
import com.jaba.awr_3.core.prodata.mod.WagonMod;
import com.jaba.awr_3.core.prodata.services.TrainService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;
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
    private final TrainService trainService;
    private final WagonJpa wagonJpa;

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

    public void sendStart(int conId, Long trainId) {
        broadcast(conId + "_START/id=" + trainId);
    }

    public void sendStop(int conId, Long trainId) {
        broadcast(conId + "_STOP/id=" + trainId);
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

            } catch (Exception e) {

                log.warn("Send failed to {} → {}", id, e.toString());

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

        String clientId;

        ClientConnection(Socket s) {

            this.socket = s;

            try {

                this.writer = new PrintWriter(
                        new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);

            } catch (IOException e) {

                log.warn("Writer failed for {}", s.getRemoteSocketAddress(), e);

            }
        }
    }

    private void handleClient(Socket socket) {

        String remote = socket.getRemoteSocketAddress().toString();

        log.info("handleClient started for {}", remote);

        ClientConnection conn = null;

        try {

            conn = new ClientConnection(socket);

            String clientId =
                    "client_" + remote.replaceAll("[^0-9]", "_") + "_" + System.currentTimeMillis();

            conn.clientId = clientId;

            clients.put(clientId, conn);

            log.info("Client added: {} | active: {}", clientId, clients.size());

            InputStream in = socket.getInputStream();

            byte[] buffer = new byte[4096];

            while (running && !socket.isClosed()) {

                int len = in.read(buffer);

                if (len == -1) {

                    log.info("Client closed connection {}", clientId);

                    break;
                }

                String message = new String(buffer, 0, len, StandardCharsets.UTF_8).trim();

                if (message.isEmpty()) continue;

                log.info("RAW RECEIVED from {}: {}", clientId, message);

                try {

                    if (message.startsWith("ID=")) {

                        processResultsMessage(message);

                    } else {

                        log.info("TEXT from {} → {}", clientId, message);

                    }

                } catch (Exception e) {

                    log.error("Message processing error from {}: {}",
                            clientId, e.getMessage(), e);

                }
            }

        } catch (Throwable t) {

            log.error("Critical error for {}: {}", remote, t.toString(), t);

        } finally {

            if (conn != null && conn.clientId != null) {

                clients.remove(conn.clientId, conn);

                log.info("Client disconnected: {} | remaining: {}",
                        conn.clientId, clients.size());
            }

            try {

                socket.close();

            } catch (Exception ignored) {}
        }
    }

    @Transactional
    private void processResultsMessage(String message) {

        try {

            int pipeIndex = message.indexOf('|');
            if (pipeIndex == -1) return;

            String idPart = message.substring(0, pipeIndex);
            String id = idPart.replace("ID=", "");

            String jsonPart = message.substring(pipeIndex + 1).trim();

            Map<String, Object> data =
                    mapper.readValue(jsonPart, new TypeReference<>() {});

            Integer totalWagons = (Integer) data.get("total_wagons");

            List<Map<String, Object>> wagons =
                    (List<Map<String, Object>>) data.get("wagons");

            System.out.println("ID=" + id + "   TOTAL WAGONS=" + totalWagons);
            TrainMod train = trainService.getTrainById(Long.parseLong(id));
            if (train == null) {
                return;
            }

            if (train.getCount() != totalWagons) {
                return;
            }



            for (Map<String, Object> wagon : wagons) {

                Integer row = (Integer) wagon.get("row");
                String number = (String) wagon.get("number");
                String quality = (String) wagon.get("quality");
                for (WagonMod wm : train.getWagons()) {
                    if(wm.getRowNum()==row){
                        if (wm.getWagonNumber().isEmpty()) {
                            wm.setWagonNumber(number);
                            
                            wagonJpa.save(wm);
                        }
                    }
                }

                System.out.println(
                        "row=" + row +
                        " , number=" + number +
                        " , quality=" + quality
                );
            }



        } catch (Exception e) {

            log.error("Parse error: {}", e.getMessage(), e);

        }
    }

    @PreDestroy
    public void shutdown() {

        running = false;

        try {

            if (serverSocket != null) serverSocket.close();

        } catch (Exception ignored) {}

        serverExecutor.shutdownNow();
        clientPool.shutdownNow();

        log.info("TCP server stopped");
    }
}
