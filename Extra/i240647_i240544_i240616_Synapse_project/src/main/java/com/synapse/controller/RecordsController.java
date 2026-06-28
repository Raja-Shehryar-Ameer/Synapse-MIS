package com.synapse.controller;

import com.synapse.model.MedicalRecord;
import com.synapse.model.Patient;
import com.synapse.repository.PatientRepository;
import com.synapse.service.FileStorageService;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Organize Medical Records.
 */
public class RecordsController {

    private final PatientRepository patientRepo = new PatientRepository();
    private final FileStorageService fileService = new FileStorageService();

    public static final String[] CATEGORIES = {
            "Lab Results", "Prescriptions", "Imaging / X-Ray",
            "Discharge Summary", "Insurance", "Other"
    };

    public MedicalRecord saveMedicalRecord(Patient patient, String title, String category, File file) {
        String storedPath = fileService.saveFileToStorage(file);
        MedicalRecord record = new MedicalRecord(title, category, storedPath);
        if (file != null) {
            record.setFileSize((double) file.length());
        }
        patient.addMedicalRecord(record);
        patientRepo.update(patient);
        return record;
    }

    public List<MedicalRecord> getMedicalRecords(Patient patient) {
        if (patient == null || patient.getMedicalRecords() == null) {
            return Collections.emptyList();
        }
        return patient.getMedicalRecords();
    }

    public void deleteMedicalRecord(Patient patient, MedicalRecord record, boolean deleteStoredFile) {
        if (patient == null || record == null) {
            return;
        }

        UUID recordId = record.getRecordId();
        String path = record.getFilePath();
        patient.removeMedicalRecord(recordId);
        patientRepo.update(patient);

        if (deleteStoredFile && path != null && !path.isBlank()) {
            fileService.deleteStoredFile(path);
        }
    }
}
