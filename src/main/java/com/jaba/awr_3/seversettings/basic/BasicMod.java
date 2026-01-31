package com.jaba.awr_3.seversettings.basic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BasicMod {
    private String language;
    private String timeZone;
    private int listingPort;
    private String listingAddress;

}

