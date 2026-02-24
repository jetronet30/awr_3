package com.jaba.awr_3.controllers.numberdetectioncontrollers;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.jaba.awr_3.core.numberdetection.ocr.OcrService;
import com.jaba.awr_3.servermanager.ServerManager;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class OcrCon {

    private final OcrService ocrService;

    /**
     * OCR პარამეტრების გვერდის ჩატვირთვა
     */
    @PostMapping("/ocr")
    public String postOcr(Model model) {
        model.addAttribute("ocrModels", ocrService.listOcrModels());

        return "numberdetection/ocr"; // → შენი Thymeleaf/HTML შაბლონის სახელი
    }

    /**
     * სრული განახლება ერთი OCR მოდელისთვის (index-ით)
     */
    @PostMapping("/updateOcrSettings")
    @ResponseBody
    public Map<String, Object> updateOcrSettings(
            @RequestParam("index") int index,
            @RequestParam("cam1Usr") String cam1Usr,
            @RequestParam("cam2Usr") String cam2Usr,
            @RequestParam("cam1Passwd") String cam1Passwd,
            @RequestParam("cam2Passwd") String cam2Passwd,
            @RequestParam("cam1Port") int cam1Port,
            @RequestParam("cam2Port") int cam2Port,
            @RequestParam("rtspUrl1") String rtspUrl1,
            @RequestParam("rtspUrl2") String rtspUrl2,
            @RequestParam("roiX1") double roiX1,
            @RequestParam("roiY1") double roiY1,
            @RequestParam("roiX2") double roiX2,
            @RequestParam("roiY2") double roiY2,
            @RequestParam("activeDetection") boolean activeDetection,
            @RequestParam("activeStream") boolean activeStream) {

        return ocrService.updateOcrByIndex(
                index,
                cam1Usr, cam2Usr,
                cam1Passwd, cam2Passwd,
                cam1Port, cam2Port,
                rtspUrl1, rtspUrl2,
                roiX1, roiY1, roiX2, roiY2,
                activeDetection, activeStream);
    }

    /**
     * მხოლოდ Detection-ის ჩართვა/გამორთვა
     */
    @PostMapping("/ocr/setDetectionActive")
    @ResponseBody
    public Map<String, Object> setDetectionActive(
            @RequestParam("index") int index,
            @RequestParam("active") boolean active) {
        return ocrService.setActiveDetection(index, active);
    }

    /**
     * მხოლოდ Stream-ის ჩართვა/გამორთვა
     */
    @PostMapping("/ocr/setStreamActive")
    @ResponseBody
    public Map<String, Object> setStreamActive(
            @RequestParam("index") int index,
            @RequestParam("active") boolean active) {
        return ocrService.setActiveStream(index, active);
    }

    /**
     * სერვერის გადატვირთვა (ანალოგიურად TcpCon-ში)
     */
    @PostMapping("/ocr-setting-reboot")
    public String setOcrAndReboot(Model model) {
        ServerManager.reboot();
        return "settings/reboot"; // იგივე გვერდი, რაც TCP-ში
    }

    // თუ გჭირდება GET მეთოდი ფორმის ჩასატვირთად (რეკომენდებულია)
    // @GetMapping("/ocr")
    // public String getOcr(Model model) {
    // return postOcr(model); // ან განსხვავებული ლოგიკა
    // }
}