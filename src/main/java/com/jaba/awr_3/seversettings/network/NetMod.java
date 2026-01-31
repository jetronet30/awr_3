package com.jaba.awr_3.seversettings.network;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetMod {
    private String inName;
    private String ip;
    private String gateWay;
    private String subnet;
    private String dns1;
    private String dns2;
    private String metric;
    private boolean isLink;
    private boolean isInternet;
}
