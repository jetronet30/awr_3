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
import com.jaba.awr_3.core.process.ProcesCom3;
import com.jaba.awr_3.core.prodata.services.TrainService;
import com.jaba.awr_3.core.units.UnitService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ScaleCon3 {
    private final TrainService trainService;
    private final ProcesCom3 procesCom3;
    private final EmitterServic emitter;
    private final ComService comService;

    @PostMapping("/scale3")
    public String postSacale3(Model m) {
        m.addAttribute("cam3Enabled", false);
        m.addAttribute("magonNumLeght_3", UnitService.W_NUM_LEN);
        m.addAttribute("conId_3", comService.getPortByIndex(3).getComName());
        return "proces/scale3";
    }

    @PostMapping("/startWeighing_3")
    public String startWeighing3(Model m) {
        m.addAttribute("cam3Enabled", false);
        m.addAttribute("magonNumLeght_3", UnitService.W_NUM_LEN);
        m.addAttribute("conId_3", comService.getPortByIndex(3).getComName());
        if (!trainService.isWorkInProgress(comService.getPortByIndex(3).getComName())) {
            procesCom3.sendDataTSR4000(GlobalRight.getSequenceIdHex_3() + "CSTART7C34" + GlobalRight.getSuffixHex_3());
        }
        return "proces/scale3";
    }

    @PostMapping("/doneWeighing_3")
    public String doneWeighing3(Model m) {
        m.addAttribute("cam3Enabled", false);
        m.addAttribute("magonNumLeght_3", UnitService.W_NUM_LEN);
        m.addAttribute("conId_3", comService.getPortByIndex(3).getComName());
        trainService.closeTrain(comService.getPortByIndex(3).getComName());
        return "proces/scale3";
    }

    @PostMapping("/abortWeighing_3")
    public String abortWeighing3(Model m) {
        m.addAttribute("cam3Enabled", false);
        m.addAttribute("magonNumLeght_3", UnitService.W_NUM_LEN);
        m.addAttribute("conId_3", comService.getPortByIndex(3).getComName());
        trainService.deleteTrainByConId(comService.getPortByIndex(3).getComName());
        procesCom3.sendDataTSR4000(GlobalRight.getSequenceIdHex_3() + "CABORT933C" + GlobalRight.getSuffixHex_3());
        return "proces/scale3";
    }

    @PostMapping("/showweighingWagons3")
    public String showWagon3(Model m) {
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(comService.getPortByIndex(3).getComName()));
        return "proces/beans/opdata3";
    }

    @PostMapping("/updateAllWeighing_3")
    public String updateWagon3(Model m) {
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(comService.getPortByIndex(3).getComName()));
        return "proces/beans/opdata3";
    }

    @PostMapping("/addwagonWeighing_3")
    public String addWagon3(Model m,
            @RequestParam("wagonNumber") String wagonNumber,
            @RequestParam("product") String product,
            @RequestParam(value = "count", required = false, defaultValue = "0") int count) {
        trainService.addWagonToTrain(comService.getPortByIndex(3).getComName(), wagonNumber, product, count);
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(comService.getPortByIndex(3).getComName()));
        return "proces/beans/opdata3";
    }

    @PostMapping("/editWgon3")
    @ResponseBody
    public Map<String, Object> editWagon3(
            @RequestParam("connId") String connId,
            @RequestParam("id") Long id,
            @RequestParam("product") String product,
            @RequestParam("wagonNumber") String wagonNum) {

        return trainService.updateWagonToTrain(id, connId, wagonNum, product,
                comService.getPortByIndex(3).isRightToUpdateTare());
    }

    @GetMapping(value = "/sendscale3", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getSendScale3() {
        return emitter.addEmitter();
    }

}
