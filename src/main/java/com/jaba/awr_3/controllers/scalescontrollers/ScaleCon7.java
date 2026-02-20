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
import com.jaba.awr_3.core.process.ProcesTcp2;
import com.jaba.awr_3.core.prodata.services.TrainService;
import com.jaba.awr_3.core.units.UnitService;
import com.jaba.awr_3.inits.repo.RepoInit;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ScaleCon7 {
    private final TrainService trainService;
    private final TcpService tcpService;
    private final EmitterServic emitter;
    private final ProcesTcp2 procesTcp2;

    @PostMapping("/scale7")
    public String postSacale7(Model m) {
        m.addAttribute("cam7Enabled", false);
        m.addAttribute("magonNumLeght_7", UnitService.W_NUM_LEN);
        m.addAttribute("conId_7", tcpService.getTcpByIndex(7).getTcpName());
        return "proces/scale7";
    }

    @PostMapping("/startWeighing_7")
    public String startWeighing7(Model m) {
        m.addAttribute("cam7Enabled", false);
        m.addAttribute("magonNumLeght_7", UnitService.W_NUM_LEN);
        m.addAttribute("conId_7", tcpService.getTcpByIndex(7).getTcpName());
        if (!trainService.isWorkInProgress(tcpService.getTcpByIndex(7).getTcpName())) {
            procesTcp2.sendDataTSR4000(GlobalRight.getSequenceIdHex_7() + "CSTART7C34" + GlobalRight.getSuffixHex_7());
        }
        return "proces/scale7";
    }

    @PostMapping("/doneWeighing_7")
    public String doneWeighing7(Model m) {
        m.addAttribute("cam7Enabled", false);
        m.addAttribute("magonNumLeght_7", UnitService.W_NUM_LEN);
        m.addAttribute("conId_7", tcpService.getTcpByIndex(7).getTcpName());
        trainService.closeTrain(tcpService.getTcpByIndex(7).getTcpName());
        return "proces/scale7";
    }

    @PostMapping("/abortWeighing_7")
    public String abortWeighing7(Model m) {
        m.addAttribute("cam7Enabled", false);
        m.addAttribute("magonNumLeght_7", UnitService.W_NUM_LEN);
        m.addAttribute("conId_7", tcpService.getTcpByIndex(7).getTcpName());
        trainService.deleteTrainByConId(tcpService.getTcpByIndex(7).getTcpName());
        procesTcp2.sendDataTSR4000(GlobalRight.getSequenceIdHex_7() + "CABORT933C" + GlobalRight.getSuffixHex_7());
        return "proces/scale7";
    }

    @PostMapping("/showweighingWagons7")
    public String showWagon7(Model m) {
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(tcpService.getTcpByIndex(7).getTcpName()));
        return "proces/beans/opdata7";
    }

    @PostMapping("/updateAllWeighing_7")
    public String updateWagon7(Model m) {
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(tcpService.getTcpByIndex(7).getTcpName()));
        return "proces/beans/opdata7";
    }

    @PostMapping("/addwagonWeighing_7")
    public String addWagon7(Model m,
            @RequestParam("wagonNumber") String wagonNumber,
            @RequestParam("product") String product,
            @RequestParam(value = "count", required = false, defaultValue = "0") int count) {
        trainService.addWagonToTrain(tcpService.getTcpByIndex(7).getTcpName(), wagonNumber, product, count);
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(tcpService.getTcpByIndex(7).getTcpName()));
        return "proces/beans/opdata7";
    }

    @PostMapping("/editWgon7")
    @ResponseBody
    public Map<String, Object> editWagon7(
            @RequestParam("connId") String connId,
            @RequestParam("id") Long id,
            @RequestParam("product") String product,
            @RequestParam("wagonNumber") String wagonNum) {

        return trainService.updateWagonToTrain(id, connId, wagonNum, product,
                tcpService.getTcpByIndex(7).isRightToUpdateTare());
    }

    @GetMapping("/pdf7")
    public ResponseEntity<Resource> getPdf7() {
        FileSystemResource file = new FileSystemResource(RepoInit.PDF_REPOSITOR_LAST_7 + "/report7.pdf");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(file);
    }

    @GetMapping(value = "/sendscale7", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getSendScale7() {
        return emitter.addEmitter();
    }

}
