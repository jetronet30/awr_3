package com.jaba.awr_3.inits.postgres;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jaba.awr_3.inits.repo.RepoInit;

@Service
public class DataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataService.class);
    private static final File DATA_SETTINGS_JSON = new File(RepoInit.SERVER_SETTINGS_REPO, "datasettings.json");
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public static void initDataSettings() {
        if (!DATA_SETTINGS_JSON.exists()) {
            try {
                DATA_SETTINGS_JSON.createNewFile();
                DataMod dMod = new DataMod();
                dMod.setDataName("m_db");
                dMod.setDataPassword("bostana30");
                dMod.setDataUser("jetronet");
                dMod.setDataPort(5432);
                dMod.setDataHost("localhost");
                dMod.setDataUrl("jdbc:postgresql://" + "localhost" + ":" + 5432 + "/" + "m_db");
                dMod.setResDataName("rm_db");
                dMod.setResDataPassword("bostana30");
                dMod.setResDataUser("jetronet");
                dMod.setResDataPort(5432);
                dMod.setResDataHost("localhost");
                dMod.setResDataUrl("jdbc:postgresql://" + "localhost" + ":" + 5432 + "/" + "rm_db");
                MAPPER.writeValue(DATA_SETTINGS_JSON, dMod);
                LOGGER.info("Default DATA settings created at {}", DATA_SETTINGS_JSON.getAbsolutePath());
            } catch (Exception e) {
                LOGGER.error("Failed to create DATA SETTINGS {}", e);
            }
        }
    }

    public Map<String, Object> updateDataSettings(String name, String user, String pass, String rePass, String host,
            int port) {
        Map<String, Object> respons = new HashMap<>();
        if (pass.equals(rePass)) {
            try {
                DataMod dMod = new DataMod();
                dMod.setDataName(name);
                dMod.setDataUser(user);
                dMod.setDataPassword(pass);
                dMod.setDataPort(port);
                dMod.setDataHost(host);
                dMod.setDataUrl("jdbc:postgresql://" + host + ":" + port + "/" + name);
                MAPPER.writeValue(DATA_SETTINGS_JSON, dMod);
                respons.put("success", true);
            } catch (Exception e) {
                respons.put("success", false);
                respons.put("message", "Failed to Update DATA SETTINGS ");
                LOGGER.error("Failed to Update DATA SETTINGS {}", e);
            }

        }else{
            respons.put("success", false);
            respons.put("message", "passwors not equals ");
        }

        return respons;
    }

    public static DataMod getDataSettingsStatic() {
        DataMod dMod;
        try {
            dMod = MAPPER.readValue(DATA_SETTINGS_JSON, DataMod.class);
        } catch (Exception e) {
            dMod = null;
        }
        return dMod;
    }

    public DataMod getDataSettings() {
        DataMod dMod;
        try {
            dMod = MAPPER.readValue(DATA_SETTINGS_JSON, DataMod.class);
        } catch (Exception e) {
            dMod = null;
        }
        return dMod;
    }

}
