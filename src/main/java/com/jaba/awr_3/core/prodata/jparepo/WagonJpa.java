package com.jaba.awr_3.core.prodata.jparepo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jaba.awr_3.core.prodata.mod.WagonMod;

public interface WagonJpa extends JpaRepository<WagonMod, Long> {

}
