package com.jaba.awr_3.controllers.scalescontrollers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.jaba.awr_3.core.archive.ArchiveService;


import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ArchivCon {
    private final ArchiveService archiveService;

    @PostMapping("/archive")
    public String postArchive(Model m) {
        m.addAttribute("scaleNames", archiveService.getScaleNames());

        return "proces/archive/archive";
    }

    @PostMapping("/showTrains")
    public String postTrains(Model m) {
        m.addAttribute("trains", archiveService.getLas100Trains());
        return "proces/archive/archivbean";
    }

    @GetMapping("/trainArchiv/{id}")
    public String getTrainArchiv(@PathVariable Long id, Model m) {
        System.out.println("Received request for train archive with ID: " + id);
        return "proces/archive/trainpage"; 
    }

    

}
