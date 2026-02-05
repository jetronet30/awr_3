package com.jaba.awr_3.controllers.scalessettingscontrollers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.jaba.awr_3.core.tare.MDBreader;
import com.jaba.awr_3.core.tare.WtareService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class TareCon {
    private final WtareService wtareService;
    private final MDBreader mdBreader;


    @PostMapping("/wagontares")
    public String postTares(Model m) {
        m.addAttribute("wTares", wtareService.getTaresSortedByNumber());
        return "scalessettings/wagontares";
    }

    @PostMapping("/addwagonTare")
    public String addtTares(Model m, @RequestParam("tare") String tare,
                                     @RequestParam("wagonNumber") String number) {
        wtareService.addOrUpdateWtare(tare, number);                               
        m.addAttribute("wTares", wtareService.getTaresSortedByNumber());
        return "scalessettings/wagontares";
    }

    @PostMapping("/uploaTareBaseMDB")
    public String uploadVideo(@RequestParam("tareBase") MultipartFile file, Model m) {
        mdBreader.uploadBackup(file);
        m.addAttribute("wTares", wtareService.getTaresSortedByNumber());
        return "scalessettings/wagontares";
    }

}
