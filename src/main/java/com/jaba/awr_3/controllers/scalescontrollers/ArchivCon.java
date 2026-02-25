package com.jaba.awr_3.controllers.scalescontrollers;

import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.jaba.awr_3.core.archive.ArchiveService;
import com.jaba.awr_3.core.units.UnitService;
import com.jaba.awr_3.inits.repo.RepoInit;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ArchivCon {
    private final ArchiveService archiveService;

    @PostMapping("/archive")
    public String postArchive(Model m) {
        m.addAttribute("magonNumLeght_train", UnitService.W_NUM_LEN);
        m.addAttribute("scaleNames", archiveService.getScaleNames());
        return "proces/archive/archive";
    }

    @PostMapping("/archive/showTrains")
    public String postTrains(Model m) {
        m.addAttribute("trains", archiveService.getLas100Trains());
        return "proces/archive/archivbean";
    }

    @PostMapping("/archive/notNumberd")
    public String postNotNumberd(Model m) {
        m.addAttribute("trains", archiveService.getTrainsWithoutNumbers());
        return "proces/archive/archivbean";
    }

    @PostMapping("/archive/notMatched")
    public String postNotMatched(Model m) {
        m.addAttribute("trains", archiveService.getTrainsWithoutMatched());
        return "proces/archive/archivbean";
    }

    @PostMapping("/archive/filter")
    public String postFilter(Model m,
            @RequestParam("scaleName") String scaleName,
            @RequestParam("dateFrom") String dateFrom,
            @RequestParam("dateTo") String dateTo) {
        m.addAttribute("trains", archiveService.getTrainsFiltered(scaleName, dateFrom, dateTo));
        return "proces/archive/archivbean";
    }

    @PostMapping("/archive/edit/{id}")
    public String getTrainArchiv(@PathVariable Long id, Model m) {
        m.addAttribute("wagons", archiveService.getWagonsByTrainId(id));
        m.addAttribute("video_1_exists", RepoInit.VIDEO_ARCHIVE + "/1_" + id + ".mp4");
        m.addAttribute("video_2_exists", RepoInit.VIDEO_ARCHIVE + "/2_" + id + ".mp4");
        m.addAttribute("id", id);
        System.out.println(id);
        return "proces/archive/trainpage";
    }

    @PostMapping("/arhcive/editWgonNumber")
    @ResponseBody
    public Map<String, Object> editWagon0(
            @RequestParam("id") Long id,
            @RequestParam("product") String product,
            @RequestParam("wagonNumber") String wagonNum) {

        return archiveService.setWagonNumber(id, wagonNum, product);
    }

    //////////////////////////////////////////////////////////////////////////////
    /// 
    @GetMapping("/archive/showPDF/{id}")
    public ResponseEntity<Resource> getPdf0(@PathVariable Long id) {
        FileSystemResource file = new FileSystemResource(RepoInit.PDF_REPOSITOR_FULL + "/" + id + ".pdf");
        System.out.println(" test    test     "+ id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(file);
    }

    @PostMapping("/archive/showPDF/post/{id}")
    public ResponseEntity<Resource> postPDF(@PathVariable Long id) {
        FileSystemResource file = new FileSystemResource(RepoInit.PDF_REPOSITOR_FULL + "/" + id + ".pdf");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(file);
    }
    ///////////////////////////////////////////////////////////////////////////////
    /// 

}
