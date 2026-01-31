package com.jaba.awr_3.controllers.settingscontrollers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class SerCon {

    @PostMapping("/server")
    public String postServer(){
        return "settings/server";
    }
}
