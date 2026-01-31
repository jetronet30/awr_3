package com.jaba.awr_3.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MainCon {

    @GetMapping("/")
    public String getMain(Model m) {
        return "main";
    }
}
