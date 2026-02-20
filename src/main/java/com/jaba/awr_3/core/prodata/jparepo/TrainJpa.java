package com.jaba.awr_3.core.prodata.jparepo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.jaba.awr_3.core.prodata.mod.TrainMod;

public interface TrainJpa extends JpaRepository<TrainMod, Long> {
    Optional<TrainMod> findByOpenTrueAndConId(String conId);

    Optional<TrainMod> findByOpenTrueAndDoneTrueAndConId(String conId);

    boolean existsByOpenTrueAndConId(String conId);

    List<TrainMod> findAllByDoneTrueAndOpenFalseOrderByWeighingStartDateTimeDesc();

    List<TrainMod> findTop100ByOrderByWeighingStopDateTimeDesc();

    List<TrainMod> findAllByOrderByWeighingStopDateTimeDesc();

    @Query("SELECT DISTINCT t.scaleName FROM TrainMod t WHERE t.scaleName IS NOT NULL ORDER BY t.scaleName ASC")
    List<String> findDistinctScaleNames();
}
