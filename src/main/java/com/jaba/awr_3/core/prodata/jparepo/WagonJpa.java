package com.jaba.awr_3.core.prodata.jparepo;

import com.jaba.awr_3.core.prodata.mod.WagonMod;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WagonJpa extends JpaRepository<WagonMod, Long> {

    // invalid ვაგონების წაშლა ერთი ტრეინისთვის
    @Modifying
    @Query("DELETE FROM WagonMod w WHERE w.train.id = :trainId AND w.valid = false")
    int deleteByTrainIdAndValidFalse(@Param("trainId") Long trainId);

    // სურვილისამებრ: rowNum-ით ძებნა კონკრეტულ ტრეინში
    // (გამოგადგებათ addWagonToTrain-ის ოპტიმიზაციისთვის)
    Optional<WagonMod> findByTrainIdAndRowNum(Long trainId, int rowNum);

    // თუ გჭირდებათ სხვა ფილტრები მოგვიანებით
    // List<WagonMod> findByTrainIdAndValidTrue(Long trainId);
}
