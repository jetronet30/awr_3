package com.jaba.awr_3.core.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import com.jaba.awr_3.core.prodata.mod.TrainMod;
import com.jaba.awr_3.core.prodata.mod.WagonMod;
import com.jaba.awr_3.core.units.UnitService;
import com.jaba.awr_3.inits.repo.RepoInit;
import com.jaba.awr_3.seversettings.basic.BasicService;

import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfCreator {

    private static final File SAVE_REPO_LAST_0 = RepoInit.PDF_REPOSITOR_LAST_0;
    private static final File SAVE_REPO_LAST_1 = RepoInit.PDF_REPOSITOR_LAST_1;
    private static final File SAVE_REPO_LAST_2 = RepoInit.PDF_REPOSITOR_LAST_2;
    private static final File SAVE_REPO_LAST_3 = RepoInit.PDF_REPOSITOR_LAST_3;
    private static final File SAVE_REPO_LAST_4 = RepoInit.PDF_REPOSITOR_LAST_4;
    private static final File SAVE_REPO_LAST_5 = RepoInit.PDF_REPOSITOR_LAST_5;
    private static final File SAVE_REPO_LAST_6 = RepoInit.PDF_REPOSITOR_LAST_6;
    private static final File SAVE_REPO_LAST_7 = RepoInit.PDF_REPOSITOR_LAST_7;
    private static final File SAVE_REPO_LAST_8 = RepoInit.PDF_REPOSITOR_LAST_8;
    private static final File SAVE_REPO_LAST_9 = RepoInit.PDF_REPOSITOR_LAST_9;

    private static final float LEFT = 50;
    private static final float TOP = 800;
    private static final float LINE_H = 10;
    private static final float BOTTOM_MARGIN = 50;

    private static final float X_NO = LEFT;
    private static final float X_VEH = LEFT + 40;
    private static final float X_PROD = LEFT + 140;
    private static final float X_TARE = LEFT + 220;
    private static final float X_GROSS = LEFT + 280;
    private static final float X_NETT = LEFT + 340;
    private static final float X_SPEED = LEFT + 400;

    private FontManager fontManager;
    private int currentPageNumber;

    public void createPdfWeb(TrainMod train) {
        if (train == null || train.getWagons() == null || train.getWagons().isEmpty())
            return;
        String connId = train.getConId();
        try (PDDocument doc = new PDDocument()) {
            fontManager = new FontManager(doc);
            currentPageNumber = 1;
            PDPage page = addPage(doc);
            PDPageContentStream cs = new PDPageContentStream(doc, page);

            drawPageNumber(cs, page); // პირველი გვერდი

            // სათაური და ლოგო მოშორებულია

            float y = TOP - 40; // დავიწყოთ ოდნავ ქვემოთ, რადგან სათაური/ლოგო აღარ არის

            y = writeMainInfo(cs, y, train);
            y = writeTableHeader(cs, y);

            List<WagonMod> wagons = train.getWagons();
            wagons.sort(Comparator.comparingInt(WagonMod::getRowNum));
            int rowNumber = 1;

            for (WagonMod w : wagons) {
                EnsureResult er = ensureSpace(doc, cs, y, LINE_H);
                if (er.newPageCreated) {
                    page = er.newPage;
                    cs.close();
                    cs = new PDPageContentStream(doc, page);
                    currentPageNumber++;
                    drawPageNumber(cs, page); // ახალი გვერდის ნომერი
                    y = writeTableHeader(cs, TOP);
                } else {
                    cs = er.cs;
                    y = er.y;
                }
                writeCellMixed(cs, X_NO, y, String.format("%03d", rowNumber++));
                writeCellMixed(cs, X_VEH, y, safe(w.getWagonNumber()));
                writeCellMixed(cs, X_PROD, y, safe(w.getProduct()));
                writeCellMixed(cs, X_TARE, y, safe(w.getTare()));
                writeCellMixed(cs, X_GROSS, y, safe(w.getWeight()));
                writeCellMixed(cs, X_NETT, y, safe(w.getNeto()));
                writeCellMixed(cs, X_SPEED, y, safe(w.getSpeed()));
                y -= LINE_H;
            }
            writeCellMixed(cs, X_NO, y, "TOTAL TRAIN WEIGHT");
            writeCellMixed(cs, X_VEH, y, "");
            writeCellMixed(cs, X_PROD, y, safe(""));
            writeCellMixed(cs, X_TARE, y, safeOrXXXX(train.getTare()));
            writeCellMixed(cs, X_GROSS, y, safe(train.getGross()));
            writeCellMixed(cs, X_NETT, y, safeOrXXXX(train.getNeto()));

            EnsureResult er1 = ensureSpace(doc, cs, y, 8 * LINE_H);
            cs = er1.cs;
            y = er1.y - LINE_H + 2;
            writeLineMixed(cs, LEFT, y--,
                    "--------------------------------------------------------------------------------");
            y -= LINE_H + 2;
            writeLineMixed(cs, LEFT, y--,
                    getLocalized("total_gross") + " : " + safe(train.getGross()) + " " + UnitService.WEIGHT_UNIT);
            y -= LINE_H + 2;
            writeLineMixed(cs, LEFT, y--,
                    getLocalized("total_tare") + " : " + safeOrXXXX(train.getTare()) + " " + UnitService.WEIGHT_UNIT);
            y -= LINE_H + 2;
            writeLineMixed(cs, LEFT, y--,
                    getLocalized("total_net") + " : " + safeOrXXXX(train.getNeto()) + " " + UnitService.WEIGHT_UNIT);
            y -= LINE_H + 2;
            writeLineMixed(cs, LEFT, y--, getLocalized("direction") + " : " + getLocalized(train.getDirection()));
            y -= LINE_H + 2;
            writeLineMixed(cs, LEFT, y--,
                    getLocalized("max_speed") + " : " + safe(train.getMaxSpeed()) + " " + UnitService.SPEED_UNIT);
            y -= LINE_H + 2;
            writeLineMixed(cs, LEFT, y,
                    getLocalized("min_speed") + " : " + safe(train.getMinSpeed()) + " " + UnitService.SPEED_UNIT);

            if (!train.isNormalSpeed() || !train.isNormalWeight()) {
                y -= LINE_H * 2;
                writeLineMixed(cs, LEFT, y, getLocalized("validation.not_commercial") + " !!!");
            }

            cs.close();

            PDDocumentInformation info = doc.getDocumentInformation();
            info.setCreator(connId);
            info.setCreationDate(java.util.Calendar.getInstance());

            AccessPermission ap = new AccessPermission();
            ap.setCanModify(false);
            StandardProtectionPolicy spp = new StandardProtectionPolicy("MINITELSY", "", ap);
            spp.setEncryptionKeyLength(128);
            doc.protect(spp);

            FileUtils.cleanDirectory(RepoInit.PDF_REPOSITOR_FULL);
            File report = new File(RepoInit.PDF_REPOSITOR_FULL, train.getId() + ".pdf");
            doc.save(report);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createPdf(TrainMod train) {
        if (train == null || train.getWagons() == null || train.getWagons().isEmpty())
            return;
        String connId = train.getConId();
        try (PDDocument doc = new PDDocument()) {
            fontManager = new FontManager(doc);
            currentPageNumber = 1;
            PDPage page = addPage(doc);
            PDPageContentStream cs = new PDPageContentStream(doc, page);

            drawPageNumber(cs, page); // პირველი გვერდის ნომერი

            // სათაური და ლოგო მოშორებულია

            float y = TOP - 40; // დავიწყოთ ოდნავ ქვემოთ

            y = writeMainInfo(cs, y, train);
            y = writeTableHeader(cs, y);

            List<WagonMod> wagons = train.getWagons();
            wagons.sort(Comparator.comparingInt(WagonMod::getRowNum));
            int rowNumber = 1;

            for (WagonMod w : wagons) {
                EnsureResult er = ensureSpace(doc, cs, y, LINE_H);
                if (er.newPageCreated) {
                    page = er.newPage;
                    cs.close();
                    cs = new PDPageContentStream(doc, page);
                    currentPageNumber++;
                    drawPageNumber(cs, page); // ახალი გვერდის ნომერი
                    y = writeTableHeader(cs, TOP);
                } else {
                    cs = er.cs;
                    y = er.y;
                }
                writeCellMixed(cs, X_NO, y, String.format("%03d", rowNumber++));
                writeCellMixed(cs, X_VEH, y, safe(w.getWagonNumber()));
                writeCellMixed(cs, X_PROD, y, safe(w.getProduct()));
                writeCellMixed(cs, X_TARE, y, safe(w.getTare()));
                writeCellMixed(cs, X_GROSS, y, safe(w.getWeight()));
                writeCellMixed(cs, X_NETT, y, safe(w.getNeto()));
                writeCellMixed(cs, X_SPEED, y, safe(w.getSpeed()));
                y -= LINE_H;
            }

            EnsureResult er1 = ensureSpace(doc, cs, y, 8 * LINE_H);
            cs = er1.cs;
            y = er1.y - LINE_H + 2;
            writeLineMixed(cs, LEFT, y--,
                    "-------------------------------------------------------------------------------- ");
            y -= LINE_H + 2;
            writeLineMixed(cs, LEFT, y--,
                    getLocalized("total_gross") + " : " + safe(train.getGross()) + " " + UnitService.WEIGHT_UNIT);
            y -= LINE_H + 2;
            writeLineMixed(cs, LEFT, y--,
                    getLocalized("total_tare") + " : " + safeOrXXXX(train.getTare()) + " " + UnitService.WEIGHT_UNIT);
            y -= LINE_H + 2;
            writeLineMixed(cs, LEFT, y--,
                    getLocalized("total_net") + " : " + safeOrXXXX(train.getNeto()) + " " + UnitService.WEIGHT_UNIT);
            y -= LINE_H + 2;
            writeLineMixed(cs, LEFT, y--, getLocalized("direction") + " : " + getLocalized(train.getDirection()));
            y -= LINE_H + 2;
            writeLineMixed(cs, LEFT, y--,
                    getLocalized("max_speed") + " : " + safe(train.getMaxSpeed()) + " " + UnitService.SPEED_UNIT);
            y -= LINE_H + 2;
            writeLineMixed(cs, LEFT, y,
                    getLocalized("min_speed") + " : " + safe(train.getMinSpeed()) + " " + UnitService.SPEED_UNIT);

            if (!train.isNormalSpeed() || !train.isNormalWeight()) {
                y -= LINE_H * 2;
                writeLineMixed(cs, LEFT, y, getLocalized("validation.not_commercial") + " !!!");
            }

            cs.close();

            PDDocumentInformation info = doc.getDocumentInformation();
            info.setCreator(connId);
            info.setCreationDate(java.util.Calendar.getInstance());

            AccessPermission ap = new AccessPermission();
            ap.setCanModify(false);
            StandardProtectionPolicy spp = new StandardProtectionPolicy("MINITELSY", "", ap);
            spp.setEncryptionKeyLength(128);
            doc.protect(spp);

            File report = getLastReportFile(train.getScaleIndex());
            if (report != null) {
                doc.save(report);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== დანარჩენი მეთოდები უცვლელია ====================

    private String getLocalized(String key) {
        if (key == null || key.isBlank())
            return "";
        String docLang = BasicService.DOCLANGUAGE;
        Locale pdfLocale = (docLang != null && !docLang.isBlank())
                ? Locale.forLanguageTag(docLang)
                : LocaleContextHolder.getLocale();
        try {
            return ResourceBundle.getBundle("messages", pdfLocale).getString(key);
        } catch (MissingResourceException e) {
            return ResourceBundle.getBundle("messages", Locale.ENGLISH).getString(key);
        }
    }

    private void drawPageNumber(PDPageContentStream cs, PDPage page) throws IOException {
        float pageWidth = page.getMediaBox().getWidth();
        float x = pageWidth - 80;
        float y = 30;

        String pageText = "Page " + currentPageNumber;

        cs.beginText();
        cs.setFont(fontManager.getLatinFont(), 10);
        cs.newLineAtOffset(x, y);
        cs.showText(pageText);
        cs.endText();
    }

    private PDPage addPage(PDDocument doc) {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        return page;
    }

    private float writeMainInfo(PDPageContentStream cs, float y, TrainMod train) throws IOException {
        writeLineMixed(cs, LEFT, y, getLocalized("weighing_datetime") + " : " + safe(train.getWeighingStopDateTime()));
        y -= LINE_H + 2;
        writeLineMixed(cs, LEFT, y, getLocalized("instrument") + " : " + safe(train.getScaleName()));
        y -= LINE_H + 2;
        writeLineMixed(cs, LEFT, y, getLocalized("method") + " : " + getLocalized(train.getWeighingMethod()));
        y -= LINE_H + 2;
        writeLineMixed(cs, LEFT, y, getLocalized("process_id") + " : " + safe(String.format("%010d", train.getId())));
        y -= LINE_H + 2;
        writeLineMixed(cs, LEFT, y,
                "---------------------------------------------------------------------------------------------------------");
        y -= LINE_H + 2;
        return y;
    }

    private float writeTableHeader(PDPageContentStream cs, float y) throws IOException {
        writeCellMixed(cs, X_NO, y, "No");
        writeCellMixed(cs, X_VEH, y, "Vehicle");
        writeCellMixed(cs, X_PROD, y, "Product");
        writeCellMixed(cs, X_TARE, y, "Tare");
        writeCellMixed(cs, X_GROSS, y, "Gross");
        writeCellMixed(cs, X_NETT, y, "Net");
        writeCellMixed(cs, X_SPEED, y, "Speed");
        return y - LINE_H + 1;
    }

    private EnsureResult ensureSpace(PDDocument doc, PDPageContentStream cs, float y, float neededSpace)
            throws IOException {
        float safeBottom = BOTTOM_MARGIN + 25; // დამატებითი ბუფერი

        if (y - neededSpace <= safeBottom) {
            try {
                cs.close();
            } catch (Exception ignored) {
            }
            PDPage newPage = addPage(doc);
            PDPageContentStream newCs = new PDPageContentStream(doc, newPage);
            return new EnsureResult(newCs, TOP, true, newPage);
        }
        return new EnsureResult(cs, y, false, null);
    }

    private static class EnsureResult {
        final PDPageContentStream cs;
        final float y;
        final boolean newPageCreated;
        final PDPage newPage;

        EnsureResult(PDPageContentStream cs, float y, boolean newPageCreated, PDPage newPage) {
            this.cs = cs;
            this.y = y;
            this.newPageCreated = newPageCreated;
            this.newPage = newPage;
        }
    }

    private void writeCellMixed(PDPageContentStream cs, float x, float y, String text) throws IOException {
        if (text == null || text.isEmpty())
            text = "    ";
        cs.beginText();
        cs.newLineAtOffset(x, y);
        writeTextMixed(cs, text, 10);
        cs.endText();
    }

    private void writeLineMixed(PDPageContentStream cs, float x, float y, String text) throws IOException {
        if (text == null || text.isEmpty())
            text = "    ";
        cs.beginText();
        cs.newLineAtOffset(x, y);
        writeTextMixed(cs, text, 12);
        cs.endText();
    }

    private void writeTextMixed(PDPageContentStream cs, String text, float fontSize) throws IOException {
        if (text == null || text.isEmpty())
            return;
        StringBuilder currentRun = new StringBuilder();
        PDType0Font currentFont = null;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            PDType0Font neededFont = fontManager.getFontForChar(c);
            if (neededFont == null) {
                flushRun(cs, currentRun, currentFont, fontSize);
                currentRun.setLength(0);
                cs.setFont(fontManager.getLatinFont(), fontSize);
                cs.showText(fontManager.getFallbackChar());
                currentFont = null;
                continue;
            }
            if (currentFont == null || currentFont != neededFont) {
                flushRun(cs, currentRun, currentFont, fontSize);
                currentRun.setLength(0);
                currentFont = neededFont;
            }
            currentRun.append(c);
        }
        flushRun(cs, currentRun, currentFont, fontSize);
    }

    private void flushRun(PDPageContentStream cs, StringBuilder run, PDType0Font font, float fontSize)
            throws IOException {
        if (run.length() > 0 && font != null) {
            cs.setFont(font, fontSize);
            cs.showText(run.toString());
        }
    }

    private String safe(Object o) {
        return o == null ? "    " : o.toString();
    }

    private String safeOrXXXX(BigDecimal bd) {
        return bd != null ? bd.toPlainString() : "    ";
    }

    private File getLastReportFile(int connId) {
        return switch (connId) {
            case 0 -> new File(SAVE_REPO_LAST_0, "report0.pdf");
            case 1 -> new File(SAVE_REPO_LAST_1, "report1.pdf");
            case 2 -> new File(SAVE_REPO_LAST_2, "report2.pdf");
            case 3 -> new File(SAVE_REPO_LAST_3, "report3.pdf");
            case 4 -> new File(SAVE_REPO_LAST_4, "report4.pdf");
            case 5 -> new File(SAVE_REPO_LAST_5, "report5.pdf");
            case 6 -> new File(SAVE_REPO_LAST_6, "report6.pdf");
            case 7 -> new File(SAVE_REPO_LAST_7, "report7.pdf");
            case 8 -> new File(SAVE_REPO_LAST_8, "report8.pdf");
            case 9 -> new File(SAVE_REPO_LAST_9, "report9.pdf");
            default -> null;
        };
    }
}