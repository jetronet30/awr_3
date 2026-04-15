package com.jaba.awr_3.controllers.securitycontrollers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;

import com.jaba.awr_3.security.servvices.SecurityService;
import com.jaba.awr_3.servermanager.ServerManager;

import lombok.RequiredArgsConstructor;



@Controller
@RequiredArgsConstructor
public class SeCon {
    private final SecurityService sService;

    @PostMapping("/administrator")
    public String postAdminitrator(Model m) {
        m.addAttribute("users", sService.getAllS());
        return "settings/administrator";
    }

    @PostMapping("/administrator/editUser")
    public String postEditUser(Model m){
        m.addAttribute("users", sService.getAllS());
        return "settings/administrator";
    }


    @PostMapping("/administrator/reboot")
    public String saveAndRebootAdministrator() {
        ServerManager.reboot();
        return "settings/reboot";
    }
    

}
