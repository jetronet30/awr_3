package com.jaba.awr_3.core.prodata.services;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.jaba.awr_3.core.prodata.jparepo.TrainJpa;
import com.jaba.awr_3.core.prodata.jparepo.WagonJpa;
import com.jaba.awr_3.core.prodata.mod.TrainMod;
import com.jaba.awr_3.core.prodata.mod.WagonMod;
import com.jaba.awr_3.servermanager.ServerManger;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TrainService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrainService.class);
    private final TrainJpa trainJpa;
    private final WagonJpa wagonJpa;

    @Transactional
    public void addTrain(String conId, String instrument, String scaleName) {
        TrainMod train = new TrainMod();
        train.setConId(conId);
        train.setWeighingStartDateTime(ServerManger.getSystemDateTime());
        train.setInstrument(instrument);
        train.setScaleName(scaleName);
        train.setOpen(true);
        train.setCorrected(false);
        train.setWagoCount(0);
        trainJpa.save(train);
    }

    @Transactional
    public void addWagonToTrain(String conId, String scaleName, int rowNum, String weighString) {
        TrainMod train = trainJpa.findByOpenTrueAndConId(conId).orElse(null);
        if (train != null) {
            for (WagonMod w : train.getWagons()) {
                if (w.getRowNum() != rowNum) {
                    WagonMod wagon = new WagonMod();
                    wagon.setSacleName(scaleName);
                    train.getWagons().add(wagon);
                    wagon.setTrain(train);
                    wagon.setRowNum(rowNum);
                    wagon.setWeight(getBigDecimal(weighString));
                    wagonJpa.save(wagon);
                }else{
                    w.setWeight(getBigDecimal(weighString));
                    wagonJpa.save(w);
                }
            }
        } else {
            LOGGER.warn("No open train found for conId: {}", conId);
        }

    }


    private BigDecimal getBigDecimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

}
