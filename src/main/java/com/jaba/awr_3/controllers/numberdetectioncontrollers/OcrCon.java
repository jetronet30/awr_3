package com.jaba.awr_3.controllers.numberdetectioncontrollers;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

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
        return "numberdetection/ocr";
    }

    /**
     * სრული განახლება ერთი OCR მოდელისთვის (index-ით)
     */
    @PostMapping("/updateOcrSettings")
    @ResponseBody
    public Map<String, Object> updateOcrSettings(
            @RequestParam("index") int index,
            @RequestParam("rtspUrl1") String rtspUrl1,
            @RequestParam("rtspUrl2") String rtspUrl2,
            @RequestParam("minConfidence_1") double minConfidence_1,
            @RequestParam("minObJecWidth_1") int minObJecWidth_1,
            @RequestParam("minobJecHeight_1") int minobJecHeight_1,
            @RequestParam("minConfidence_2") double minConfidence_2,
            @RequestParam("minObJecWidth_2") int minObJecWidth_2,
            @RequestParam("minobJecHeight_2") int minobJecHeight_2,
            @RequestParam("activeDetection") boolean activeDetection,
            @RequestParam("activeStream") boolean activeStream) {

        return ocrService.updateOcrByIndex(
                index,
                rtspUrl1, rtspUrl2,
                minConfidence_1, minObJecWidth_1, minobJecHeight_1,
                minConfidence_2, minObJecWidth_2, minobJecHeight_2,
                activeDetection, activeStream);
    }

    @PostMapping("/yoloupload")
    public String uploadYoloModel(@RequestParam("yolo") MultipartFile file, Model m) {
        ocrService.uploadYoloModel(file);
        m.addAttribute("ocrModels", ocrService.listOcrModels());
        return "numberdetection/ocr";
    }

    @PostMapping("/trocrupload")
    public String uploadTrocrModel(@RequestParam("trocr") MultipartFile file, Model m) {
        ocrService.uploadTrocrModel(file);
        m.addAttribute("ocrModels", ocrService.listOcrModels());
        return "numberdetection/ocr";
    }

    /**
     * სერვერის გადატვირთვა (ანალოგიურად TcpCon-ში)
     */
    @PostMapping("/ocr-setting-reboot")
    public String setOcrAndReboot(Model model) {
        ServerManager.reboot();
        return "settings/reboot"; // იგივე გვერდი, რაც TCP-ში
    }

}