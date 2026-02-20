package com.jaba.awr_3.controllers.scalescontrollers;

import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import com.jaba.awr_3.core.process.ProcesCom1;
import com.jaba.awr_3.core.prodata.services.TrainService;
import com.jaba.awr_3.core.units.UnitService;
import com.jaba.awr_3.inits.repo.RepoInit;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ScaleCon1 {
    private final TrainService trainService;
    private final ProcesCom1 procesCom1;
    private final EmitterServic emitter;
    private final ComService comService;

    @PostMapping("/scale1")
    public String postSacale1(Model m) {
        m.addAttribute("cam1Enabled", false);
        m.addAttribute("magonNumLeght_1", UnitService.W_NUM_LEN);
        m.addAttribute("conId_1", comService.getPortByIndex(1).getComName());
        return "proces/scale1";
    }

    @PostMapping("/startWeighing_1")
    public String startWeighing1(Model m) {
        m.addAttribute("cam1Enabled", false);
        m.addAttribute("magonNumLeght_1", UnitService.W_NUM_LEN);
        m.addAttribute("conId_1", comService.getPortByIndex(1).getComName());
        if (!trainService.isWorkInProgress(comService.getPortByIndex(1).getComName())) {
            procesCom1.sendDataTSR4000(GlobalRight.getSequenceIdHex_1() + "CSTART7C34" + GlobalRight.getSuffixHex_1());
        }
        return "proces/scale1";
    }

    @PostMapping("/doneWeighing_1")
    public String doneWeighing1(Model m) {
        m.addAttribute("cam1Enabled", false);
        m.addAttribute("magonNumLeght_1", UnitService.W_NUM_LEN);
        m.addAttribute("conId_1", comService.getPortByIndex(1).getComName());
        trainService.closeTrain(comService.getPortByIndex(1).getComName());
        return "proces/scale1";
    }

    @PostMapping("/abortWeighing_1")
    public String abortWeighing1(Model m) {
        m.addAttribute("cam1Enabled", false);
        m.addAttribute("magonNumLeght_1", UnitService.W_NUM_LEN);
        m.addAttribute("conId_1", comService.getPortByIndex(1).getComName());
        trainService.deleteTrainByConId(comService.getPortByIndex(1).getComName());
        procesCom1.sendDataTSR4000(GlobalRight.getSequenceIdHex_1() + "CABORT933C" + GlobalRight.getSuffixHex_1());
        return "proces/scale1";
    }

    @PostMapping("/showweighingWagons1")
    public String showWagon1(Model m) {
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(comService.getPortByIndex(1).getComName()));
        return "proces/beans/opdata1";
    }

    @PostMapping("/updateAllWeighing_1")
    public String updateWagon1(Model m) {
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(comService.getPortByIndex(1).getComName()));
        return "proces/beans/opdata1";
    }

    @PostMapping("/addwagonWeighing_1")
    public String addWagon1(Model m,
            @RequestParam("wagonNumber") String wagonNumber,
            @RequestParam("product") String product,
            @RequestParam(value = "count", required = false, defaultValue = "0") int count) {
        trainService.addWagonToTrain(comService.getPortByIndex(1).getComName(), wagonNumber, product, count);
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(comService.getPortByIndex(1).getComName()));
        return "proces/beans/opdata1";
    }

    @PostMapping("/editWgon1")
    @ResponseBody
    public Map<String, Object> editWagon1(
            @RequestParam("connId") String connId,
            @RequestParam("id") Long id,
            @RequestParam("product") String product,
            @RequestParam("wagonNumber") String wagonNum) {

        return trainService.updateWagonToTrain(id, connId, wagonNum, product,
                comService.getPortByIndex(1).isRightToUpdateTare());
    }

    @GetMapping("/pdf1")
    public ResponseEntity<Resource> getPdf1() {
        FileSystemResource file = new FileSystemResource(RepoInit.PDF_REPOSITOR_LAST_1 + "/report1.pdf");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(file);
    }

    

    @GetMapping(value = "/sendscale1", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getSendScale1() {
        return emitter.addEmitter();
    }

}
