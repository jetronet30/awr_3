package com.jaba.awr_3.servermanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.concurrent.*;

public class ServerManager {

    private static final Logger log = LoggerFactory.getLogger(ServerManager.class);
    private static final int PROCESS_TIMEOUT_SECONDS = 30; // ბრძანების შესრულების დროის ლიმიტი (წამებში)

    /**
     * ასრულებს სისტემურ ბრძანებას განსაზღვრული დროის ლიმიტით, აღრიცხავს stdout-სა
     * და stderr-ს.
     *
     * @param cmd ბრძანება და მისი არგუმენტები.
     * @return true თუ exit code == 0, წინააღმდეგ შემთხვევაში false.
     */
    private static boolean executeCommand(String... cmd) {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true); // stdout-ისა და stderr-ის გაერთიანება

        try {
            log.info("ბრძანების შესრულება: {}", String.join(" ", cmd));
            Process process = pb.start();

            // გამომავალი სტრიმის ასინქრონულად წაკითხვა
            CompletableFuture<String> outputFuture = CompletableFuture.supplyAsync(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    StringBuilder out = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        out.append(line).append(System.lineSeparator());
                    }
                    return out.toString();
                } catch (IOException e) {
                    log.error("შეცდომა გამომავალი სტრიმის წაკითხვისას", e);
                    return "";
                }
            });

            boolean finished = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.error("ბრძანებამ გადააჭარბა დროის ლიმიტს (>{} წამი): {}", PROCESS_TIMEOUT_SECONDS,
                        String.join(" ", cmd));
                return false;
            }

            int exitCode = process.exitValue();
            String output = outputFuture.get(1, TimeUnit.SECONDS).trim();

            if (!output.isEmpty()) {
                log.info("ბრძანების გამომავალი: {}", output);
            }

            if (exitCode == 0) {
                log.info("ბრძანება წარმატებით შესრულდა: {}", String.join(" ", cmd));
                return true;
            } else {
                log.error("ბრძანება ჩაიშალა, exit code {}: {}", exitCode, String.join(" ", cmd));
                return false;
            }
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            log.error("შეცდომა ბრძანების შესრულებისას: {}", String.join(" ", cmd), e);
            return false;
        }
    }

    /**
     * სისტემის გადატვირთვა.
     *
     * @return true თუ წარმატებულია, წინააღმდეგ შემთხვევაში false.
     */
    public static boolean reboot() {
        return executeCommand("reboot");
    }

    /**
     * სისტემის დაუყოვნებლივ გამორთვა.
     *
     * @return true თუ წარმატებულია, წინააღმდეგ შემთხვევაში false.
     */
    public static boolean shutdown() {
        return executeCommand("shutdown", "now");
    }

    /**
     * ქსელის გადატვირთვა netplan-ის კონფიგურაციის გამოყენებით.
     *
     * @return true თუ წარმატებულია, წინააღმდეგ შემთხვევაში false.
     */
    public static boolean restartNetwork() {
        log.info("ქსელის კონფიგურაციის გამოყენება 'netplan apply'-ით...");
        return executeCommand("netplan", "apply");
    }

    /**
     * ფოლდერის რეკურსიულად წაშლა - გამოიყენება სისტემის საწყის მდგომარეობაში
     * დასაბრუნებლად.
     *
     * @param folderPath ფოლდერის აბსოლუტური ან ფარდობითი გზა.
     * @return true თუ ფოლდერი წარმატებით წაიშალა, წინააღმდეგ შემთხვევაში false.
     */
    public static boolean factoryReset(String folderPath) {
        try {
            Path folder = Paths.get(folderPath);
            if (Files.notExists(folder)) {
                log.warn("ფოლდერი არ არსებობს: {}", folderPath);
                return false;
            }
            // რეკურსიული წაშლა
            Files.walk(folder)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            log.info("წაიშალა: {}", path);
                        } catch (IOException e) {
                            log.error("ვერ მოხერხდა წაშლა: {}", path, e);
                            throw new RuntimeException(e);
                        }
                    });
            log.info("საწყის მდგომარეობაში დაბრუნება წარმატებით დასრულდა: {}", folderPath);
            return true;
        } catch (Exception e) {
            log.error("საწყის მდგომარეობაში დაბრუნება ჩაიშალა: {}", folderPath, e);
            return false;
        }
    }

    /**
     * აბრუნებს სისტემის მიმდინარე დროს ფორმატირებულ სტრიქონად.
     *
     * @return ფორმატირებული დრო "yyyy-MM-dd HH:mm:ss".
     */
    public static String getSystemDateTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * აბრუნებს სისტემის მიმდინარე თარიღს ფორმატირებულ სტრიქონად.
     *
     * @return ფორმატირებული თარიღი "yyyy-MM-dd".
     */

    public static String getSystemDate() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    /**
     * აბრუნებს სისტემის ოპერირების დროს (uptime) მარტივ ფორმატში.
     *
     * @return uptime სტრიქონი ან "მიუწვდომელია" შეცდომის შემთხვევაში.
     */
    public static String getUptime() {
        try {
            ProcessBuilder pb = new ProcessBuilder("uptime", "-p");
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            process.waitFor(5, TimeUnit.SECONDS);
            return output.isEmpty() ? "მიუწვდომელია" : output;
        } catch (Exception e) {
            log.error("ვერ მოხერხდა uptime-ის მიღება", e);
            return "მიუწვდომელია";
        }
    }

    /**
     * სისტემის დროის სინქრონიზაცია NTP-ის გამოყენებით.
     *
     * @return true თუ წარმატებულია, წინააღმდეგ შემთხვევაში false.
     */
    public static boolean syncTimeWithNTP() {
        return executeCommand("timedatectl", "set-ntp", "true");
    }

}