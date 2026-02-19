package com.jaba.awr_3.controllers.scalescontrollers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.jaba.awr_3.core.prodata.services.TrainService;


import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ArchivCon {
    private final TrainService trainService;

    @PostMapping("/archive")
    public String postArchive(Model m) {

        return "proces/archive/archive";
    }

    @PostMapping("/showTrains")
    public String postTrains(Model m) {
        m.addAttribute("trains", trainService.getAllTrainsSortedByDateCreation());
        return "proces/archive/archivbean";
    }

    @GetMapping("/trainArchiv/{id}")
    public String getTrainArchiv(@PathVariable Long id, Model m) {
        System.out.println("Received request for train archive with ID: " + id);
        return "proces/archive/trainpage"; 
    }

    

}
