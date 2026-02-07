package com.jaba.awr_3.controllers.settingscontrollers;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.jaba.awr_3.servermanager.ServerManager;
import com.jaba.awr_3.seversettings.basic.BasicService;
import com.jaba.awr_3.seversettings.network.NetService;

import lombok.RequiredArgsConstructor;



@Controller
@RequiredArgsConstructor
public class BasicCon {
    private final BasicService bService;
    private final NetService nService;

    @PostMapping("/basic")
    public String postBasic(Model m) {
        m.addAttribute("basic", bService.getBasicSettings());
        m.addAttribute("ips", nService.getIpAddress());
        return "settings/basic";
    }

    @PostMapping("/setBasicSettings")
    @ResponseBody
    public Map<String, Object> setBasicSettings(@RequestParam("language") String language,
                                                 @RequestParam("timeZone") String timeZone,
                                                 @RequestParam("port") int port,
                                                 @RequestParam("ipAddress") String ip) {
        return bService.updateBasic(language, timeZone,port, ip);
    }

    @PostMapping("/basic-save-and-reboot")
    public String setNetworkAndReboot(Model m) {
        ServerManager.reboot();
        return "settings/reboot";
    }

}
