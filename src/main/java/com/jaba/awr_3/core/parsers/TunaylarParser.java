package com.jaba.awr_3.core.parsers;

import org.springframework.stereotype.Service;

import com.jaba.awr_3.controllers.emitter.EmitterServic;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TunaylarParser {
    private final EmitterServic emitterServic;

    public void parseSectors(String text, String scaleName, String conId, int scaleIndex, boolean automatic,
            boolean rightToUpdateTare) {
        String[] lines = text.split(" ");
        emitterServic.sendToScale(conId, getWeight(lines[1]));
        System.out.println("Parsing Tunaylar data: " + lines[1]);

    }

    private String getWeight(String input) {
        if (input == null || input.isEmpty()) {
            return "0.0";
        }
        input = input.replaceFirst("^0+", "");
        if (input.endsWith("0") && input.length() > 0) {
            input = input.substring(0, input.length() - 1);
        }
        double result = 0;
        try {
            result = Double.parseDouble(input) / 100.0;
        } catch (NumberFormatException e) {
            result = 0;
        }
        return String.valueOf(result);
    }

}
