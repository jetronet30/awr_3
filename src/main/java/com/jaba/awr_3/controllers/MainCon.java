package com.jaba.awr_3.controllers;



import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.jaba.awr_3.core.prodata.services.TrainService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MainCon {
    private final TrainService trainService;

    @GetMapping("/")
    public String getMain(Model m) {
        trainService.addWagonToTrain("ttsu","", "ProductA", 4);
        return "main";
    }
}
