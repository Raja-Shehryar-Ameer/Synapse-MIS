package com.synapse.controller;

import com.synapse.model.Patient;
import com.synapse.model.SymptomLog;
import com.synapse.repository.PatientRepository;

/**
 * UC3: Log Symptoms — creates SymptomLog, checks for recurring trends.
 */
public class SymptomsController {

    private final PatientRepository patientRepo = new PatientRepository();

    /**
     * Logs a symptom for the given patient.
     * @return true if a trend was detected (3+ occurrences of same symptom)
     */
    public boolean logSymptom(Patient patient, String symptom, Integer severityLevel, String notes) {
        SymptomLog log = new SymptomLog(symptom, severityLevel, notes);
        patient.addSymptomLog(log);
        boolean trendDetected = patient.checkSymptomTrend(symptom);
        patientRepo.update(patient);
        return trendDetected;
    }

    public void deleteSymptomLog(Patient patient, java.util.UUID id) {
        patient.removeSymptomLog(id);
        patientRepo.update(patient);
    }
}
