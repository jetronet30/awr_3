package com.jaba.awr_3.core.archive;

import java.util.List;

import org.springframework.stereotype.Service;

import com.jaba.awr_3.core.prodata.jparepo.TrainJpa;
import com.jaba.awr_3.core.prodata.mod.TrainMod;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArchiveService {
    private final TrainJpa trainJpa;

    public List<TrainMod> getLas100Trains(){
        return trainJpa.findTop100ByOrderByWeighingStopDateTimeDesc();   
    }

    public List<String> getScaleNames(){
        return trainJpa.findDistinctScaleNames();
    }


}
