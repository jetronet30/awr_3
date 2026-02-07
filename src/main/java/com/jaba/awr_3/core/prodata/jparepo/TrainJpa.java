package com.jaba.awr_3.core.prodata.jparepo;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.jaba.awr_3.core.prodata.mod.TrainMod;

public interface TrainJpa  extends JpaRepository<TrainMod, Long>{
    Optional<TrainMod> findByOpenTrueAndConId(String conId);
    boolean existsByOpenTrueAndConId(String conId);
}
