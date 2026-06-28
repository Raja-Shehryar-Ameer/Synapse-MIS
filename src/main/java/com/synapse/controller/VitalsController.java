package com.synapse.controller;

import com.synapse.model.Patient;
import com.synapse.model.VitalLog;
import com.synapse.repository.PatientRepository;

/**
 * UC2: Log Vitals — creates VitalLog, validates ranges, checks abnormality.
 */
public class VitalsController {

    private final PatientRepository patientRepo = new PatientRepository();

    /**
     * Logs vital readings for the given patient.
     * @return the created VitalLog (caller can check isAbnormal flag)
     */
    public VitalLog logVitals(Patient patient, String bloodPressure, Integer heartRate, Double temperature) {
        VitalLog log = new VitalLog(bloodPressure, heartRate, temperature);

        if (!log.validateRanges()) {
            return null; // invalid ranges
        }

        log.checkAbnormal();
        patient.addVitalLog(log);
        patientRepo.update(patient);
        return log;
    }

    public void deleteVitalLog(Patient patient, java.util.UUID id) {
        patient.removeVitalLog(id);
        patientRepo.update(patient);
    }
}
