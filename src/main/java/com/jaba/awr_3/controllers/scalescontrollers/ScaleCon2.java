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
import com.jaba.awr_3.core.process.ProcesCom2;
import com.jaba.awr_3.core.prodata.services.TrainService;
import com.jaba.awr_3.core.units.UnitService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ScaleCon2 {
    private final TrainService trainService;
    private final ProcesCom2 procesCom2;
    private final EmitterServic emitter;
    private final ComService comService;

    @PostMapping("/scale2")
    public String postSacale2(Model m) {
        m.addAttribute("cam2Enabled", false);
        m.addAttribute("magonNumLeght_2", UnitService.W_NUM_LEN);
        m.addAttribute("conId_2", comService.getPortByIndex(2).getComName());
        return "proces/scale2";
    }

    @PostMapping("/startWeighing_2")
    public String startWeighing2(Model m) {
        m.addAttribute("cam2Enabled", false);
        m.addAttribute("magonNumLeght_2", UnitService.W_NUM_LEN);
        m.addAttribute("conId_2", comService.getPortByIndex(2).getComName());
        if (!trainService.isWorkInProgress(comService.getPortByIndex(2).getComName())) {
            procesCom2.sendDataTSR4000(GlobalRight.getSequenceIdHex_2() + "CSTART7C34" + GlobalRight.getSuffixHex_2());
        }
        return "proces/scale2";
    }

    @PostMapping("/doneWeighing_2")
    public String doneWeighing2(Model m) {
        m.addAttribute("cam2Enabled", false);
        m.addAttribute("magonNumLeght_2", UnitService.W_NUM_LEN);
        m.addAttribute("conId_2", comService.getPortByIndex(2).getComName());
        trainService.closeTrain(comService.getPortByIndex(2).getComName());
        return "proces/scale2";
    }

    @PostMapping("/abortWeighing_2")
    public String abortWeighing2(Model m) {
        m.addAttribute("cam2Enabled", false);
        m.addAttribute("magonNumLeght_2", UnitService.W_NUM_LEN);
        m.addAttribute("conId_2", comService.getPortByIndex(2).getComName());
        trainService.deleteTrainByConId(comService.getPortByIndex(2).getComName());
        procesCom2.sendDataTSR4000(GlobalRight.getSequenceIdHex_2() + "CABORT933C" + GlobalRight.getSuffixHex_2());
        return "proces/scale2";
    }

    @PostMapping("/showweighingWagons2")
    public String showWagon2(Model m) {
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(comService.getPortByIndex(2).getComName()));
        return "proces/beans/opdata2";
    }

    @PostMapping("/updateAllWeighing_2")
    public String updateWagon2(Model m) {
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(comService.getPortByIndex(2).getComName()));
        return "proces/beans/opdata2";
    }

    @PostMapping("/addwagonWeighing_2")
    public String addWagon2(Model m,
            @RequestParam("wagonNumber") String wagonNumber,
            @RequestParam("product") String product,
            @RequestParam(value = "count", required = false, defaultValue = "0") int count) {
        trainService.addWagonToTrain(comService.getPortByIndex(2).getComName(), wagonNumber, product, count);
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(comService.getPortByIndex(2).getComName()));
        return "proces/beans/opdata2";
    }

    @PostMapping("/editWgon2")
    @ResponseBody
    public Map<String, Object> editWagon2(
            @RequestParam("connId") String connId,
            @RequestParam("id") Long id,
            @RequestParam("product") String product,
            @RequestParam("wagonNumber") String wagonNum) {

        return trainService.updateWagonToTrain(id, connId, wagonNum, product,
                comService.getPortByIndex(2).isRightToUpdateTare());
    }

    @GetMapping(value = "/sendscale2", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getSendScale2() {
        return emitter.addEmitter();
    }

}
