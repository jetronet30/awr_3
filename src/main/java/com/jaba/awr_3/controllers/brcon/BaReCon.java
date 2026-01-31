package com.jaba.awr_3.controllers.brcon;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.jaba.awr_3.inits.repo.RepoInit;
import com.jaba.awr_3.servermanager.backuprecovery.Backup;
import com.jaba.awr_3.servermanager.backuprecovery.BackupRecovery;
import com.jaba.awr_3.servermanager.backuprecovery.Recovery;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequiredArgsConstructor
public class BaReCon {
    private final BackupRecovery backupRecovery;

    @PostMapping("/backuprecovery")
    public String postBacRec(Model m) {
        m.addAttribute("backupName", backupRecovery.getBackupName());
        m.addAttribute("backupDataName", backupRecovery.getBackupDataName());
        return "settings/backuprecovery";
    }

    @PostMapping("/createbeckup")
    public String createBuckup(Model m) {
        Backup.backup();
        m.addAttribute("backupName", backupRecovery.getBackupName());
        m.addAttribute("backupDataName", backupRecovery.getBackupDataName());
        return "settings/backuprecovery";
    }

    // --- ჩამოტვირთვის კონტროლერი ---
    @GetMapping("/backup/{filename:.+}")
    public ResponseEntity<Resource> download(@PathVariable String filename) throws IOException {
        Path filePath = Paths.get(RepoInit.BACKUP_REPO_MVC.getAbsolutePath()).resolve(filename).normalize();
        File file = filePath.toFile();
        if (!file.exists() || !file.isFile()) {
            System.out.println("File not found!");
            return ResponseEntity.notFound().build();
        }
        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(filePath));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PostMapping("/uploadbackup")
    public String uploadVideo(@RequestParam("backup") MultipartFile file, Model m) {
        backupRecovery.uploadBackup(file);
        m.addAttribute("backupName", backupRecovery.getBackupName());
        m.addAttribute("backupDataName", backupRecovery.getBackupDataName());
        Recovery.recovery();
        return "settings/backuprecovery";
    }
}
