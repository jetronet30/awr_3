package com.jaba.awr_3.controllers.scalessettingscontrollers;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.jaba.awr_3.core.connectors.TcpService;
import com.jaba.awr_3.servermanager.ServerManager;
import com.jaba.awr_3.seversettings.network.NetService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class TcpCon {
    private final TcpService tcpService;
    private final NetService nService;

    @PostMapping("/tcp")
    public String postTcp(Model m) {
        m.addAttribute("tcps", tcpService.listTcps());
        m.addAttribute("ips", nService.getIpAddress());
        return "scalessettings/tcp";
    }

    @PostMapping("/tcp-setting-reboot")
    public String setTcpndReboot(Model m) {
        ServerManager.reboot();
        return "settings/reboot";
    }

    @PostMapping("/updateTcpsettings")
    @ResponseBody
    public Map<String, Object> updateTcpSettings(
            @RequestParam("tcpName") String tcpName,
            @RequestParam("tcpscaleName") String scaleName,
            @RequestParam("isActive") boolean isActive,
            @RequestParam("instrument") String instrument,
            @RequestParam("ipAddress") String ipAddress,
            @RequestParam("tcpPort") int tcpPort,
            @RequestParam("rightToUpdate") boolean rightToUpdate,
            @RequestParam("automatic") boolean automatic) {
        return tcpService.updateTcpSettingsByName(tcpName, scaleName, tcpName, instrument, ipAddress, tcpPort, isActive, automatic, rightToUpdate);
    }
}
