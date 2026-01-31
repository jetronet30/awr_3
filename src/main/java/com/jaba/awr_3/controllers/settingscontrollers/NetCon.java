package com.jaba.awr_3.controllers.settingscontrollers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.jaba.awr_3.servermanager.ServerManger;
import com.jaba.awr_3.seversettings.network.NetService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class NetCon {
    private final NetService netService;

    @PostMapping("/network")
    public String postNet(Model m) {
        m.addAttribute("nets", netService.getNet());
        return "settings/network";
    }

    @PostMapping("/netw-save-and-write")
    public String setNetworkAndWrite(Model m) {
        netService.writeNetYaml();
        ServerManger.restartNetwork();
        m.addAttribute("nets", netService.getNet());
        return "settings/network";
    }

    @PostMapping("/netw-save-and-reboot")
    public String setNetworkAndReboot(Model m) {
        ServerManger.reboot();
        return "settings/reboot";
    }

    @PostMapping("/testNetwork")
    @ResponseBody
    public Map<String, Boolean> testNetwork(@RequestParam("test") Boolean test) {
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", test);
        return response;
    }

    @PostMapping("/setLanSettings")
    @ResponseBody
    public Map<String, Object> setLanSettings(
                                             @RequestParam("inName") String inName,
                                             @RequestParam("ip") String ip,
                                             @RequestParam("gateway") String gateway,
                                             @RequestParam("subnet") String subnet,
                                             @RequestParam("dns1") String dns1,
                                             @RequestParam("dns2") String dns2,
                                             @RequestParam("metric") String metric) {                                    
        return netService.updateLanSetting(inName, ip, gateway, subnet, dns1, dns2, metric);
    }
}