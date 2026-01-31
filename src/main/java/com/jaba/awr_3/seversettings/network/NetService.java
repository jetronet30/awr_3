package com.jaba.awr_3.seversettings.network;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaba.awr_3.inits.repo.RepoInit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class NetService {
    private static final Logger logger = LoggerFactory.getLogger(NetService.class);
    private static final File LAN_YAML = new File("/etc/netplan/50-cloud-init.yaml");
    private static final File LAN_JSON = new File(RepoInit.SERVER_SETTINGS_REPO, "lansettings.json");

    @PostConstruct
    public void convertYamlToJson() {
        try {
            logger.info("Converting YAML to JSON: {}", LAN_YAML.getAbsolutePath());
            Yaml yaml = new Yaml();
            try (FileInputStream inputStream = new FileInputStream(LAN_YAML)) {
                Map<String, Object> yamlData = yaml.load(inputStream);
                if (yamlData == null) {
                    logger.warn("YAML file is empty or invalid: {}", LAN_YAML.getAbsolutePath());
                    return;
                }

                List<NetMod> netMods = parseYamlToNetMod(yamlData);
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(LAN_JSON, netMods);
                logger.info("Successfully converted YAML to JSON and saved to: {}", LAN_JSON.getAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("Error converting YAML to JSON: {}", e.getMessage(), e);
        }
    }

    public List<String> getIpAddress() {
        List<String> ips = new ArrayList<>();
        ips.clear();
        for (NetMod nm : getNet()) {
            ips.add(nm.getIp());
        }
        return ips;
    }

    public List<NetMod> getNet() {
        try {
            logger.info("Reading JSON from: {}", LAN_JSON.getAbsolutePath());
            ObjectMapper mapper = new ObjectMapper();
            List<NetMod> nets = mapper.readValue(LAN_JSON, new TypeReference<List<NetMod>>() {
            });
            logger.info("Successfully read {} network interfaces from JSON", nets.size());
            for (NetMod nt : nets) {
                nt.setLink(iaActive(nt.getInName()));
                nt.setInternet(pingInternet(nt.getInName()));
            }
            return nets;
        } catch (IOException e) {
            logger.error("Error reading JSON: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private boolean iaActive(String inName) {
        File carrierFile = new File("/sys/class/net/" + inName + "/carrier");
        try {
            if (carrierFile.exists()) {
                String content = Files.readString(carrierFile.toPath()).trim();
                return "1".equals(content);
            }
        } catch (IOException e) {
            logger.error("Failed to read interface status for {}", inName, e);
        }
        return false;
    }

    public boolean pingInternet(String interfaceName) {
        try {
            // Construct the ping command with fixed IP
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ping", "-4", "-I", interfaceName, "-c", "1", "1.1.1.1");
            // Start the process
            Process process = processBuilder.start();
            // Wait for the process to complete with a 50ms timeout
            boolean completed = process.waitFor(50, TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroy(); // Terminate the process if it times out
                return false;
            }
            // Read the output
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line;
            boolean success = false;
            // Check for successful ping response
            while ((line = reader.readLine()) != null) {
                if (line.contains("1 packets transmitted, 1 received") ||
                        line.contains("1 packets transmitted, 1 packets received")) {
                    success = true;
                    break;
                }
            }
            return success;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Map<String, Object> updateLanSetting(String inName, String ip, String gateWay, String subnet, String dns1,
            String dns2, String metric) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "");

        // არგუმენტების ძირითადი ვალიდაცია
        if (inName == null || inName.trim().isEmpty()) {
            response.put("message", "ინტერფეისის სახელი (inName) არ შეიძლება იყოს ცარიელი");
            logger.warn("Invalid inName: {}", inName);
            return response;
        }

        // IP მისამართის ვალიდაცია
        if (ip != null) {
            if (!ip.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
                response.put("message", "არასწორი IP მისამართის ფორმატი: " + ip + ". მაგ.: 192.168.1.60");
                logger.warn("Invalid IP format: {}", ip);
                return response;
            }
            String[] octets = ip.split("\\.");
            for (String octet : octets) {
                int value = Integer.parseInt(octet);
                if (value < 0 || value > 255) {
                    response.put("message", "IP მისამართის ოქტეტი უნდა იყოს 0-255 დიაპაზონში: " + ip);
                    logger.warn("Invalid IP octet range: {}", ip);
                    return response;
                }
            }
        }

        // Gateway-ს ვალიდაცია
        if (gateWay != null) {
            if (!gateWay.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
                response.put("message", "არასწორი Gateway მისამართის ფორმატი: " + gateWay + ". მაგ.: 192.168.1.1");
                logger.warn("Invalid Gateway format: {}", gateWay);
                return response;
            }
            String[] gwOctets = gateWay.split("\\.");
            for (String octet : gwOctets) {
                int value = Integer.parseInt(octet);
                if (value < 0 || value > 255) {
                    response.put("message", "Gateway მისამართის ოქტეტი უნდა იყოს 0-255 დიაპაზონში: " + gateWay);
                    logger.warn("Invalid Gateway octet range: {}", gateWay);
                    return response;
                }
            }
        }

        // სუბნეტის ვალიდაცია: CIDR (0-32) ან სუბნეტის მასკა (0.0.0.0 - 255.255.255.255)
        String cidrSubnet = null;
        if (subnet != null) {
            if (subnet.matches("^\\d{1,2}$")) {
                int subnetValue = Integer.parseInt(subnet);
                if (subnetValue < 0 || subnetValue > 32) {
                    response.put("message", "არასწორი CIDR სუბნეტი, უნდა იყოს 0-32: " + subnet);
                    logger.warn("Invalid CIDR subnet: {}", subnet);
                    return response;
                }
                cidrSubnet = subnet;
            } else if (subnet.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
                String[] octets = subnet.split("\\.");
                long mask = 0;
                for (String octet : octets) {
                    int value = Integer.parseInt(octet);
                    if (value < 0 || value > 255) {
                        response.put("message", "სუბნეტის მასკის ოქტეტი უნდა იყოს 0-255: " + subnet);
                        logger.warn("Invalid subnet octet range: {}", subnet);
                        return response;
                    }
                    mask = (mask << 8) + value;
                }
                // CIDR-ის გამოთვლა 0-დან 32-მდე
                int cidr = Long.bitCount(mask & 0xFFFFFFFFL);
                if (cidr > 32) {
                    response.put("message", "არასწორი სუბნეტის მასკა: " + subnet + ". CIDR უნდა იყოს 0-32");
                    logger.warn("Invalid subnet mask CIDR exceeds 32: {}", subnet);
                    return response;
                }
                cidrSubnet = String.valueOf(cidr);
            } else {
                response.put("message", "არასწორი სუბნეტის ფორმატი: " + subnet + ". მაგ.: 0.0.0.0 ან 0-32");
                logger.warn("Invalid subnet format: {}", subnet);
                return response;
            }
        }

        // DNS მისამართების ვალიდაცია
        if (dns1 != null) {
            if (!dns1.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
                response.put("message", "არასწორი DNS1 მისამართის ფორმატი: " + dns1 + ". მაგ.: 8.8.8.8");
                logger.warn("Invalid DNS1 format: {}", dns1);
                return response;
            }
            String[] dnsOctets = dns1.split("\\.");
            for (String octet : dnsOctets) {
                int value = Integer.parseInt(octet);
                if (value < 0 || value > 255) {
                    response.put("message", "DNS1 მისამართის ოქტეტი უნდა იყოს 0-255 დიაპაზონში: " + dns1);
                    logger.warn("Invalid DNS1 octet range: {}", dns1);
                    return response;
                }
            }
        }
        if (dns2 != null) {
            if (!dns2.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
                response.put("message", "არასწორი DNS2 მისამართის ფორმატი: " + dns2 + ". მაგ.: 8.8.4.4");
                logger.warn("Invalid DNS2 format: {}", dns2);
                return response;
            }
            String[] dnsOctets = dns2.split("\\.");
            for (String octet : dnsOctets) {
                int value = Integer.parseInt(octet);
                if (value < 0 || value > 255) {
                    response.put("message", "DNS2 მისამართის ოქტეტი უნდა იყოს 0-255 დიაპაზონში: " + dns2);
                    logger.warn("Invalid DNS2 octet range: {}", dns2);
                    return response;
                }
            }
        }

        // Metric-ის ვალიდაცია
        if (metric != null) {
            if (!metric.matches("^\\d+$")) {
                response.put("message",
                        "არასწორი metric ფორმატი: " + metric + ". უნდა იყოს მთელი რიცხვი (მაგ.: 0, 100)");
                logger.warn("Invalid metric format: {}", metric);
                return response;
            }
            int metricValue = Integer.parseInt(metric);
            if (metricValue < 0 || metricValue > 1000) {
                response.put("message", "Metric უნდა იყოს 0-1000 დიაპაზონში: " + metric);
                logger.warn("Invalid metric range: {}", metric);
                return response;
            }
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            List<NetMod> nets = mapper.readValue(LAN_JSON, new TypeReference<List<NetMod>>() {
            });
            boolean found = false;

            // IP კონფლიქტის შემოწმება
            if (ip != null) {
                for (NetMod nt : nets) {
                    if (!nt.getInName().equals(inName) && ip.equals(nt.getIp())) {
                        response.put("message",
                                "IP მისამართი " + ip + " უკვე გამოიყენება ინტერფეისის " + nt.getInName() + "-ის მიერ");
                        logger.warn("IP conflict: {} already used by {}", ip, nt.getInName());
                        return response;
                    }
                }
            }

            // ინტერფეისის ძებნა და განახლება
            for (NetMod nt : nets) {
                if (nt.getInName().equals(inName)) {
                    nt.setIp(ip != null ? ip : nt.getIp());
                    nt.setGateWay(gateWay != null ? gateWay : nt.getGateWay());
                    nt.setSubnet(cidrSubnet != null ? cidrSubnet : nt.getSubnet());
                    nt.setDns1(dns1 != null ? dns1 : nt.getDns1());
                    nt.setDns2(dns2 != null ? dns2 : nt.getDns2());
                    nt.setMetric(metric != null ? metric : nt.getMetric());
                    found = true;
                    break;
                }
            }

            if (!found) {
                response.put("message", "ინტერფეისი " + inName + " ვერ მოიძებნა");
                logger.warn("Interface not found: {}", inName);
                return response;
            }

            // განახლებული სიის JSON-ში ჩაწერა
            mapper.writerWithDefaultPrettyPrinter().writeValue(LAN_JSON, nets);

            // YAML ფაილის განახლება
            writeNetYaml();

            response.put("success", true);
            response.put("message", "ინტერფეისი " + inName + " წარმატებით განახლდა");
            logger.info("Successfully updated interface: {}", inName);

        } catch (IOException e) {
            logger.error("Error updating JSON or YAML: {}", e.getMessage(), e);
            response.put("message", "შეცდომა JSON-ის ან YAML-ის განახლებისას: " + e.getMessage());
        }

        return response;
    }

    public void writeNetYaml() {
        try {
            logger.info("Writing JSON to YAML: {}", LAN_YAML.getAbsolutePath());
            ObjectMapper mapper = new ObjectMapper();
            List<NetMod> nets = mapper.readValue(LAN_JSON, new TypeReference<List<NetMod>>() {
            });

            Map<String, Object> yamlData = new LinkedHashMap<>();
            Map<String, Object> network = new LinkedHashMap<>();
            network.put("version", 2);
            Map<String, Object> ethernets = new LinkedHashMap<>();

            for (NetMod netMod : nets) {
                Map<String, Object> interfaceData = new LinkedHashMap<>();
                interfaceData.put("dhcp4", false);

                List<String> addresses = new ArrayList<>();
                if (netMod.getIp() != null && netMod.getSubnet() != null) {
                    addresses.add(netMod.getIp() + "/" + netMod.getSubnet());
                }
                interfaceData.put("addresses", addresses);

                Map<String, Object> nameservers = new LinkedHashMap<>();
                List<String> dnsAddresses = new ArrayList<>();
                if (netMod.getDns1() != null) {
                    dnsAddresses.add(netMod.getDns1());
                }
                if (netMod.getDns2() != null) {
                    dnsAddresses.add(netMod.getDns2());
                }
                nameservers.put("addresses", dnsAddresses);
                interfaceData.put("nameservers", nameservers);

                List<Map<String, Object>> routes = new ArrayList<>();
                if (netMod.getGateWay() != null && netMod.getMetric() != null) {
                    Map<String, Object> route = new LinkedHashMap<>();
                    route.put("to", "default");
                    route.put("via", netMod.getGateWay());
                    route.put("metric", Integer.parseInt(netMod.getMetric()));
                    routes.add(route);
                }
                interfaceData.put("routes", routes);

                interfaceData.put("optional", true);
                ethernets.put(netMod.getInName(), interfaceData);
            }

            network.put("ethernets", ethernets);
            yamlData.put("network", network);

            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            options.setIndent(2);
            options.setIndicatorIndent(0);

            Yaml yaml = new Yaml(options);
            try (FileWriter writer = new FileWriter(LAN_YAML)) {
                yaml.dump(yamlData, writer);
            }
            logger.info("Successfully converted JSON to YAML and saved to: {}", LAN_YAML.getAbsolutePath());

        } catch (IOException e) {
            logger.error("Error converting JSON to YAML: {}", e.getMessage(), e);
        }
    }

    private List<NetMod> parseYamlToNetMod(Map<String, Object> yamlData) {
        List<NetMod> netMods = new ArrayList<>();
        if (yamlData == null || !yamlData.containsKey("network")) {
            logger.warn("YAML file is empty or missing 'network' key");
            return netMods;
        }

        Map<String, Object> network = (Map<String, Object>) yamlData.get("network");
        if (!network.containsKey("ethernets")) {
            logger.warn("YAML file is missing 'ethernets' key");
            return netMods;
        }

        Map<String, Object> ethernets = (Map<String, Object>) network.get("ethernets");
        for (Map.Entry<String, Object> entry : ethernets.entrySet()) {
            String inName = entry.getKey();
            Map<String, Object> interfaceData = (Map<String, Object>) entry.getValue();

            NetMod netMod = new NetMod();
            netMod.setInName(inName);

            if (interfaceData.containsKey("addresses")) {
                List<String> addresses = (List<String>) interfaceData.get("addresses");
                if (!addresses.isEmpty()) {
                    String address = addresses.get(0).replaceAll("\"", "");
                    String[] addressParts = address.split("/");
                    netMod.setIp(addressParts[0]);
                    netMod.setSubnet(addressParts.length > 1 ? addressParts[1] : "");
                }
            }

            if (interfaceData.containsKey("nameservers")) {
                Map<String, Object> nameservers = (Map<String, Object>) interfaceData.get("nameservers");
                if (nameservers.containsKey("addresses")) {
                    List<String> dnsAddresses = (List<String>) nameservers.get("addresses");
                    if (dnsAddresses.size() >= 1) {
                        netMod.setDns1(dnsAddresses.get(0));
                    }
                    if (dnsAddresses.size() >= 2) {
                        netMod.setDns2(dnsAddresses.get(1));
                    }
                }
            }

            if (interfaceData.containsKey("routes")) {
                List<Map<String, Object>> routes = (List<Map<String, Object>>) interfaceData.get("routes");
                if (!routes.isEmpty()) {
                    Map<String, Object> route = routes.get(0);
                    String gateway = String.valueOf(route.get("via")).replaceAll("\"", "");
                    netMod.setGateWay(gateway);
                    netMod.setMetric(String.valueOf(route.get("metric")));
                }
            }

            netMods.add(netMod);
        }

        logger.info("Parsed {} network interfaces from YAML", netMods.size());
        return netMods;
    }

}