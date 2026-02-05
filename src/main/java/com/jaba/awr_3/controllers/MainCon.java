package com.jaba.awr_3.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.jaba.awr_3.core.sysutils.SysIdService;
import com.jaba.awr_3.servermanager.ServerManger;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MainCon {

    @GetMapping("/")
    public String getMain(Model m) {
        System.out.println(ServerManger.getSystemDateTime());
        System.out.println(SysIdService.getSysId());
        return "main";
    }
}
