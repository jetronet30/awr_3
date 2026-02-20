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
import com.jaba.awr_3.core.process.ProcesTcp3;
import com.jaba.awr_3.core.prodata.services.TrainService;
import com.jaba.awr_3.core.units.UnitService;
import com.jaba.awr_3.inits.repo.RepoInit;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ScaleCon8 {
    private final TrainService trainService;
    private final TcpService tcpService;
    private final EmitterServic emitter;
    private final ProcesTcp3 procesTcp3;

    @PostMapping("/scale8")
    public String postSacale8(Model m) {
        m.addAttribute("cam8Enabled", false);
        m.addAttribute("magonNumLeght_8", UnitService.W_NUM_LEN);
        m.addAttribute("conId_8", tcpService.getTcpByIndex(8).getTcpName());
        return "proces/scale8";
    }

    @PostMapping("/startWeighing_8")
    public String startWeighing8(Model m) {
        m.addAttribute("cam8Enabled", false);
        m.addAttribute("magonNumLeght_8", UnitService.W_NUM_LEN);
        m.addAttribute("conId_8", tcpService.getTcpByIndex(8).getTcpName());
        if (!trainService.isWorkInProgress(tcpService.getTcpByIndex(8).getTcpName())) {
            procesTcp3.sendDataTSR4000(GlobalRight.getSequenceIdHex_8() + "CSTART7C34" + GlobalRight.getSuffixHex_8());
        }
        return "proces/scale8";
    }

    @PostMapping("/doneWeighing_8")
    public String doneWeighing8(Model m) {
        m.addAttribute("cam8Enabled", false);
        m.addAttribute("magonNumLeght_8", UnitService.W_NUM_LEN);
        m.addAttribute("conId_8", tcpService.getTcpByIndex(8).getTcpName());
        trainService.closeTrain(tcpService.getTcpByIndex(8).getTcpName());
        return "proces/scale8";
    }

    @PostMapping("/abortWeighing_8")
    public String abortWeighing8(Model m) {
        m.addAttribute("cam8Enabled", false);
        m.addAttribute("magonNumLeght_8", UnitService.W_NUM_LEN);
        m.addAttribute("conId_8", tcpService.getTcpByIndex(8).getTcpName());
        trainService.deleteTrainByConId(tcpService.getTcpByIndex(8).getTcpName());
        procesTcp3.sendDataTSR4000(GlobalRight.getSequenceIdHex_8() + "CABORT933C" + GlobalRight.getSuffixHex_8());
        return "proces/scale8";
    }

    @PostMapping("/showweighingWagons8")
    public String showWagon8(Model m) {
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(tcpService.getTcpByIndex(8).getTcpName()));
        return "proces/beans/opdata8";
    }

    @PostMapping("/updateAllWeighing_8")
    public String updateWagon8(Model m) {
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(tcpService.getTcpByIndex(8).getTcpName()));
        return "proces/beans/opdata8";
    }

    @PostMapping("/addwagonWeighing_8")
    public String addWagon8(Model m,
            @RequestParam("wagonNumber") String wagonNumber,
            @RequestParam("product") String product,
            @RequestParam(value = "count", required = false, defaultValue = "0") int count) {
        trainService.addWagonToTrain(tcpService.getTcpByIndex(8).getTcpName(), wagonNumber, product, count);
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(tcpService.getTcpByIndex(8).getTcpName()));
        return "proces/beans/opdata8";
    }

    @PostMapping("/editWgon8")
    @ResponseBody
    public Map<String, Object> editWagon8(
            @RequestParam("connId") String connId,
            @RequestParam("id") Long id,
            @RequestParam("product") String product,
            @RequestParam("wagonNumber") String wagonNum) {

        return trainService.updateWagonToTrain(id, connId, wagonNum, product,
                tcpService.getTcpByIndex(8).isRightToUpdateTare());
    }

    @GetMapping("/pdf8")
    public ResponseEntity<Resource> getPdf8() {
        FileSystemResource file = new FileSystemResource(RepoInit.PDF_REPOSITOR_LAST_8 + "/report8.pdf");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(file);
    }

    @GetMapping(value = "/sendscale8", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getSendScale8() {
        return emitter.addEmitter();
    }

}
