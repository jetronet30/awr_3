package com.jaba.awr_3.core.numberdetection.ocr;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcrMod {
    private int index;
    private String cam1Passwd;
    private String cam2Passwd;
    private String cam1Usr;
    private String cam2Usr;
    private int cam1Port;
    private int cam2Port;
    private String rtspUrl_1;
    private String rtspUrl_2;
    private double roiX1;
    private double roiY1;
    private double roiX2;
    private double roiY2;
    private boolean activeDetection;
    private boolean activeStream;

}
