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
import com.jaba.awr_3.core.process.ProcesCom4;
import com.jaba.awr_3.core.prodata.services.TrainService;
import com.jaba.awr_3.core.units.UnitService;
import com.jaba.awr_3.inits.repo.RepoInit;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ScaleCon4 {
    private final TrainService trainService;
    private final ProcesCom4 procesCom4;
    private final EmitterServic emitter;
    private final ComService comService;

    @PostMapping("/scale4")
    public String postSacale4(Model m) {
        m.addAttribute("cam4Enabled", false);
        m.addAttribute("magonNumLeght_4", UnitService.W_NUM_LEN);
        m.addAttribute("conId_4", comService.getPortByIndex(4).getComName());
        return "proces/scale4";
    }

    @PostMapping("/startWeighing_4")
    public String startWeighing4(Model m) {
        m.addAttribute("cam4Enabled", false);
        m.addAttribute("magonNumLeght_4", UnitService.W_NUM_LEN);
        m.addAttribute("conId_4", comService.getPortByIndex(4).getComName());
        if (!trainService.isWorkInProgress(comService.getPortByIndex(4).getComName())) {
            procesCom4.sendDataTSR4000(GlobalRight.getSequenceIdHex_4() + "CSTART7C34" + GlobalRight.getSuffixHex_4());
        }
        return "proces/scale4";
    }

    @PostMapping("/doneWeighing_4")
    public String doneWeighing4(Model m) {
        m.addAttribute("cam4Enabled", false);
        m.addAttribute("magonNumLeght_4", UnitService.W_NUM_LEN);
        m.addAttribute("conId_4", comService.getPortByIndex(4).getComName());
        trainService.closeTrain(comService.getPortByIndex(4).getComName());
        return "proces/scale4";
    }

    @PostMapping("/abortWeighing_4")
    public String abortWeighing4(Model m) {
        m.addAttribute("cam4Enabled", false);
        m.addAttribute("magonNumLeght_4", UnitService.W_NUM_LEN);
        m.addAttribute("conId_4", comService.getPortByIndex(4).getComName());
        trainService.deleteTrainByConId(comService.getPortByIndex(4).getComName());
        procesCom4.sendDataTSR4000(GlobalRight.getSequenceIdHex_4() + "CABORT933C" + GlobalRight.getSuffixHex_4());
        return "proces/scale4";
    }

    @PostMapping("/showweighingWagons4")
    public String showWagon4(Model m) {
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(comService.getPortByIndex(4).getComName()));
        return "proces/beans/opdata4";
    }

    @PostMapping("/updateAllWeighing_4")
    public String updateWagon4(Model m) {
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(comService.getPortByIndex(4).getComName()));
        return "proces/beans/opdata4";
    }

    @PostMapping("/addwagonWeighing_4")
    public String addWagon4(Model m,
            @RequestParam("wagonNumber") String wagonNumber,
            @RequestParam("product") String product,
            @RequestParam(value = "count", required = false, defaultValue = "0") int count) {
        trainService.addWagonToTrain(comService.getPortByIndex(4).getComName(), wagonNumber, product, count);
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(comService.getPortByIndex(4).getComName()));
        return "proces/beans/opdata4";
    }

    @PostMapping("/editWgon4")
    @ResponseBody
    public Map<String, Object> editWagon4(
            @RequestParam("connId") String connId,
            @RequestParam("id") Long id,
            @RequestParam("product") String product,
            @RequestParam("wagonNumber") String wagonNum) {

        return trainService.updateWagonToTrain(id, connId, wagonNum, product,
                comService.getPortByIndex(4).isRightToUpdateTare());
    }

    @GetMapping("/pdf4")
    public ResponseEntity<Resource> getPdf4() {
        FileSystemResource file = new FileSystemResource(RepoInit.PDF_REPOSITOR_LAST_4 + "/report4.pdf");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(file);
    }

    @GetMapping(value = "/sendscale4", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getSendScale4() {
        return emitter.addEmitter();
    }

}
