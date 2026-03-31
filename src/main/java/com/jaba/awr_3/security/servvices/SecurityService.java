package com.jaba.awr_3.security.servvices;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jaba.awr_3.inits.repo.RepoInit;
import com.jaba.awr_3.security.mod.SeMod;

@Service
public class SecurityService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityService.class);
    private static final File UNITS_JSON = new File(RepoInit.SERVER_SETTINGS_REPO, "security.json");
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public static void initSecS() {
        if (!UNITS_JSON.exists()) {
            try {
                UNITS_JSON.createNewFile();
                SeMod sMod_1 = new SeMod();
                sMod_1.setId(1L);
                sMod_1.setUsername("admin");
                sMod_1.setPassword("admin");
                sMod_1.setRole("ADMIN");
                SeMod sMod_2 = new SeMod();
                sMod_2.setId(2L);
                sMod_2.setUsername("user");
                sMod_2.setPassword("user");
                sMod_2.setRole("USER");
                MAPPER.writeValue(UNITS_JSON, new SeMod[] { sMod_1, sMod_2 });
                LOGGER.info(" write SECURITY to JSON COMPLECTE ");
            } catch (Exception e) {
                LOGGER.error("Error write SECURITY to JSON: {}", e.getMessage(), e);
            }
        }
    }   

}
