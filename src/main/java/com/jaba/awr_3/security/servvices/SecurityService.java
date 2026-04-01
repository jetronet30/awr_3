package com.jaba.awr_3.security.servvices;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jaba.awr_3.inits.repo.RepoInit;
import com.jaba.awr_3.security.mod.SeMod;



import jakarta.annotation.PostConstruct;

@Service
public class SecurityService {

    // SecurityService-ში დაამატე ეს ველი
    private final PasswordEncoder passwordEncoder;

    // კონსტრუქტორი
    public SecurityService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityService.class);
    private static final File UNITS_JSON = new File(RepoInit.SERVER_SETTINGS_REPO, "security.json");
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private static final int MAX_USERS = 5;
    private static final int MAX_ADMINS = 2;

   
    @PostConstruct
    public void initSecS() {
        if (!UNITS_JSON.exists()) {
            try {
                UNITS_JSON.createNewFile();
                SeMod sMod_1 = new SeMod();
                sMod_1.setId(1L);
                sMod_1.setUsername("admin");
                sMod_1.setPassword(passwordEncoder.encode("admin")); // hashed
                sMod_1.setRole("ADMIN");

                SeMod sMod_2 = new SeMod();
                sMod_2.setId(2L);
                sMod_2.setUsername("user");
                sMod_2.setPassword(passwordEncoder.encode("user")); // hashed
                sMod_2.setRole("USER");

                MAPPER.writeValue(UNITS_JSON, new SeMod[] { sMod_1, sMod_2 });
                LOGGER.info(" write SECURITY to JSON COMPLETE ");
            } catch (Exception e) {
                LOGGER.error("Error write SECURITY to JSON: {}", e.getMessage(), e);
            }
        }
    }

    public List<SeMod> getAllS() {
        try {
            SeMod[] array = MAPPER.readValue(UNITS_JSON, SeMod[].class);
            return array != null ? new ArrayList<>(Arrays.asList(array)) : new ArrayList<>();
        } catch (Exception e) {
            LOGGER.error("Error read SECURITY from JSON: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public SeMod getSByUsername(String username) {
        try {
            return getAllS().stream()
                    .filter(s -> s.getUsername().equals(username))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            LOGGER.error("Error get SECURITY by username from JSON: {}", e.getMessage(), e);
            return null;
        }
    }

    public Map<String, Object> editSmod(Long id, String username, String password, String rePassword, String newRole) {
        Map<String, Object> response = Map.of("success", false);
        try {
            List<SeMod> sMods = getAllS();
            SeMod sMod = sMods.stream()
                    .filter(s -> s.getId().equals(id))
                    .findFirst()
                    .orElse(null);

            if (sMod != null) {
                // გასწორებული ლოგიკა + hashing
                if (password.equals(rePassword) && !password.isEmpty() && !rePassword.isEmpty()) {
                    sMod.setUsername(username);
                    sMod.setPassword(passwordEncoder.encode(password)); // hashed
                    sMod.setRole(newRole);
                    MAPPER.writeValue(UNITS_JSON, sMods);
                    response = Map.of("success", true);
                } else {
                    response = Map.of("success", false, "message", "Passwords do not match or are empty");
                }
            } else {
                response = Map.of("success", false, "message", "User not found");
            }
        } catch (Exception e) {
            LOGGER.error("Error edit SECURITY in JSON: {}", e.getMessage(), e);
            response = Map.of("success", false);
        }
        return response;
    }

    public void deleteSmod(Long id) {
        try {
            List<SeMod> sMods = getAllS();
            sMods.removeIf(s -> s.getId().equals(id));
            MAPPER.writeValue(UNITS_JSON, sMods);
            LOGGER.info(" delete SECURITY from JSON COMPLETE ");
        } catch (Exception e) {
            LOGGER.error("Error delete SECURITY from JSON: {}", e.getMessage(), e);
        }
    }

    public Map<String, Object> addSmod(String username, String password, String rePassword, String role) {
        Map<String, Object> response = Map.of("success", false);
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        boolean isUser = "USER".equalsIgnoreCase(role);
        boolean passwordsMatch = password.equals(rePassword) && !password.isEmpty() && !rePassword.isEmpty();
        boolean usernameExists = username != null && !username.isEmpty()
                && getAllS().stream().anyMatch(s -> s.getUsername().equalsIgnoreCase(username));

        try {
            if (isAdmin
                    && getAllS().stream().filter(s -> "ADMIN".equalsIgnoreCase(s.getRole())).count() >= MAX_ADMINS) {
                response = Map.of("success", false, "message", "Maximum number of admins reached");
                return response;
            }
            if (isUser && getAllS().stream().filter(s -> "USER".equalsIgnoreCase(s.getRole())).count() >= MAX_USERS) {
                response = Map.of("success", false, "message", "Maximum number of users reached");
                return response;
            }

            if (passwordsMatch && !usernameExists) {
                List<SeMod> sMods = getAllS();
                Long newId = sMods.stream()
                        .mapToLong(SeMod::getId)
                        .max()
                        .orElse(0L) + 1;

                SeMod newSmod = new SeMod();
                newSmod.setId(newId);
                newSmod.setUsername(username);
                newSmod.setPassword(passwordEncoder.encode(password)); // hashed
                newSmod.setRole(role);

                sMods.add(newSmod);
                MAPPER.writeValue(UNITS_JSON, sMods);
                response = Map.of("success", true);
            } else {
                response = Map.of("success", false);
            }
        } catch (Exception e) {
            LOGGER.error("Error add SECURITY to JSON: {}", e.getMessage(), e);
            response = Map.of("success", false);
        }
        return response;
    }
}