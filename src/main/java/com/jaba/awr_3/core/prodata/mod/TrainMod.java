package com.jaba.awr_3.core.prodata.mod;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrainMod {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Long sysId;
    private String processId;
    private String scaleName;
    private String instrument;
    private String conId;
    private String weighingStartDateTime;
    private String weighingStopDateTime;
    private String direction;
    private String maxSpeed;
    private String minSpeed;
    private BigDecimal gross;
    private BigDecimal tare;
    private BigDecimal neto;
    private boolean open;
    private boolean corrected;
    private int wagoCount;
    private String videoPatch;

    @OneToMany(mappedBy = "train", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<WagonMod> wagons = new ArrayList<>();


}
