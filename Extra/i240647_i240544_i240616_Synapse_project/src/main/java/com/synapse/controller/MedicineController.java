package com.synapse.controller;

import com.synapse.model.*;
import com.synapse.repository.PatientRepository;
import com.synapse.service.InteractionEngine;
import com.synapse.service.NotificationScheduler;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * UC6: Manage Medicine Inventory & Prescriptions.
 * UC8: Check Drug Interaction.
 */
public class MedicineController {

    private final PatientRepository patientRepo = new PatientRepository();
    private final NotificationScheduler scheduler = new NotificationScheduler();
    private final InteractionEngine interactionEngine = new InteractionEngine();

    public Map<String, Boolean> saveMedicine(Patient patient, String name, Integer quantity, LocalDate expiryDate) {
        Medicine med = new Medicine(name, quantity, expiryDate);
        Map<String, Boolean> statusFlags = med.checkLowStockOrExpiry();
        patient.addMedicineToInventory(med);
        patientRepo.update(patient);
        return statusFlags;
    }

    public void setPrescriptionSchedule(Patient patient, Medicine med, String dosage, List<LocalTime> times, String frequency) {
        MedicationSchedule schedule = new MedicationSchedule(dosage, times, frequency);
        med.setSchedule(schedule);
        patientRepo.update(patient);
        scheduler.schedulePrescriptionReminders(schedule);
    }

    /** UC8: Evaluates drug interactions before adding a medicine. */
    public List<DrugInteraction> initiateMedicineAddition(Patient patient, String name, Integer qty, LocalDate expiry) {
        Medicine newMed = new Medicine(name, qty, expiry);
        List<Medicine> inventory = patient.getMedicineInventory();
        return interactionEngine.evaluateInteractions(newMed, inventory);
    }

    /** UC8: Patient decides to proceed or cancel after seeing interactions. */
    public void processDecision(Patient patient, String decisionType, Medicine med) {
        if ("PROCEED".equalsIgnoreCase(decisionType)) {
            patient.addMedicineToInventory(med);
            patientRepo.update(patient);
        }
        // CANCEL: do nothing, med is garbage collected
    }

    public void deleteMedicine(Patient patient, java.util.UUID id) {
        patient.removeMedicine(id);
        patientRepo.update(patient);
    }

    public void removePrescriptionSchedule(Patient patient, Medicine med) {
        med.setSchedule(null);
        patientRepo.update(patient);
    }
}
