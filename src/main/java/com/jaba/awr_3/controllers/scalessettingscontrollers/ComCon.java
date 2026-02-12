package com.jaba.awr_3.controllers.scalessettingscontrollers;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.jaba.awr_3.core.connectors.ComService;
import com.jaba.awr_3.core.process.ProcesCom0;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ComCon {
    private final ComService comService;
    private final ProcesCom0 procesCom1;

    @PostMapping("/comport")
    public String postComPort(Model m) {
        m.addAttribute("coms", comService.listComPorts());
        procesCom1.stopProcess();
        return "scalessettings/comport";
    }

    @PostMapping("/setcomsettings")
    @ResponseBody
    public Map<String, Object> setComSettings(
            @RequestParam("comPortName") String name,
            @RequestParam("scaleName") String sacaleName,
            @RequestParam("activ") boolean activ,
            @RequestParam("automatic") boolean automatic,
            @RequestParam("instrument") String instrument,
            @RequestParam("baudRate") int baudRate,
            @RequestParam("parity") int parity,
            @RequestParam("dataBits") int dataBits,
            @RequestParam("stopBit") int stopBit,
            @RequestParam("rightToUpdate") boolean rightToUpdate) {
                procesCom1.startProcess();
        return comService.updateComPort(name,sacaleName,activ,automatic,instrument,parity, baudRate, dataBits, stopBit,rightToUpdate);
    }

}
