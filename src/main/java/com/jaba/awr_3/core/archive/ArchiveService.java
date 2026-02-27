package com.jaba.awr_3.core.archive;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.jaba.awr_3.core.connectors.ComService;
import com.jaba.awr_3.core.connectors.TcpService;
import com.jaba.awr_3.core.pdf.PdfCreator;
import com.jaba.awr_3.core.prodata.jparepo.TrainJpa;
import com.jaba.awr_3.core.prodata.jparepo.WagonJpa;
import com.jaba.awr_3.core.prodata.mod.TrainMod;
import com.jaba.awr_3.core.prodata.mod.WagonMod;
import com.jaba.awr_3.core.prodata.services.TrainService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArchiveService {
    private final TrainJpa trainJpa;
    private final PdfCreator pdfCreator;
    private final WagonJpa wagonJpa;
    private final TrainService trainService;
    private final ComService comService;
    private final TcpService tcpService;

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveService.class);

    public List<TrainMod> getLas100Trains() {
        return trainJpa.findTop100ByOrderByWeighingStopDateTimeDesc();
    }

    public List<String> getScaleNames() {
        return trainJpa.findDistinctScaleNames();
    }

    public List<TrainMod> getTrainsWithoutNumbers() {
        return trainJpa.findAllByAllwagonsNumberedFalseOrderByWeighingStopDateTimeDesc();
    }

    public List<TrainMod> getTrainsWithoutMatched() {
        return trainJpa.findAllByMatchedFalseOrderByWeighingStopDateTimeDesc();
    }

    public List<TrainMod> getTrainsFiltered(String scaleName, String dateFrom, String dateTo) {
        // თუ scaleName არის "all" ან ცარიელი → გავხადოთ null (რომ არ გაფილტროს)
        if (scaleName == null || scaleName.isBlank() || scaleName.equalsIgnoreCase("all")) {
            scaleName = null;
        }
        // dateFrom დამუშავება
        if (dateFrom != null && !dateFrom.isBlank()) {
            dateFrom = dateFrom + " 00:00:00";
        } else {
            dateFrom = null;
        }
        // dateTo დამუშავება
        if (dateTo != null && !dateTo.isBlank()) {
            dateTo = dateTo + " 23:59:59";
        } else {
            dateTo = null;
        }
        return trainJpa.findFilteredTrains(scaleName, dateFrom, dateTo);
    }

    public List<WagonMod> getWagonsByTrainId(Long id) {

        return trainService.getWagonsOpenAndByConIdAndSortedRow(id);
    }

    public Map<String, Object> setWagonNumber(Long id, String wagonNumber, String product) {
        Map<String, Object> response = new HashMap<>();
        WagonMod wagon = wagonJpa.findById(id).orElse(null);
        if (wagon == null) {
            response.put("success", false);
            response.put("message", "Wagon not found with id: " + id);
            LOGGER.warn("Wagon not found with id: {}", id);
            return response;
        }
        if (wagon.getTrain().isBlocked()) {
            response.put("success", false);
            response.put("message", "Train  is Blocked : " + id);
            LOGGER.warn("Train is Blocked: {}", id);
            return response;
        }

        String conId = wagon.getConnId();
        boolean updateTare = false;
        if (comService.getPortByName(conId) != null) {
            updateTare = comService.getPortByName(conId).isRightToUpdateTare();
        } else if (tcpService.getTcpByName(conId) != null) {
            updateTare = tcpService.getTcpByName(conId).isRightToUpdateTare();
        }

        response = trainService.updateWagonToTrain(id, conId, wagonNumber, product, updateTare);

        pdfCreator.createPdfWeb(wagon.getTrain());
        return response;
    }

    public void setTrainBlocked(Long id) {

    }

    public void createPdfForArChiv(Long id) {
        TrainMod train = trainJpa.findById(id).orElse(null);
        if (train == null) {
            LOGGER.warn("No train found for id: {}", id);
            return;
        }
        pdfCreator.createPdfWeb(train);
        LOGGER.info("PDF created for train with id: {}", id);
    }

}
