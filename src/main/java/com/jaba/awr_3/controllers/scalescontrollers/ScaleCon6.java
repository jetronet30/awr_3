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
import com.jaba.awr_3.core.process.ProcesTcp1;
import com.jaba.awr_3.core.prodata.services.TrainService;
import com.jaba.awr_3.core.units.UnitService;
import com.jaba.awr_3.inits.repo.RepoInit;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ScaleCon6 {
    private final TrainService trainService;
    private final TcpService tcpService;
    private final EmitterServic emitter;
    private final ProcesTcp1 procesTcp1;

    @PostMapping("/scale6")
    public String postSacale6(Model m) {
        m.addAttribute("cam6Enabled", false);
        m.addAttribute("magonNumLeght_6", UnitService.W_NUM_LEN);
        m.addAttribute("conId_6", tcpService.getTcpByIndex(6).getTcpName());
        return "proces/scale6";
    }

    @PostMapping("/startWeighing_6")
    public String startWeighing6(Model m) {
        m.addAttribute("cam6Enabled", false);
        m.addAttribute("magonNumLeght_6", UnitService.W_NUM_LEN);
        m.addAttribute("conId_6", tcpService.getTcpByIndex(6).getTcpName());
        if (!trainService.isWorkInProgress(tcpService.getTcpByIndex(6).getTcpName())) {
            procesTcp1.sendDataTSR4000(GlobalRight.getSequenceIdHex_6() + "CSTART7C34" + GlobalRight.getSuffixHex_6());
        }
        return "proces/scale6";
    }

    @PostMapping("/doneWeighing_6")
    public String doneWeighing6(Model m) {
        m.addAttribute("cam6Enabled", false);
        m.addAttribute("magonNumLeght_6", UnitService.W_NUM_LEN);
        m.addAttribute("conId_6", tcpService.getTcpByIndex(6).getTcpName());
        trainService.closeTrain(tcpService.getTcpByIndex(6).getTcpName());
        return "proces/scale6";
    }

    @PostMapping("/abortWeighing_6")
    public String abortWeighing6(Model m) {
        m.addAttribute("cam6Enabled", false);
        m.addAttribute("magonNumLeght_6", UnitService.W_NUM_LEN);
        m.addAttribute("conId_6", tcpService.getTcpByIndex(6).getTcpName());
        trainService.deleteTrainByConId(tcpService.getTcpByIndex(6).getTcpName());
        procesTcp1.sendDataTSR4000(GlobalRight.getSequenceIdHex_6() + "CABORT933C" + GlobalRight.getSuffixHex_6());
        return "proces/scale6";
    }

    @PostMapping("/showweighingWagons6")
    public String showWagon6(Model m) {
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(tcpService.getTcpByIndex(6).getTcpName()));
        return "proces/beans/opdata6";
    }

    @PostMapping("/updateAllWeighing_6")
    public String updateWagon6(Model m) {
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(tcpService.getTcpByIndex(6).getTcpName()));
        return "proces/beans/opdata6";
    }

    @PostMapping("/addwagonWeighing_6")
    public String addWagon6(Model m,
            @RequestParam("wagonNumber") String wagonNumber,
            @RequestParam("product") String product,
            @RequestParam(value = "count", required = false, defaultValue = "0") int count) {
        trainService.addWagonToTrain(tcpService.getTcpByIndex(6).getTcpName(), wagonNumber, product, count);
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(tcpService.getTcpByIndex(6).getTcpName()));
        return "proces/beans/opdata6";
    }

    @PostMapping("/editWgon6")
    @ResponseBody
    public Map<String, Object> editWagon6(
            @RequestParam("connId") String connId,
            @RequestParam("id") Long id,
            @RequestParam("product") String product,
            @RequestParam("wagonNumber") String wagonNum) {

        return trainService.updateWagonToTrain(id, connId, wagonNum, product,
                tcpService.getTcpByIndex(6).isRightToUpdateTare());
    }

    @GetMapping("/pdf6")
    public ResponseEntity<Resource> getPdf6() {
        FileSystemResource file = new FileSystemResource(RepoInit.PDF_REPOSITOR_LAST_6 + "/report6.pdf");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(file);
    }

    @GetMapping(value = "/sendscale6", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getSendScale6() {
        return emitter.addEmitter();
    }

}
