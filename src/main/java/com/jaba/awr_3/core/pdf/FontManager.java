package com.jaba.awr_3.core.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FontManager {
    private final Map<String, PDType0Font> fonts = new HashMap<>();
    private final PDDocument doc;
    private final String fallbackChar = "□";

    public FontManager(PDDocument doc) {
        this.doc = doc;
        loadFonts();
    }

    private void loadFonts() {
        Map<String, String> fontPaths = Map.of(
            "latin",    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "georgian", "/usr/share/fonts/truetype/noto/NotoSansGeorgian-Regular.ttf",
            "cyrillic", "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "azeri",    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "armenian", "/usr/share/fonts/truetype/noto/NotoSansArmenian-Regular.ttf",
            "kazakh",   "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "turkish",  "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "tukmen",   "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"

        );

        fontPaths.forEach((name, path) -> {
            File file = new File(path);
            if (file.exists()) {
                try {
                    fonts.put(name, PDType0Font.load(doc, file));
                } catch (Exception e) {
                    System.err.println("Failed to load font: " + name + " - " + path);
                }
            } else {
                System.err.println("Font not found: " + path);
            }
        });

        if (!fonts.containsKey("latin")) {
            throw new IllegalStateException("Latin font is required!");
        }
    }

    public PDType0Font getFontForChar(char c) {
        String script = getScript(c);
        return fonts.get(script);
    }

    public PDType0Font getLatinFont() {
        return fonts.get("latin");
    }

    public String getFallbackChar() {
        return fallbackChar;
    }

    private String getScript(char c) {
        int code = (int) c;

        // ქართული (მხედრული + მთავრული)
        if ((code >= 0x10A0 && code <= 0x10FF) || // Mkhedruli
            (code >= 0x1C90 && code <= 0x1CFF)) { // Mtavruli
            return "georgian";
        }

        // ლათინური, ციფრები, პუნქტუაცია
        if (code <= 0x02FF) {
            return "latin";
        }

        // სხვა სიმბოლოებისთვის — ვერ ამოვიცანით, ვაბრუნებთ null → გამოიყენება fallback
        return null;
    }
}