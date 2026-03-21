package com.jaba.awr_3.core.numberdetection.ocr;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcrMod {
    private int index;
    private String rtspUrl_1;
    private String rtspUrl_2;
    private double minConfidence_1;
    private int minObJecWidth_1;
    private int minobJecHeight_1;
    private double minConfidence_2;
    private int minObJecWidth_2;
    private int minobJecHeight_2;
    private String hlsRepo;
    private String videoArChive;
    private String YoloModel;
    private String TROCModel;
    private int wagonNumberLength;
    private boolean activeDetection;
    private boolean activeStream;

}
