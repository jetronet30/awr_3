package com.jaba.awr_3.core.numberdetection.ocr;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jaba.awr_3.core.units.UnitService;
import com.jaba.awr_3.inits.repo.RepoInit;

@Service
public class OcrService {

    private static final File OCR_SETTINGS = new File(RepoInit.SERVER_SETTINGS_REPO, "ocrsettings.json");
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final Logger LOGGER = LoggerFactory.getLogger(OcrService.class);

    // Thread-safe file access
    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();

    // === Initialization (static, thread-safe) ===
    public static void initOcr() {
        LOCK.writeLock().lock();
        try {
            if (OCR_SETTINGS.exists()) {
                LOGGER.info("ocrsettings.json already exists: {}", OCR_SETTINGS.getAbsolutePath());
                return;
            }

            List<OcrMod> ocrModels = new ArrayList<>();

            OcrMod mod0 = new OcrMod(0, "admin", "admin", "admin", "admin", 554, 554,
                    "rtsp://admin:admin@192.168.1.11:554", "rtsp://admin:admin@192.168.1.12:554", 0.1, 0.1, 0.9, 0.9,
                    0.8, 0.4, 40, 140, RepoInit.CAM_0_REPO.getAbsolutePath(), RepoInit.VIDEO_ARCHIVE.getAbsolutePath(),
                    RepoInit.YOLO_AND_TROCR.getAbsolutePath() + "/best.pt",
                    RepoInit.YOLO_AND_TROCR.getAbsolutePath() + "/trocr-large-printed", UnitService.W_NUM_LEN, true,
                    true);

            OcrMod mod1 = new OcrMod(1, "admin", "admin", "admin", "admin", 554, 554,
                    "rtsp://admin:admin@192.168.1.11:554", "rtsp://admin:admin@192.168.1.12:554", 0.1, 0.1, 0.9, 0.9,
                    0.8, 0.4, 40, 140, RepoInit.CAM_1_REPO.getAbsolutePath(), RepoInit.VIDEO_ARCHIVE.getAbsolutePath(),
                    RepoInit.YOLO_AND_TROCR.getAbsolutePath() + "/best.pt",
                    RepoInit.YOLO_AND_TROCR.getAbsolutePath() + "/trocr-large-printed", UnitService.W_NUM_LEN, true,
                    true);

            OcrMod mod2 = new OcrMod(2, "admin", "admin", "admin", "admin", 554, 554,
                    "rtsp://admin:admin@192.168.1.11:554", "rtsp://admin:admin@192.168.1.12:554", 0.1, 0.1, 0.9, 0.9,
                    0.8, 0.4, 40, 140, RepoInit.CAM_2_REPO.getAbsolutePath(), RepoInit.VIDEO_ARCHIVE.getAbsolutePath(),
                    RepoInit.YOLO_AND_TROCR.getAbsolutePath() + "/best.pt",
                    RepoInit.YOLO_AND_TROCR.getAbsolutePath() + "/trocr-large-printed", UnitService.W_NUM_LEN, true,
                    true);

            OcrMod mod3 = new OcrMod(3, "admin", "admin", "admin", "admin", 554, 554,
                    "rtsp://admin:admin@192.168.1.11:554", "rtsp://admin:admin@192.168.1.12:554", 0.1, 0.1, 0.9, 0.9,
                    0.8, 0.4, 40, 140, RepoInit.CAM_3_REPO.getAbsolutePath(), RepoInit.VIDEO_ARCHIVE.getAbsolutePath(),
                    RepoInit.YOLO_AND_TROCR.getAbsolutePath() + "/best.pt",
                    RepoInit.YOLO_AND_TROCR.getAbsolutePath() + "/trocr-large-printed", UnitService.W_NUM_LEN, true,
                    true);

            OcrMod mod4 = new OcrMod(4, "admin", "admin", "admin", "admin", 554, 554,
                    "rtsp://admin:admin@192.168.1.11:554", "rtsp://admin:admin@192.168.1.12:554", 0.1, 0.1, 0.9, 0.9,
                    0.8, 0.4, 40, 140, RepoInit.CAM_4_REPO.getAbsolutePath(), RepoInit.VIDEO_ARCHIVE.getAbsolutePath(),
                    RepoInit.YOLO_AND_TROCR.getAbsolutePath() + "/best.pt",
                    RepoInit.YOLO_AND_TROCR.getAbsolutePath() + "/trocr-large-printed", UnitService.W_NUM_LEN, true,
                    true);
            OcrMod mod5 = new OcrMod(5, "admin", "admin", "admin", "admin", 554, 554,
                    "rtsp://admin:admin@192.168.1.11:554", "rtsp://admin:admin@192.168.1.12:554", 0.1, 0.1, 0.9, 0.9,
                    0.8, 0.4, 40, 140, RepoInit.CAM_5_REPO.getAbsolutePath(), RepoInit.VIDEO_ARCHIVE.getAbsolutePath(),
                    RepoInit.YOLO_AND_TROCR.getAbsolutePath() + "/best.pt",
                    RepoInit.YOLO_AND_TROCR.getAbsolutePath() + "/trocr-large-printed", UnitService.W_NUM_LEN, true,
                    true);
            OcrMod mod6 = new OcrMod(6, "admin", "admin", "admin", "admin", 554, 554,
                    "rtsp://admin:admin@192.168.1.11:554", "rtsp://admin:admin@192.168.1.12:554", 0.1, 0.1, 0.9, 0.9,
                    0.8, 0.4, 40, 140, RepoInit.CAM_6_REPO.getAbsolutePath(), RepoInit.VIDEO_ARCHIVE.getAbsolutePath(),
                    RepoInit.YOLO_AND_TROCR.getAbsolutePath() + "/best.pt",
                    RepoInit.YOLO_AND_TROCR.getAbsolutePath() + "/trocr-large-printed", UnitService.W_NUM_LEN, true,
                    true);
            OcrMod mod7 = new OcrMod(7, "admin", "admin", "admin", "admin", 554, 554,
                    "rtsp://admin:admin@192.168.1.11:554", "rtsp://admin:admin@192.168.1.12:554", 0.1, 0.1, 0.9, 0.9,
                    0.8, 0.4, 40, 140, RepoInit.CAM_7_REPO.getAbsolutePath(), RepoInit.VIDEO_ARCHIVE.getAbsolutePath(),
                    RepoInit.YOLO_AND_TROCR.getAbsolutePath() + "/best.pt",
                    RepoInit.YOLO_AND_TROCR.getAbsolutePath() + "/trocr-large-printed", UnitService.W_NUM_LEN, true,
                    true);
            OcrMod mod8 = new OcrMod(8, "admin", "admin", "admin", "admin", 554, 554,
                    "rtsp://admin:admin@192.168.1.11:554", "rtsp://admin:admin@192.168.1.12:554", 0.1, 0.1, 0.9, 0.9,
                    0.8, 0.4, 40, 140, RepoInit.CAM_8_REPO.getAbsolutePath(), RepoInit.VIDEO_ARCHIVE.getAbsolutePath(),
                    RepoInit.YOLO_AND_TROCR.getAbsolutePath() + "/best.pt",
                    RepoInit.YOLO_AND_TROCR.getAbsolutePath() + "/trocr-large-printed", UnitService.W_NUM_LEN, true,
                    true);
            OcrMod mod9 = new OcrMod(9, "admin", "admin", "admin", "admin", 554, 554,
                    "rtsp://admin:admin@192.168.1.11:554", "rtsp://admin:admin@192.168.1.12:554", 0.1, 0.1, 0.9, 0.9,
                    0.8, 0.4, 40, 140, RepoInit.CAM_9_REPO.getAbsolutePath(), RepoInit.VIDEO_ARCHIVE.getAbsolutePath(),
                    RepoInit.YOLO_AND_TROCR.getAbsolutePath() + "/best.pt",
                    RepoInit.YOLO_AND_TROCR.getAbsolutePath() + "/trocr-large-printed", UnitService.W_NUM_LEN, true,
                    true);

            ocrModels.addAll(Arrays.asList(mod0, mod1, mod2, mod3, mod4, mod5, mod6, mod7, mod8, mod9));
            MAPPER.writeValue(OCR_SETTINGS, ocrModels);
            LOGGER.info("ocrsettings.json created successfully: {} entries, path: {}",
                    ocrModels.size(), OCR_SETTINGS.getAbsolutePath());

        } catch (IOException e) {
            LOGGER.error("Failed to create ocrsettings.json", e);
        } catch (Exception ex) {
            LOGGER.error("Unexpected error during OCR initialization", ex);
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    // === Safe file read ===
    private List<OcrMod> readOcrModels() throws IOException {
        LOCK.readLock().lock();
        try {
            if (!OCR_SETTINGS.exists()) {
                LOGGER.warn("ocrsettings.json does not exist");
                return new ArrayList<>();
            }
            return MAPPER.readValue(OCR_SETTINGS, new TypeReference<List<OcrMod>>() {
            });
        } finally {
            LOCK.readLock().unlock();
        }
    }

    // === Safe file write ===
    private void writeOcrModels(List<OcrMod> models) throws IOException {
        LOCK.writeLock().lock();
        try {
            MAPPER.writeValue(OCR_SETTINGS, models);
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    // === List all ===
    public List<OcrMod> listOcrModels() {
        try {
            List<OcrMod> models = readOcrModels();
            LOGGER.debug("OCR models list requested: {} entries", models.size());
            return models;
        } catch (IOException e) {
            LOGGER.error("Failed to read OCR models list", e);
            return new ArrayList<>();
        }
    }

    public OcrMod getOcrByIndex(int index) {
        for (OcrMod mod : listOcrModels()) {
            if (mod.getIndex() == index) {
                LOGGER.debug("OCR model found by index: {}", index);
                return mod;
            }
        }
        LOGGER.debug("OCR model not found by index: {}", index);
        return null;
    }

    // === Full update by index ===
    public Map<String, Object> updateOcrByIndex(
            int index,
            String cam1Usr, String cam2Usr,
            String cam1Passwd, String cam2Passwd,
            int cam1Port, int cam2Port,
            String rtspUrl1, String rtspUrl2,
            double roiX1, double roiY1, double roiX2, double roiY2,
            boolean activeDetection, boolean activeStream) {

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);

        if (index < 0 || index > 19) {
            response.put("error", "Index must be between 0 and 19");
            return response;
        }
        if (cam1Port < 1 || cam1Port > 65535 || cam2Port < 1 || cam2Port > 65535) {
            response.put("error", "Invalid port number(s)");
            return response;
        }
        if (roiX1 < 0 || roiX1 > 1 || roiY1 < 0 || roiY1 > 1 ||
                roiX2 < 0 || roiX2 > 1 || roiY2 < 0 || roiY2 > 1 ||
                roiX1 >= roiX2 || roiY1 >= roiY2) {
            response.put("error", "Invalid ROI coordinates");
            return response;
        }

        try {
            List<OcrMod> models = readOcrModels();
            boolean found = false;

            for (OcrMod mod : models) {
                if (mod.getIndex() == index) {
                    mod.setCam1Usr(cam1Usr.trim());
                    mod.setCam2Usr(cam2Usr.trim());
                    mod.setCam1Passwd(cam1Passwd.trim());
                    mod.setCam2Passwd(cam2Passwd.trim());
                    mod.setCam1Port(cam1Port);
                    mod.setCam2Port(cam2Port);
                    mod.setRtspUrl_1(rtspUrl1.trim());
                    mod.setRtspUrl_2(rtspUrl2.trim());
                    mod.setRoiX1(roiX1);
                    mod.setRoiY1(roiY1);
                    mod.setRoiX2(roiX2);
                    mod.setRoiY2(roiY2);
                    mod.setVideoArChive(RepoInit.VIDEO_ARCHIVE.getAbsolutePath());
                    mod.setHlsRepo(getOcrByIndex(index).getHlsRepo());
                    mod.setTROCModel(RepoInit.YOLO_AND_TROCR.getAbsolutePath() + "/trocr-large-printed");
                    mod.setYoloModel(RepoInit.YOLO_AND_TROCR.getAbsolutePath() + "/best.pt");
                    mod.setWagonNumberLength(UnitService.W_NUM_LEN);
                    mod.setActiveDetection(activeDetection);
                    mod.setActiveStream(activeStream);
                    found = true;
                    break;
                }
            }

            if (!found) {
                response.put("error", "OCR model not found with index: " + index);
                return response;
            }

            writeOcrModels(models);
            response.put("success", true);
            response.put("message", "OCR settings updated successfully for index " + index);

        } catch (IOException e) {
            LOGGER.error("Failed to update OCR model index {}", index, e);
            response.put("error", "File write error");
        }

        return response;
    }

    public void uploadYoloModel(MultipartFile uploadFile) {
        if (uploadFile == null || uploadFile.isEmpty()) {
            throw new IllegalArgumentException("ფაილი ცარიელია ან არ არის ატვირთული.");
        }

        final Logger logger = LoggerFactory.getLogger(getClass());

        // ფაილის სახელი უნდა იყოს ზუსტად "best.pt" (შეგიძლია შეცვალო თუ გინდა სხვა
        // სახელი)
        final String EXPECTED_FILENAME = "best.pt";

        // ვალიდაცია: ორიგინალური სახელი უნდა იყოს best.pt
        String originalFilename = uploadFile.getOriginalFilename();
        if (originalFilename == null || !originalFilename.equalsIgnoreCase(EXPECTED_FILENAME)) {
            throw new IllegalArgumentException(
                    "ატვირთული ფაილი უნდა ერქვას ზუსტად '" + EXPECTED_FILENAME + "'. " +
                            "მიღებული: " + (originalFilename != null ? originalFilename : "უსახელო"));
        }

        // სურვილისამებრ: ზომის ლიმიტი (YOLO .pt ფაილები ჩვეულებრივ 6-100 MB-ია, მაგრამ
        // დიდი მოდელები შეიძლება იყოს უფრო დიდი)
        final long MAX_SIZE_BYTES = 500L * 1024 * 1024; // 500 MB
        if (uploadFile.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    String.format("ფაილი ძალიან დიდია. მაქსიმუმ: %,d MB (ახლანდელი: ~%,.1f MB)",
                            MAX_SIZE_BYTES / (1024 * 1024),
                            uploadFile.getSize() / (1024.0 * 1024.0)));
        }

        // სურვილისამებრ: გაფართოების შემოწმება (case-insensitive)
        if (!originalFilename.toLowerCase().endsWith(".pt")) {
            throw new IllegalArgumentException("ფაილი უნდა იყოს .pt გაფართოებით (PyTorch YOLO weights)");
        }

        try {
            // საქაღალდე სადაც შეინახება (შენი RepoInit-დან)
            Path uploadDir = Paths.get(RepoInit.YOLO_AND_TROCR.getAbsolutePath());

            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                logger.info("შეიქმნა დირექტორია YOLO/TROCR მოდელებისთვის: {}", uploadDir);
            }

            // საბოლოო გზა: .../yolo_and_trocr/best.pt
            Path targetPath = uploadDir.resolve(EXPECTED_FILENAME);

            // ატომარული ჩაწერა: ჯერ temp-ში → შემდეგ rename (თავიდან ავიცილებთ ნახევრად
            // ჩაწერილ ფაილს)
            Path tempPath = targetPath.resolveSibling(EXPECTED_FILENAME + ".tmp");

            // ჩაწერა დროებით ფაილში
            Files.copy(uploadFile.getInputStream(), tempPath, StandardCopyOption.REPLACE_EXISTING);

            // თუ ყველაფერი OK → ატომარული გადატანა (rename)
            Files.move(tempPath, targetPath,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);

            logger.info("YOLO best.pt წარმატებით აიტვირთა: {} → {} (ზომა: {} bytes)",
                    originalFilename, targetPath, uploadFile.getSize());

            // სურვილისამებრ: აქ შეგიძლია დაამატო ლოგიკა, რომ განაახლო მოდელი runtime-ში
            // მაგალითად: YOLOService.reloadModel(targetPath.toString());

        } catch (IOException e) {
            logger.error("YOLO best.pt ატვირთვა ჩაიშალა: {}", uploadFile.getOriginalFilename(), e);
            throw new RuntimeException("YOLO მოდელის (best.pt) შენახვა ვერ მოხერხდა: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("მოულოდნელი შეცდომა YOLO best.pt ატვირთვისას", e);
            throw new RuntimeException("შეუძლებელია YOLO best.pt-ის შენახვა", e);
        }
    }

    public void uploadTrocrModel(MultipartFile uploadFile) {
        if (uploadFile == null || uploadFile.isEmpty()) {
            throw new IllegalArgumentException("ფაილი ცარიელია ან არ არის ატვირთული.");
        }

        final Logger logger = LoggerFactory.getLogger(getClass());

        // ფაილი უნდა იყოს ZIP
        String originalFilename = uploadFile.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException(
                    "TROCR მოდელისთვის უნდა ატვირთოთ ZIP არქივი (.zip). მიღებული: " +
                            (originalFilename != null ? originalFilename : "უსახელო"));
        }

        // ზომის ლიმიტი (მაგ. 5GB – შეცვალე საჭიროებისამებრ)
        final long MAX_SIZE_BYTES = 5L * 1024 * 1024 * 1024;
        if (uploadFile.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    String.format("ZIP ფაილი ძალიან დიდია. მაქსიმუმ: %,d MB (ახლანდელი: ~%,.1f MB)",
                            MAX_SIZE_BYTES / (1024 * 1024),
                            uploadFile.getSize() / (1024.0 * 1024.0)));
        }

        try {
            // საბაზისო დირექტორია (შენი RepoInit-დან)
            Path baseDir = Paths.get(RepoInit.YOLO_AND_TROCR.getAbsolutePath());

            if (!Files.exists(baseDir)) {
                Files.createDirectories(baseDir);
                logger.info("შეიქმნა დირექტორია TROCR მოდელებისთვის: {}", baseDir);
            }

            // TROCR-ისთვის ცალკე sub-folder (რომ არ გადაფაროს YOLO)
            // შეგიძლია შეცვალო სახელი, მაგ. "trocr-model" ან "trocr-v2024"
            

               // გადაწერა
            // Files.walk(baseDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            

            if (!Files.exists(baseDir)) {
                Files.createDirectories(baseDir);
            }

            // ატომარული unzip: ჯერ temp ZIP → extract
            Path tempZipPath = baseDir.resolveSibling("trocr-temp-" + System.currentTimeMillis() + ".zip");

            // ZIP-ის შენახვა დროებით
            Files.copy(uploadFile.getInputStream(), tempZipPath, StandardCopyOption.REPLACE_EXISTING);

            // Unzip ლოგიკა + Zip Slip დაცვა
            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(tempZipPath));
                    BufferedInputStream bis = new BufferedInputStream(zis)) {

                ZipEntry zipEntry;
                while ((zipEntry = zis.getNextEntry()) != null) {
                    Path resolvedPath = baseDir.resolve(zipEntry.getName()).normalize();

                    // Zip Slip თავიდან აცილება: გზა უნდა იყოს extractDir-ის შიგნით
                    if (!resolvedPath.startsWith(baseDir.normalize())) {
                        throw new IOException("Zip Slip vulnerability detected: " + zipEntry.getName());
                    }

                    if (zipEntry.isDirectory()) {
                        Files.createDirectories(resolvedPath);
                    } else {
                        // დარწმუნდი, რომ parent directory არსებობს
                        Files.createDirectories(resolvedPath.getParent());

                        // ჩაწერა
                        try (OutputStream os = Files.newOutputStream(resolvedPath, StandardOpenOption.CREATE,
                                StandardOpenOption.TRUNCATE_EXISTING)) {
                            byte[] buffer = new byte[8192];
                            int len;
                            while ((len = bis.read(buffer)) > 0) {
                                os.write(buffer, 0, len);
                            }
                        }
                    }

                    zis.closeEntry();
                }
            }

            // წაშალე დროებითი ZIP
            Files.deleteIfExists(tempZipPath);

            logger.info("TROCR მოდელი (ZIP) წარმატებით ამოარქივდა: {} → {} (ზომა: {} bytes)",
                    originalFilename, baseDir, uploadFile.getSize());

            // სურვილისამებრ: აქ შეგიძლია დაამატო მოდელის ხელახლა ჩატვირთვა / reload
            // მაგალითად: TrocrService.reloadModel(extractDir.toString());

        } catch (IOException e) {
            logger.error("TROCR ZIP არქივის ამოღება / ატვირთვა ჩაიშალა: {}", originalFilename, e);
            throw new RuntimeException("TROCR მოდელის (ZIP) ამოღება ვერ მოხერხდა: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("მოულოდნელი შეცდომა TROCR ZIP ატვირთვისას", e);
            throw new RuntimeException("შეუძლებელია TROCR ZIP-ის დამუშავება", e);
        }
    }

}