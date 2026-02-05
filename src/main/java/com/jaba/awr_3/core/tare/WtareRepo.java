package com.jaba.awr_3.core.tare;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WtareRepo extends JpaRepository <WtareMod, Long>{
    // არსებობს თუ არა wagonNumber-ით
    Optional<WtareMod> findByWagonNumber(String wagonNumber);
    // ყველა დალაგებული wagonNumber-ის მიხედვით (ASC)
    List<WtareMod> findAllByOrderByWagonNumberAsc();
    
}
