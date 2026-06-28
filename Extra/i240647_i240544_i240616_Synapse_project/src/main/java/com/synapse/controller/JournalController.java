package com.synapse.controller;

import com.synapse.model.JournalEntry;
import com.synapse.model.Patient;
import com.synapse.repository.PatientRepository;

/**
 * UC5: Write Journal Entry — creates and saves JournalEntry.
 */
public class JournalController {

    private final PatientRepository patientRepo = new PatientRepository();

    public void saveJournalEntry(Patient patient, String content, String moodTag) {
        JournalEntry entry = new JournalEntry(content, moodTag);
        patient.addJournalEntry(entry);
        patientRepo.update(patient);
    }

    public void deleteJournalEntry(Patient patient, java.util.UUID id) {
        patient.removeJournalEntry(id);
        patientRepo.update(patient);
    }
}
