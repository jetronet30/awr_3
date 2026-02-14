package com.jaba.awr_3.controllers.scalescontrollers;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.jaba.awr_3.controllers.emitter.EmitterServic;
import com.jaba.awr_3.core.connectors.ComService;
import com.jaba.awr_3.core.globalvar.GlobalRight;
import com.jaba.awr_3.core.process.ProcesCom0;
import com.jaba.awr_3.core.prodata.services.TrainService;
import com.jaba.awr_3.core.units.UnitService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ScaleCon0 {
    private final TrainService trainService;
    private final ProcesCom0 procesCom0;
    private final EmitterServic emitter;
    private final ComService comService;

    @PostMapping("/scale0")
    public String postSacale0(Model m) {
        m.addAttribute("cam0Enabled", false);
        m.addAttribute("magonNumLeght_0", UnitService.W_NUM_LEN);
        m.addAttribute("conId_0", comService.getPortByIndex(0).getComName());
        return "proces/scale0";
    }

    @PostMapping("/startWeighing_0")
    public String startWeighing0(Model m) {
        m.addAttribute("cam0Enabled", false);
        m.addAttribute("magonNumLeght_0", UnitService.W_NUM_LEN);
        m.addAttribute("conId_0", comService.getPortByIndex(0).getComName());
        if(!trainService.isWorkInProgress(comService.getPortByIndex(0).getComName())) {
            procesCom0.sendDataTSR4000(GlobalRight.getSequenceIdHex_0() + "CSTART7C34" + GlobalRight.getSuffixHex_0());
        }
        return "proces/scale0";
    }

    @PostMapping("/abortWeighing_0")
    public String abortWeighing0(Model m) {
        m.addAttribute("cam0Enabled", true);
        m.addAttribute("magonNumLeght_0", UnitService.W_NUM_LEN);
        m.addAttribute("conId_0", comService.getPortByIndex(0).getComName());
        procesCom0.sendDataTSR4000(GlobalRight.getSequenceIdHex_1() + "CABORT933C" + GlobalRight.getSuffixHex_1());
        return "proces/scale0";
    }

    @PostMapping("/showweighingWagons0")
    public String showWagon0(Model m) {
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(comService.getPortByIndex(0).getComName()));
        return "proces/beans/opdata0";
    }

    @PostMapping("/updateAllWeighing_0")
    public String updateWagon0(Model m) {
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(comService.getPortByIndex(0).getComName()));
        return "proces/beans/opdata0";
    }

    @PostMapping("/addwagonWeighing_0")
    public String addWagon0(Model m,
            @RequestParam("wagonNumber") String wagonNumber,
            @RequestParam("product") String product,
            @RequestParam(value = "count", required = false, defaultValue = "0") int count) {
        trainService.addWagonToTrain(comService.getPortByIndex(0).getComName(), wagonNumber, product, count);
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(comService.getPortByIndex(0).getComName()));
        return "proces/beans/opdata0";
    }

    @PostMapping("/editWgon0")
    @ResponseBody
    public Map<String, Object> editWagon0(
            @RequestParam("connId") String connId,
            @RequestParam("id") Long id,
            @RequestParam("product") String product,
            @RequestParam("wagonNumber") String wagonNum) {

        return trainService.updateWagonToTrain(id, connId, wagonNum, product, true);
    }

    @GetMapping(value = "/sendscale0", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getSendScale0() {
        return emitter.addEmitter();
    }

}
