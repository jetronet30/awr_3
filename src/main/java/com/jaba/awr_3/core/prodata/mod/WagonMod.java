package com.jaba.awr_3.core.prodata.mod;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WagonMod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long sysId;
    private String processId;
    private String sacleName;
    private String connId;
    private String wagonNumber;
    private String product;
    private int axle;
    private String speed;
    private int rowNum;
    private BigDecimal weight;
    private BigDecimal tare;
    private BigDecimal neto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id")
    private TrainMod train;

}
