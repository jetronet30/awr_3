package com.jaba.awr_3.servermanager.backuprecovery;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Comparator;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jaba.awr_3.inits.repo.RepoInit;


@Service
public class BackupRecovery {

    public String getBackupName() {
        if (!RepoInit.BACKUP_REPO_MVC.exists() || !RepoInit.BACKUP_REPO_MVC.isDirectory()) {
            return null;
        }
        Collection<File> tarFiles = FileUtils.listFiles(RepoInit.BACKUP_REPO_MVC, new String[] { "tar.gz" }, false);
        if (tarFiles.isEmpty()) {
            return null;
        }
        File latest = tarFiles.stream()
                .max(Comparator.comparingLong(File::lastModified))
                .orElse(null);

        return latest != null ? latest.getName() : null;
    }


    public String getBackupDataName() {
        if (!RepoInit.BACKUP_REPO_MVC.exists() || !RepoInit.BACKUP_REPO_MVC.isDirectory()) {
            return null;
        }
        Collection<File> tarFiles = FileUtils.listFiles(RepoInit.BACKUP_REPO_MVC, new String[] { ".sql" }, false);
        if (tarFiles.isEmpty()) {
            return null;
        }
        File latest = tarFiles.stream()
                .max(Comparator.comparingLong(File::lastModified))
                .orElse(null);

        return latest != null ? latest.getName() : null;
    }

    public void uploadBackup(MultipartFile upladFile) {

        if (upladFile == null || upladFile.isEmpty())
            return;
        try {
            FileUtils.cleanDirectory(RepoInit.BACKUP_UPLOAD_REPO);
            Files.createDirectories(Paths.get(RepoInit.BACKUP_UPLOAD_REPO.toURI()));
            File outputFile = new File(RepoInit.BACKUP_UPLOAD_REPO, upladFile.getOriginalFilename());
            if (outputFile.exists())
                return;
            try (InputStream in = upladFile.getInputStream();
                    OutputStream out = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
