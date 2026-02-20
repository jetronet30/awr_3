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
import com.jaba.awr_3.core.connectors.TcpService;
import com.jaba.awr_3.core.globalvar.GlobalRight;
import com.jaba.awr_3.core.process.ProcesTcp0;
import com.jaba.awr_3.core.prodata.services.TrainService;
import com.jaba.awr_3.core.units.UnitService;
import com.jaba.awr_3.inits.repo.RepoInit;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ScaleCon5 {
    private final TrainService trainService;
    private final TcpService tcpService;
    private final EmitterServic emitter;
    private final ProcesTcp0 procesTcp0;

    @PostMapping("/scale5")
    public String postSacale5(Model m) {
        m.addAttribute("cam5Enabled", false);
        m.addAttribute("magonNumLeght_5", UnitService.W_NUM_LEN);
        m.addAttribute("conId_5", tcpService.getTcpByIndex(5).getTcpName());
        return "proces/scale5";
    }

    @PostMapping("/startWeighing_5")
    public String startWeighing5(Model m) {
        m.addAttribute("cam5Enabled", false);
        m.addAttribute("magonNumLeght_5", UnitService.W_NUM_LEN);
        m.addAttribute("conId_5", tcpService.getTcpByIndex(5).getTcpName());
        if (!trainService.isWorkInProgress(tcpService.getTcpByIndex(5).getTcpName())) {
            procesTcp0.sendDataTSR4000(GlobalRight.getSequenceIdHex_5() + "CSTART7C34" + GlobalRight.getSuffixHex_5());
        }
        return "proces/scale5";
    }

    @PostMapping("/doneWeighing_5")
    public String doneWeighing5(Model m) {
        m.addAttribute("cam5Enabled", false);
        m.addAttribute("magonNumLeght_5", UnitService.W_NUM_LEN);
        m.addAttribute("conId_5", tcpService.getTcpByIndex(5).getTcpName());
        trainService.closeTrain(tcpService.getTcpByIndex(5).getTcpName());
        return "proces/scale5";
    }

    @PostMapping("/abortWeighing_5")
    public String abortWeighing5(Model m) {
        m.addAttribute("cam5Enabled", false);
        m.addAttribute("magonNumLeght_5", UnitService.W_NUM_LEN);
        m.addAttribute("conId_5", tcpService.getTcpByIndex(5).getTcpName());
        trainService.deleteTrainByConId(tcpService.getTcpByIndex(5).getTcpName());
        procesTcp0.sendDataTSR4000(GlobalRight.getSequenceIdHex_5() + "CABORT933C" + GlobalRight.getSuffixHex_5());
        return "proces/scale5";
    }

    @PostMapping("/showweighingWagons5")
    public String showWagon5(Model m) {
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(tcpService.getTcpByIndex(5).getTcpName()));
        return "proces/beans/opdata5";
    }

    @PostMapping("/updateAllWeighing_5")
    public String updateWagon5(Model m) {
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(tcpService.getTcpByIndex(5).getTcpName()));
        return "proces/beans/opdata5";
    }

    @PostMapping("/addwagonWeighing_5")
    public String addWagon5(Model m,
            @RequestParam("wagonNumber") String wagonNumber,
            @RequestParam("product") String product,
            @RequestParam(value = "count", required = false, defaultValue = "0") int count) {
        trainService.addWagonToTrain(tcpService.getTcpByIndex(5).getTcpName(), wagonNumber, product, count);
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(tcpService.getTcpByIndex(5).getTcpName()));
        return "proces/beans/opdata5";
    }

    @PostMapping("/editWgon5")
    @ResponseBody
    public Map<String, Object> editWagon5(
            @RequestParam("connId") String connId,
            @RequestParam("id") Long id,
            @RequestParam("product") String product,
            @RequestParam("wagonNumber") String wagonNum) {

        return trainService.updateWagonToTrain(id, connId, wagonNum, product,
                tcpService.getTcpByIndex(5).isRightToUpdateTare());
    }

    @GetMapping("/pdf5")
    public ResponseEntity<Resource> getPdf5() {
        FileSystemResource file = new FileSystemResource(RepoInit.PDF_REPOSITOR_LAST_5 + "/report5.pdf");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(file);
    }

    @GetMapping(value = "/sendscale5", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getSendScale5() {
        return emitter.addEmitter();
    }

}
