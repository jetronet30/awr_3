package com.jaba.awr_3.core.connectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TcpMod {
    private String scaleName;
    private String tcpName;
    private String tcpNik;
    private String instrument;
    private String ipAddress;
    private int port;
    private boolean isActive;
    private boolean automatic;
    private boolean rightToUpdateTare;

}
