package com.jaba.awr_3.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.jaba.awr_3.core.connectors.ComMod;
import com.jaba.awr_3.core.connectors.ComService;
import com.jaba.awr_3.core.connectors.TcpMod;
import com.jaba.awr_3.core.connectors.TcpService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MainCon {
    private final ComService comService;
    private final TcpService tcpService;

    @GetMapping("/")
    public String getMain(Model m) {
        ComMod chan0 = comService.getPortByIndex(0);
        ComMod chan1 = comService.getPortByIndex(1);
        ComMod chan2 = comService.getPortByIndex(2);
        ComMod chan3 = comService.getPortByIndex(3);
        ComMod chan4 = comService.getPortByIndex(4);
        
        TcpMod chan5 = tcpService.getTcpByIndex(5);
        TcpMod chan6 = tcpService.getTcpByIndex(6);
        TcpMod chan7 = tcpService.getTcpByIndex(7);
        TcpMod chan8 = tcpService.getTcpByIndex(8);
        TcpMod chan9 = tcpService.getTcpByIndex(9);

        // აქტიურობა (isActive)
        m.addAttribute("scale0Enabled", chan0 != null && chan0.isActive());
        m.addAttribute("scale1Enabled", chan1 != null && chan1.isActive());
        m.addAttribute("scale2Enabled", chan2 != null && chan2.isActive());
        m.addAttribute("scale3Enabled", chan3 != null && chan3.isActive());
        m.addAttribute("scale4Enabled", chan4 != null && chan4.isActive());
        m.addAttribute("scale5Enabled", chan5 != null && chan5.isActive());
        m.addAttribute("scale6Enabled", chan6 != null && chan6.isActive());
        m.addAttribute("scale7Enabled", chan7 != null && chan7.isActive());
        m.addAttribute("scale8Enabled", chan8 != null && chan8.isActive());
        m.addAttribute("scale9Enabled", chan9 != null && chan9.isActive());



        m.addAttribute("ScaleName0", chan0 != null ? chan0.getScaleName() : "");
        m.addAttribute("ScaleName1", chan1 != null ? chan1.getScaleName() : "");
        m.addAttribute("ScaleName2", chan2 != null ? chan2.getScaleName() : "");
        m.addAttribute("ScaleName3", chan3 != null ? chan3.getScaleName() : "");
        m.addAttribute("ScaleName4", chan4 != null ? chan4.getScaleName() : "");
        m.addAttribute("ScaleName5", chan5 != null ? chan5.getScaleName() : "");
        m.addAttribute("ScaleName6", chan6 != null ? chan6.getScaleName() : "");
        m.addAttribute("ScaleName7", chan7 != null ? chan7.getScaleName() : "");
        m.addAttribute("ScaleName8", chan8 != null ? chan8.getScaleName() : "");
        m.addAttribute("ScaleName9", chan9 != null ? chan9.getScaleName() : "");

        return "main";
    }
}
