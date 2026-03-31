package com.jaba.awr_3.security.mod;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeMod {
    private Long id;
    private String username;
    private String password;
    private String role;

}
