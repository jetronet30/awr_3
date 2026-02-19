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
import com.jaba.awr_3.core.connectors.TcpService;
import com.jaba.awr_3.core.globalvar.GlobalRight;
import com.jaba.awr_3.core.process.ProcesTcp4;
import com.jaba.awr_3.core.prodata.services.TrainService;
import com.jaba.awr_3.core.units.UnitService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ScaleCon9 {
    private final TrainService trainService;
    private final TcpService tcpService;
    private final EmitterServic emitter;
    private final ProcesTcp4 procesTcp4;

    @PostMapping("/scale9")
    public String postSacale9(Model m) {
        m.addAttribute("cam9Enabled", false);
        m.addAttribute("magonNumLeght_9", UnitService.W_NUM_LEN);
        m.addAttribute("conId_9", tcpService.getTcpByIndex(9).getTcpName());
        return "proces/scale9";
    }

    @PostMapping("/startWeighing_9")
    public String startWeighing9(Model m) {
        m.addAttribute("cam9Enabled", false);
        m.addAttribute("magonNumLeght_9", UnitService.W_NUM_LEN);
        m.addAttribute("conId_9", tcpService.getTcpByIndex(9).getTcpName());
        if (!trainService.isWorkInProgress(tcpService.getTcpByIndex(9).getTcpName())) {
            procesTcp4.sendDataTSR4000(GlobalRight.getSequenceIdHex_9() + "CSTART7C34" + GlobalRight.getSuffixHex_9());
        }
        return "proces/scale9";
    }

    @PostMapping("/doneWeighing_9")
    public String doneWeighing9(Model m) {
        m.addAttribute("cam9Enabled", false);
        m.addAttribute("magonNumLeght_9", UnitService.W_NUM_LEN);
        m.addAttribute("conId_9", tcpService.getTcpByIndex(9).getTcpName());
        trainService.closeTrain(tcpService.getTcpByIndex(9).getTcpName());
        return "proces/scale9";
    }

    @PostMapping("/abortWeighing_9")
    public String abortWeighing9(Model m) {
        m.addAttribute("cam9Enabled", false);
        m.addAttribute("magonNumLeght_9", UnitService.W_NUM_LEN);
        m.addAttribute("conId_9", tcpService.getTcpByIndex(9).getTcpName());
        trainService.deleteTrainByConId(tcpService.getTcpByIndex(9).getTcpName());
        procesTcp4.sendDataTSR4000(GlobalRight.getSequenceIdHex_9() + "CABORT933C" + GlobalRight.getSuffixHex_9());
        return "proces/scale9";
    }

    @PostMapping("/showweighingWagons9")
    public String showWagon9(Model m) {
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(tcpService.getTcpByIndex(9).getTcpName()));
        return "proces/beans/opdata9";
    }

    @PostMapping("/updateAllWeighing_9")
    public String updateWagon9(Model m) {
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(tcpService.getTcpByIndex(9).getTcpName()));
        return "proces/beans/opdata9";
    }

    @PostMapping("/addwagonWeighing_9")
    public String addWagon9(Model m,
            @RequestParam("wagonNumber") String wagonNumber,
            @RequestParam("product") String product,
            @RequestParam(value = "count", required = false, defaultValue = "0") int count) {
        trainService.addWagonToTrain(tcpService.getTcpByIndex(9).getTcpName(), wagonNumber, product, count);
        m.addAttribute("wagons",
                trainService.getWagonsOpenAndByConIdAndSortedRow(tcpService.getTcpByIndex(9).getTcpName()));
        return "proces/beans/opdata9";
    }

    @PostMapping("/editWgon9")
    @ResponseBody
    public Map<String, Object> editWagon9(
            @RequestParam("connId") String connId,
            @RequestParam("id") Long id,
            @RequestParam("product") String product,
            @RequestParam("wagonNumber") String wagonNum) {

        return trainService.updateWagonToTrain(id, connId, wagonNum, product,
                tcpService.getTcpByIndex(9).isRightToUpdateTare());
    }

    @GetMapping(value = "/sendscale9", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getSendScale9() {
        return emitter.addEmitter();
    }

}
