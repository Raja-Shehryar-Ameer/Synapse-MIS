package com.synapse.controller;

import com.synapse.model.MedicalRecord;
import com.synapse.model.Patient;
import com.synapse.repository.PatientRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecordsControllerTest {

    private RecordsController recordsController;
    private AccountController accountController;
    private PatientRepository repository;
    private File tempFile;
    private String testEmail;

    @BeforeEach
    void setUp() throws IOException {
        recordsController = new RecordsController();
        accountController = new AccountController();
        repository = new PatientRepository();
        
        tempFile = File.createTempFile("test_medical_record", ".txt");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("Sample content for medical record test.");
        }
        
        testEmail = "recordstest" + System.currentTimeMillis() + "@test.com";
        accountController.registerPatient("Test Patient", testEmail, "pass123", LocalDate.of(1990, 1, 1), "Male", 175.0, "O+", "None", "None", "Contact", "123");
    }

    @AfterEach
    void tearDown() {
        if (tempFile.exists()) {
            tempFile.delete();
        }
        Patient p = accountController.getCurrentPatient();
        if (p != null) {
            repository.delete(p);
        }
    }

    @Test
    void testSaveAndGetMedicalRecord() {
        Patient patient = accountController.getCurrentPatient();
        assertNotNull(patient);

        MedicalRecord record = recordsController.saveMedicalRecord(patient, "Blood Test", "Lab Results", tempFile);
        assertNotNull(record);
        assertNotNull(record.getRecordId());
        assertEquals("Blood Test", record.getTitle());

        List<MedicalRecord> records = recordsController.getMedicalRecords(patient);
        assertEquals(1, records.size());
        assertEquals("Blood Test", records.get(0).getTitle());
    }

    @Test
    void testDeleteMedicalRecord() {
        Patient patient = accountController.getCurrentPatient();
        MedicalRecord record = recordsController.saveMedicalRecord(patient, "X-Ray", "Imaging / X-Ray", tempFile);
        
        List<MedicalRecord> recordsBefore = recordsController.getMedicalRecords(patient);
        assertEquals(1, recordsBefore.size());

        recordsController.deleteMedicalRecord(patient, record, true);

        List<MedicalRecord> recordsAfter = recordsController.getMedicalRecords(patient);
        assertEquals(0, recordsAfter.size());
    }
}
