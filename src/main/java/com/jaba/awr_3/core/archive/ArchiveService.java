package com.jaba.awr_3.core.archive;

import java.util.List;

import org.springframework.stereotype.Service;

import com.jaba.awr_3.core.prodata.jparepo.TrainJpa;
import com.jaba.awr_3.core.prodata.mod.TrainMod;
import com.jaba.awr_3.core.prodata.mod.WagonMod;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArchiveService {
    private final TrainJpa trainJpa;

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

    public List<WagonMod> getWagonsByTrainId(Long id){
        return trainJpa.findById(id).orElse(null).getWagons();
    }

}
