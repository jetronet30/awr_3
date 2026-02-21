package com.jaba.awr_3.core.prodata.jparepo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jaba.awr_3.core.prodata.mod.TrainMod;

public interface TrainJpa extends JpaRepository<TrainMod, Long> {
    Optional<TrainMod> findByOpenTrueAndConId(String conId);

    Optional<TrainMod> findByOpenTrueAndDoneTrueAndConId(String conId);

    boolean existsByOpenTrueAndConId(String conId);

    List<TrainMod> findAllByDoneTrueAndOpenFalseOrderByWeighingStartDateTimeDesc();

    List<TrainMod> findTop100ByOrderByWeighingStopDateTimeDesc();

    List<TrainMod> findAllByOrderByWeighingStopDateTimeDesc();

    List<TrainMod> findAllByAllwagonsNumberedFalseOrderByWeighingStopDateTimeDesc();

    @Query("SELECT DISTINCT t.scaleName FROM TrainMod t WHERE t.scaleName IS NOT NULL ORDER BY t.scaleName ASC")
    List<String> findDistinctScaleNames();

    @Query("""
            SELECT t FROM TrainMod t
            WHERE (:scaleName IS NULL OR t.scaleName = :scaleName)
            AND (:dateFrom IS NULL OR t.weighingStopDateTime >= :dateFrom)
            AND (:dateTo IS NULL OR t.weighingStopDateTime <= :dateTo)
            ORDER BY t.weighingStopDateTime DESC
            """)
    List<TrainMod> findFilteredTrains(
            @Param("scaleName") String scaleName,
            @Param("dateFrom") String dateFrom,
            @Param("dateTo") String dateTo);

    List<TrainMod> findAllByMatchedFalseOrderByWeighingStopDateTimeDesc();
}
