package com.jaba.awr_3.core.parsers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.jaba.awr_3.controllers.emitter.EmitterServic;
import com.jaba.awr_3.core.prodata.services.TrainService;
import com.jaba.awr_3.core.units.UnitService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class Tsr4000Parser {
    private final TrainService trainService;
    private final EmitterServic emitterServic;
    private static final Logger LOGGER = LoggerFactory.getLogger(Tsr4000Parser.class);

    public void parseSectors(String text, String scaleName, String conId, int scaleIndex, boolean automatic,
            boolean rightToUpdateTare) {
        if (text == null || text.length() < 1) {
            LOGGER.warn("Data too short: {}", text);
            return;
        }
        try {
            String first3 = text.substring(0, Math.min(3, text.length())).toUpperCase();
            if (first3.contains("G")) {
                if (text.length() < 45) {
                    LOGGER.warn("G-type data incomplete: {}", text);
                    return;
                }
                // String identficator = text.substring(0, 3);
                String processId = text.substring(3, 8);
                String fuulWeight = text.substring(8, 16);
                String weghtingDate = text.substring(16, 30);
                String maxSpeed = text.substring(30, 36);
                String minSpeed = text.substring(36, 42);
                String countsector7 = text.substring(42, 45);
                // String lastsector8 = text.substring(45);
                trainService.updateTrain(conId, processId, getWeight(fuulWeight), getDate(weghtingDate),
                        getSpeed(maxSpeed), getSpeed(minSpeed), getRowNum(countsector7));
                
                emitterServic.sendToScale(conId, "update-data-container");
            } else if (first3.contains("V")) {
                if (text.length() < 45) {
                    LOGGER.warn("V-type data incomplete: {}", text);
                    return;
                }
                // String identficator = text.substring(0, 3);
                String prosesId = text.substring(3, 8);
                String rowNum = text.substring(8, 11);
                String weight = text.substring(11, 17);
                String wDate = text.substring(17, 31);
                String[] speedAxle = text.split("\\s+");
                String speedAndAxle = speedAxle[speedAxle.length - 2];
                System.out.println(
                        getSpeed(speedAndAxle.substring(0, 6)) + "" + "Axle: " + getAxle(speedAndAxle.substring(6)));
                /*
                 * System.out.println(getWeight(weight));`
                 * System.out.println(getDate(wDate));
                 * System.out.println(prosesId);
                 * System.out.println(text.length());
                 * System.out.println("row NUMBER:" + getRowNum(rowNum));
                 * System.out.println(automatic);
                 */
                trainService.addWagonToTrain(conId, prosesId, getRowNum(rowNum), getWeight(weight), getDate(wDate),
                        getSpeed(speedAndAxle.substring(0, 6)), getAxle(speedAndAxle.substring(6)), rightToUpdateTare);

                emitterServic.sendToScale(conId, getWeight(weight));
                if (automatic) {
                    emitterServic.sendToScale(conId, "update-data-container");
                }
            } else if (text.toLowerCase().contains("cstart")) {
                trainService.closeTrainAndOpenNewTrain(conId, scaleName, scaleIndex);
                
                emitterServic.sendToScale(conId, "update-data-container");
                emitterServic.sendToScale(conId, "update-data-works-start");
            } else if (text.contains("Trn_Dir:")) {
                String upper = text.toUpperCase();
                if (upper.contains(" IN ") || upper.contains(":IN ") || upper.contains("(IN")) {
                    trainService.updateTrainAndWagons(conId, "IN");
                    emitterServic.sendToScale(conId, "update-data-works-stop");
                    
                    if (automatic) {
                        emitterServic.sendToScale(conId, "update-data-container");
                    }
                } else if (upper.contains(" OUT ") || upper.contains(":OUT ") || upper.contains("(OUT")) {
                    trainService.updateTrainAndWagons(conId, "OUT");
                    emitterServic.sendToScale(conId, "update-data-works-stop");
                    
                    if (automatic) {
                        emitterServic.sendToScale(conId, "update-data-container");
                    }
                }
            } else if (text.endsWith("6B")) {

            } else {
                LOGGER.warn("Unknown identifier: {}", first3);
            }
        } catch (StringIndexOutOfBoundsException e) {
            LOGGER.error("Error splitting sectors, incomplete data: {}", text, e);
        } catch (Exception e) {
            LOGGER.error("Error processing sectors: {}", text, e);
        }
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

    private String getDate(String date) {
        if (date == null || date.length() != 14) {
            return date;
        }
        String year = date.substring(0, 4);
        String month = date.substring(4, 6);
        String day = date.substring(6, 8);
        String hour = date.substring(8, 10);
        String minute = date.substring(10, 12);
        String second = date.substring(12, 14);
        return String.format("%s/%s/%s %s:%s:%s", year, month, day, hour, minute, second);
    }

    private String getSpeed(String speed) {
        if (speed == null || speed.isEmpty()) {
            return "0,0" + UnitService.SPEED_UNIT;
        }
        speed = speed.replaceFirst("^0+", "");
        if (speed.isEmpty()) {
            return "0,0" + UnitService.SPEED_UNIT;
        }
        double result = 0;
        try {
            result = Double.parseDouble(speed) / 100.0;
        } catch (NumberFormatException e) {
            result = 0;
        }
        return String.valueOf(result) + UnitService.SPEED_UNIT;
    }

    private int getRowNum(String row) {
        if (row == null || row.isEmpty()) {
            return 0;
        }
        row = row.replaceFirst("^0+", "");
        if (row.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(row);
    }

    private int getAxle(String axle) {
        if (axle == null || axle.isEmpty()) {
            return 0;
        }
        axle = axle.replaceFirst("^0+", "");
        if (axle.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(axle);
    }

}
