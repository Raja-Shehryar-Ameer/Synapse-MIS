package com.synapse.controller;

import com.synapse.model.DataBackup;
import com.synapse.model.Patient;
import com.synapse.repository.PatientRepository;
import com.synapse.service.BackupService;
import java.io.File;
import java.util.Map;

/**
 * UC13: Backup and Restore Data.
 */
public class BackupRestoreController {

    private final PatientRepository patientRepo = new PatientRepository();
    private final BackupService backupService = new BackupService();

    public String initiateBackup(Patient patient) {
        Map<String, Object> fileDetails = backupService.generateBackup(patient);
        if (fileDetails == null) {
            return null;
        }
        String path = (String) fileDetails.get("filePath");
        Double size = (Double) fileDetails.get("fileSize");

        DataBackup backup = new DataBackup(path, size, "SUCCESS");
        patient.getDataBackups().add(backup);
        patientRepo.update(patient);
        return path;
    }

    public boolean initiateRestore(Patient patient, File backupFile) {
        if (patient == null || backupFile == null) {
            return false;
        }
        Patient restored = backupService.deserializeBackup(patient, backupFile);
        if (restored == null) {
            return false;
        }
        try {
            patientRepo.restorePatient(patient, restored);
            return true;
        } catch (Exception e) {
            System.err.println("Restore persistence failed: " + e.getMessage());
            return false;
        }
    }
}
