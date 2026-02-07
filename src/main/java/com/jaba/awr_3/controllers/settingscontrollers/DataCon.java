package com.jaba.awr_3.controllers.settingscontrollers;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.jaba.awr_3.inits.postgres.DataService;
import com.jaba.awr_3.servermanager.ServerManager;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class DataCon {
    private final DataService dService;

    @PostMapping("/data")
    public String postData(Model m) {
        m.addAttribute("data", dService.getDataSettings());
        return "settings/data";
    }

    @PostMapping("/setDataSettings")
    @ResponseBody
    public Map<String, Object> setBasicSettings(@RequestParam("dataUser") String dataUser,
            @RequestParam("dataName") String dataName,
            @RequestParam("dataPassword") String dataPassword,
            @RequestParam("dataPasswordre") String dataPasswordre,
            @RequestParam("dataHost") String dataHost,
            @RequestParam("dataPort") int port) {
        return dService.updateDataSettings(dataName, dataUser, dataPassword, dataPasswordre, dataHost, port);
    }

    @PostMapping("/data-save-and-reboot")
    public String setDataAndReboot(Model m) {
        ServerManager.reboot();
        return "settings/reboot";
    }
}
