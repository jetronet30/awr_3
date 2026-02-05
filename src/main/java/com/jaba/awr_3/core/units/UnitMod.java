package com.jaba.awr_3.core.units;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnitMod {
    private String weightUnit;
    private String speedUnit;
    private int wagonNumLenUnit;
    private BigDecimal tarLimit;

}
