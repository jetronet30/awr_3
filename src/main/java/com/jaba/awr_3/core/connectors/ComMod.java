package com.jaba.awr_3.core.connectors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComMod {
    private String scaleName;
    private String comName;
    private String comNick;
    private String instrument;
    private int parity;
    private int baudRate;
    private int dataBits;
    private int stopBit;
    private boolean inUse;
    private boolean exists;
    private boolean isActive;
    private boolean automatic;
    private boolean rightToUpdateTare;
}
