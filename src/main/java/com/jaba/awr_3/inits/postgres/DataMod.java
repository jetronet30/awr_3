package com.jaba.awr_3.inits.postgres;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataMod {
    private String dataUser;
    private String dataPassword;
    private int dataPort;
    private String dataName;
    private String dataHost;
    private String dataUrl;
    private String resDataUser;
    private String resDataPassword;
    private int resDataPort;
    private String resDataName;
    private String resDataHost;
    private String resDataUrl;

}

