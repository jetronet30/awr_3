package com.jaba.awr_3.seversettings.network;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;



@Service
public class DefaultNetwork {
    private static final File LAN_YAML = new File("/etc/netplan/50-cloud-init.yaml");
    private static final File SYS_NET = new File("/sys/class/net");
    

    public static void createDefaultNetPlan() {
        if (!LAN_YAML.exists()) {
            List<String> interfaces = getNetNames();
            if (!interfaces.isEmpty()) {
                try (FileWriter writer = new FileWriter(LAN_YAML)) {
                    writer.write("network:\n");
                    writer.write("  version: 2\n");
                    writer.write("  ethernets:\n");

                    int ipCounter = 60;      // IP 192.168.1.60–დან
                    int baseMetric = 0;      // პირველი metric
                    int metricIncrement = 100; // metric ზრდა შემდეგებზე

                    for (int i = 0; i < interfaces.size(); i++) {
                        String iface = interfaces.get(i);
                        String ip = "192.168.1." + ipCounter + "/24";
                        int metric = (i == 0) ? baseMetric : baseMetric + i * metricIncrement;

                        writer.write("    " + iface + ":\n");
                        writer.write("      dhcp4: false\n");
                        writer.write("      addresses:\n");
                        writer.write("      - \"" + ip + "\"\n");
                        writer.write("      nameservers:\n");
                        writer.write("        addresses:\n");
                        writer.write("        - 8.8.8.8\n");
                        writer.write("        - 8.8.4.4\n");
                        writer.write("      routes:\n");
                        writer.write("      - to: \"default\"\n");
                        writer.write("        via: \"192.168.1.1\"\n");
                        writer.write("        metric: " + metric + "\n");
                        writer.write("      optional: true\n");

                        ipCounter++;
                    }

                    writer.flush();
                    System.out.println("Netplan configuration created at " + LAN_YAML.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("No network interfaces found in " + SYS_NET.getAbsolutePath());
            }
        } else {
            System.out.println("Netplan YAML already exists: " + LAN_YAML.getAbsolutePath());
        }
    }

    private static List<String> getNetNames() {
        List<String> netNames = new ArrayList<>();
        if (SYS_NET.exists() && SYS_NET.isDirectory()) {
            File[] files = SYS_NET.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory() && !"lo".equals(f.getName())) {
                        netNames.add(f.getName());
                    }
                }
            }
        }
        return netNames;
    }
}
