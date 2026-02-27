package com.jaba.awr_3.controllers.settingscontrollers;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.jaba.awr_3.servermanager.ServerManager;
import com.jaba.awr_3.seversettings.owner.OwnerService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class OwnerCon {
    private final OwnerService oService;

    @PostMapping("/owner")
    public String postOwner(Model m){
        m.addAttribute("owner", oService.getOwner());
        return "settings/owner";
    }

    @PostMapping("/setOwnerSettings")
    @ResponseBody
    public Map<String,Object> setOwner(@RequestParam("ownerName")String name,
                                       @RequestParam("ownerEmail")String email,
                                       @RequestParam("ownerAddress")String address,
                                       @RequestParam("ownerSerial")String serial,
                                       @RequestParam("ownerLicenzi")String licenzi){
        return oService.updateOwner(name,email,address,serial,licenzi);
    }

    @PostMapping("/logoupload")
    public String uploadOwnerLogo(@RequestParam("logo") MultipartFile file, Model m) {
        oService.uploadOwnerLogo(file);
        m.addAttribute("owner", oService.getOwner());
        System.out.println(file.getContentType());
        return "settings/owner";
    }

    @PostMapping("/owner-save-and-reboot")
    public String setDataAndReboot(Model m) {
        ServerManager.reboot();
        return "settings/reboot";
    }

}
