package com.jaba.awr_3.core.sysutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jaba.awr_3.inits.repo.RepoInit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe SYSID service:
 * - ქმნის ფაილს 0-ით თუ არ არსებობს
 * - ყოველი getSysId() → +1 და ფაილში წერა
 * - Long.MAX_VALUE-ზე → reset 0-ზე
 * - უსაფრთხოა მრავალნაკადიან გარემოში
 */
public final class SysIdService {

    private static final Logger log = LoggerFactory.getLogger(SysIdService.class);

    private static final File FILE = new File(RepoInit.UTYL_REPO, "SYSID.json");
    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    // ReentrantLock – უფრო მარტივი და საკმარისია ამ შემთხვევაში
    private static final ReentrantLock lock = new ReentrantLock();

    // init ერთხელ გამოიძახება (მაგ. @PostConstruct / static block / აპლიკაციის
    // დასაწყისში)
    public static void init() {
        if (FILE.exists()) {
            return;
        }

        File parent = FILE.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        lock.lock();
        try {
            // ორმაგი შემოწმება lock-ის შიგნით
            if (!FILE.exists()) {
                Files.write(FILE.toPath(), "0".getBytes(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                log.info("SYSID file created with value 0");
            }
        } catch (IOException e) {
            log.error("Failed to create SYSID file", e);
        } finally {
            lock.unlock();
        }
    }

    public static long getSysId() {
        lock.lock();
        try {
            long current;

            if (!FILE.exists()) {
                current = 0L;
            } else {
                try {
                    current = mapper.readValue(FILE, Long.class);
                } catch (Exception e) {
                    log.warn("Invalid SYSID file content, treating as 0", e);
                    current = 0L;
                }
            }

            // MAX_VALUE-ზე reset
            if (current == Long.MAX_VALUE) {
                current = 0L;
                log.info("SYSID reached MAX_VALUE → reset to 0");
            } else {
                current++;
            }

            // ჩაწერა ფაილში
            try {
                mapper.writeValue(FILE, current);
            } catch (IOException e) {
                log.error("Failed to write SYSID: {}", current, e);
                // აქ შეგიძლია throw RuntimeException თუ გინდა ძლიერი fail-fast
            }

            log.debug("SYSID returned and saved: {}", current);
            return current;

        } finally {
            lock.unlock();
        }
    }

    // დამხმარე მეთოდი – მიმდინარე მნიშვნელობის წაკითხვა (არ ზრდის)
    public static long peekCurrent() {
        lock.lock();
        try {
            if (!FILE.exists()) {
                return 0L;
            }
            return mapper.readValue(FILE, Long.class);
        } catch (Exception e) {
            log.warn("Cannot peek SYSID", e);
            return 0L;
        } finally {
            lock.unlock();
        }
    }

    // საჭიროებისამებრ – ძალით reset (ტესტირება / ადმინის მოქმედება)
    public static void resetToZeroUnsafe() {
        lock.lock();
        try {
            mapper.writeValue(FILE, 0L);
            log.warn("SYSID manually reset to 0");
        } catch (IOException e) {
            log.error("Reset failed", e);
        } finally {
            lock.unlock();
        }
    }
}