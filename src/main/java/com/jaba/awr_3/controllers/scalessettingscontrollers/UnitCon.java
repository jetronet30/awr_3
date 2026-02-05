package com.jaba.awr_3.controllers.scalessettingscontrollers;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.jaba.awr_3.core.units.UnitService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class UnitCon {
    private final UnitService unitService;

    @PostMapping("/units")
    public String postUnits(Model m) {
        m.addAttribute("units", unitService.getUnit());
        return "scalessettings/units";
    }

    @PostMapping("/setunits")
    @ResponseBody
    public Map<String, Object> setBasicSettings(@RequestParam("speedUnit") String speedUnit,
                                                 @RequestParam("weightUnit") String weightUnit,
                                                 @RequestParam("wagonLenUnit") int len,
                                                 @RequestParam("tareLimit")String tareLimit) {
        return unitService.updateUnits(speedUnit, weightUnit, len,tareLimit);
    }

}
