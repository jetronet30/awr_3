package com.jaba.awr_3.controllers.scalescontrollers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
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
import com.jaba.awr_3.core.prodata.jparepo.TrainJpa;
import com.jaba.awr_3.core.prodata.mod.TrainMod;
import com.jaba.awr_3.core.prodata.mod.WagonMod;
import com.jaba.awr_3.core.units.UnitService;
import com.jaba.awr_3.inits.repo.RepoInit;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ArchivCon {

    private final ArchiveService archiveService;
    private final TrainJpa trainJpa;

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

    @PostMapping(value = "/archive/showPDF/{id}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> postPdf(
            @PathVariable Long id,
            @RequestParam(value = "printOnlyIds", required = false) String printOnlyIdsStr,
            @RequestParam(value = "t", required = false) String timestamp) {

        // 1. ვპოულობთ ტრეინს
        TrainMod train = trainJpa.findById(id).orElse(null);
        if (train == null) {
            return ResponseEntity.notFound().build();
        }

        Set<Long> selectedWagonIds = null;

        // თუ printOnlyIds მოდის და არ არის ცარიელი → ვქმნით id-ების სეტს
        if (StringUtils.isNotBlank(printOnlyIdsStr)) {
            try {
                selectedWagonIds = Stream.of(printOnlyIdsStr.split(","))
                        .map(String::trim)
                        .filter(StringUtils::isNotBlank)
                        .map(Long::parseLong)
                        .collect(Collectors.toSet());
            } catch (NumberFormatException e) {
                // თუ id-ები არასწორია → fallback-ად ყველა ვაგონი
                selectedWagonIds = null;
            }
        }

        // 2. ვაგონების ფილტრაცია
        List<WagonMod> wagons = new ArrayList<>();

        // თუ selectedWagonIds არსებობს და არ არის ცარიელი → მხოლოდ მონიშნულები
        if (selectedWagonIds != null && !selectedWagonIds.isEmpty()) {
            for (WagonMod wagon : train.getWagons()) {
                if (wagon.getId() != null && selectedWagonIds.contains(wagon.getId())) {
                    wagons.add(wagon);
                }
            }
        }
        // თუ printOnlyIds არ მოვიდა ან ცარიელია → ყველა ვაგონი
        else {
            wagons.addAll(train.getWagons());
        }

        // თუ საბოლოოდ არც ერთი ვაგონი არ არის → 404
        if (wagons.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<Long> wIds = new ArrayList<>();
        wIds.clear();
        for (WagonMod wm : wagons) {
            wIds.add(wm.getId());
        }

        // 3. PDF-ის გენერაცია
        archiveService.createFragmentPdfForArchive(id, wIds);
        for (WagonMod wm : wagons) {
            System.out.println(wm.getId() + " +++++++++>>>>>>>>>>>>>>");
        }

        FileSystemResource file = new FileSystemResource(RepoInit.PDF_REPOSITOR_FULL + "/" + id + ".pdf");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(file);
    }

    @GetMapping(value = "/archive/showPDF/{id}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> getPdf(
            @PathVariable Long id,
            @RequestParam(value = "printOnlyIds", required = false) String printOnlyIdsStr,
            @RequestParam(value = "t", required = false) String timestamp) {

        // 1. ვპოულობთ ტრეინს
        TrainMod train = trainJpa.findById(id).orElse(null);
        if (train == null) {
            return ResponseEntity.notFound().build();
        }

        Set<Long> selectedWagonIds = null;

        // თუ printOnlyIds მოდის და არ არის ცარიელი → ვქმნით id-ების სეტს
        if (StringUtils.isNotBlank(printOnlyIdsStr)) {
            try {
                selectedWagonIds = Stream.of(printOnlyIdsStr.split(","))
                        .map(String::trim)
                        .filter(StringUtils::isNotBlank)
                        .map(Long::parseLong)
                        .collect(Collectors.toSet());
            } catch (NumberFormatException e) {
                // თუ id-ები არასწორია → fallback-ად ყველა ვაგონი
                selectedWagonIds = null;
            }
        }

        // 2. ვაგონების ფილტრაცია
        List<WagonMod> wagons = new ArrayList<>();

        // თუ selectedWagonIds არსებობს და არ არის ცარიელი → მხოლოდ მონიშნულები
        if (selectedWagonIds != null && !selectedWagonIds.isEmpty()) {
            for (WagonMod wagon : train.getWagons()) {
                if (wagon.getId() != null && selectedWagonIds.contains(wagon.getId())) {
                    wagons.add(wagon);
                }
            }
        }
        // თუ printOnlyIds არ მოვიდა ან ცარიელია → ყველა ვაგონი
        else {
            wagons.addAll(train.getWagons());
        }

        // თუ საბოლოოდ არც ერთი ვაგონი არ არის → 404
        if (wagons.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // 3. PDF-ის გენერაცია
        archiveService.createPdfForArChiv(id);
        for (WagonMod wm : wagons) {
            System.out.println(wm.getId() + " ++++++++++++++++++++++++++++++++++++++++++++++++++++");
        }

        FileSystemResource file = new FileSystemResource(RepoInit.PDF_REPOSITOR_FULL + "/" + id + ".pdf");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(file);
    }


    //////////////////////////////////////////////////////////////////////////////
    /// 
    @GetMapping("/archive/showPDF/{id}")
    public ResponseEntity<Resource> getPdf0(@PathVariable Long id) {
        archiveService.createPdfForArChiv(id);
        FileSystemResource file = new FileSystemResource(RepoInit.PDF_REPOSITOR_FULL + "/" + id + ".pdf");
        System.out.println(" test    test     "+ id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(file);
    }

    @PostMapping("/archive/showPDF/post/{id}")
    public ResponseEntity<Resource> postPDF(@PathVariable Long id) {
        archiveService.createPdfForArChiv(id);
        FileSystemResource file = new FileSystemResource(RepoInit.PDF_REPOSITOR_FULL + "/" + id + ".pdf");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(file);
    }
    ////

}