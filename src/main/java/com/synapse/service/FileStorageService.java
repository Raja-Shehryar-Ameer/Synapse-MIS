package com.synapse.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Pure Fabrication: Handles physical file I/O for medical records and backups.
 */
public class FileStorageService {

    private static final String STORAGE_DIR = "synapse-data/records/";

    public String saveFileToStorage(File file) {
        if (file == null || !file.exists()) return null;
        new File(STORAGE_DIR).mkdirs();
        String dest = STORAGE_DIR + System.currentTimeMillis() + "_" + file.getName();
        try {
            Files.copy(file.toPath(), Paths.get(dest), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("File storage failed: " + e.getMessage());
            return null;
        }
        return dest;
    }

    public byte[] loadBackupFile(File file) {
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            System.err.println("Backup load failed: " + e.getMessage());
            return null;
        }
    }

    public boolean deleteStoredFile(String path) {
        if (path == null || path.isBlank()) return false;
        try {
            return Files.deleteIfExists(Path.of(path));
        } catch (IOException e) {
            System.err.println("File delete failed: " + e.getMessage());
            return false;
        }
    }

    public java.util.Map<String, Object> saveBackupFile(byte[] data) {
        new File("synapse-data/backups/").mkdirs();
        String path = "synapse-data/backups/backup_" + System.currentTimeMillis() + ".syn";
        try {
            Files.write(Paths.get(path), data);
        } catch (IOException e) {
            System.err.println("Backup save failed: " + e.getMessage());
            return null;
        }
        return java.util.Map.of("filePath", path, "fileSize", (double) data.length);
    }
}
