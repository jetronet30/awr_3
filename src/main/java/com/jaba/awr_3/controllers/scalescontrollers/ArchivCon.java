package com.jaba.awr_3.controllers.scalescontrollers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;

import com.jaba.awr_3.core.prodata.services.TrainService;


import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ArchivCon {
    private final TrainService trainService;

    @PostMapping("/archive")
    public String postArchive(Model m) {
        m.addAttribute("trains", trainService.getAllTrainsSortedByDateCreation());
        return "proces/archive";
    }

    

}
