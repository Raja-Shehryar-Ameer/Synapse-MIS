package com.synapse.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.synapse.model.Patient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Handles backup serialization and restore.
 */
public class BackupService {

    private final FileStorageService fileService = new FileStorageService();

    /** Wrapper for serialized backup data. Class (not record) for Gson compatibility. */
    private static class BackupSnapshot {
        LocalDateTime createdAt;
        Patient patient;
        BackupSnapshot() {}
        BackupSnapshot(LocalDateTime createdAt, Patient patient) {
            this.createdAt = createdAt;
            this.patient = patient;
        }
        Patient patient() { return patient; }
    }

    private Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDate.class,
                        (JsonSerializer<LocalDate>) (src, t, ctx) -> ctx.serialize(src.toString()))
                .registerTypeAdapter(LocalDate.class,
                        (JsonDeserializer<LocalDate>) (json, t, ctx) -> LocalDate.parse(json.getAsString()))
                .registerTypeAdapter(LocalDateTime.class,
                        (JsonSerializer<LocalDateTime>) (src, t, ctx) -> ctx.serialize(src.toString()))
                .registerTypeAdapter(LocalDateTime.class,
                        (JsonDeserializer<LocalDateTime>) (json, t, ctx) -> LocalDateTime.parse(json.getAsString()))
                .registerTypeAdapter(LocalTime.class,
                        (JsonSerializer<LocalTime>) (src, t, ctx) -> ctx.serialize(src.toString()))
                .registerTypeAdapter(LocalTime.class,
                        (JsonDeserializer<LocalTime>) (json, t, ctx) -> LocalTime.parse(json.getAsString()))
                .registerTypeAdapter(java.util.UUID.class,
                        (JsonSerializer<java.util.UUID>) (src, t, ctx) -> ctx.serialize(src.toString()))
                .registerTypeAdapter(java.util.UUID.class,
                        (JsonDeserializer<java.util.UUID>) (json, t, ctx) -> java.util.UUID.fromString(json.getAsString()))
                .setPrettyPrinting()
                .create();
    }

    public Map<String, Object> generateBackup(Patient patient) {
        Gson gson = createGson();
        String json = gson.toJson(new BackupSnapshot(LocalDateTime.now(), patient));
        byte[] compressed = compress(json.getBytes(StandardCharsets.UTF_8));
        return fileService.saveBackupFile(compressed);
    }

    /**
     * Deserializes backup file and returns the restored Patient object.
     * Returns null if the backup belongs to a different account or is invalid.
     */
    public Patient deserializeBackup(Patient targetPatient, File backupFile) {
        try {
            byte[] compressed = fileService.loadBackupFile(backupFile);
            if (compressed == null) {
                System.err.println("[Restore] Could not read backup file.");
                return null;
            }

            byte[] decompressed = decompress(compressed);
            String json = new String(decompressed, StandardCharsets.UTF_8);
            BackupSnapshot snapshot = createGson().fromJson(json, BackupSnapshot.class);
            if (snapshot == null) {
                System.err.println("[Restore] Backup snapshot deserialized as null.");
                return null;
            }
            if (snapshot.patient() == null) {
                System.err.println("[Restore] Backup contains no patient data.");
                return null;
            }

            String backupEmail = snapshot.patient().getEmail();
            String currentEmail = targetPatient.getEmail();
            System.out.println("[Restore] Backup email: " + backupEmail + " | Current email: " + currentEmail);

            if (backupEmail == null || !currentEmail.equalsIgnoreCase(backupEmail)) {
                System.err.println("[Restore] Email mismatch — backup belongs to a different account.");
                return null;
            }

            return snapshot.patient();
        } catch (Exception e) {
            System.err.println("[Restore] Exception: " + e.getClass().getSimpleName() + " — " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @deprecated Use deserializeBackup + PatientRepository.restorePatient instead
     */
    public boolean restoreIntoPatient(Patient targetPatient, File backupFile) {
        Patient restored = deserializeBackup(targetPatient, backupFile);
        if (restored == null) return false;
        copyRestoredState(targetPatient, restored);
        return true;
    }

    private void copyRestoredState(Patient target, Patient restored) {
        target.setFullName(restored.getFullName());
        target.setDateOfBirth(restored.getDateOfBirth());
        target.setGender(restored.getGender());
        target.setHeightCm(restored.getHeightCm());
        target.setCurrentBMI(restored.getCurrentBMI());
        target.setEmergencyProfile(restored.getEmergencyProfile());

        replaceList(target.getVitalLogs(), restored.getVitalLogs());
        replaceList(target.getSymptomLogs(), restored.getSymptomLogs());
        replaceList(target.getWeightLogs(), restored.getWeightLogs());
        replaceList(target.getDietLogs(), restored.getDietLogs());
        replaceList(target.getHydrationLogs(), restored.getHydrationLogs());
        replaceList(target.getJournalEntries(), restored.getJournalEntries());
        replaceList(target.getMedicines(), restored.getMedicines());
        replaceList(target.getCalendarEvents(), restored.getCalendarEvents());
        replaceList(target.getMedicalRecords(), restored.getMedicalRecords());
        replaceList(target.getHealthReports(), restored.getHealthReports());
        replaceList(target.getDataBackups(), restored.getDataBackups());
    }

    private <T> void replaceList(java.util.List<T> target, java.util.List<T> restored) {
        target.clear();
        if (restored != null) {
            target.addAll(restored);
        }
    }

    private byte[] compress(byte[] data) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(data);
            gzip.finish();
            return bos.toByteArray();
        } catch (IOException e) {
            return data;
        }
    }

    private byte[] decompress(byte[] data) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             GZIPInputStream gzip = new GZIPInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = gzip.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            return data;
        }
    }
}
