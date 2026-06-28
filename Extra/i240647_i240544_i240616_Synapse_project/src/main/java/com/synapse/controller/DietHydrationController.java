package com.synapse.controller;

import com.synapse.model.*;
import com.synapse.repository.PatientRepository;
import com.synapse.service.NotificationScheduler;
import java.util.List;

/**
 * UC4: Track Diet, BMI and Hydration.
 */
public class DietHydrationController {

    private final PatientRepository patientRepo = new PatientRepository();
    private final NotificationScheduler scheduler = new NotificationScheduler();

    public DietLog logDiet(Patient patient, List<FoodItem> foodItems) {
        DietLog log = new DietLog(foodItems);
        patient.addDietLog(log);
        patientRepo.update(patient);
        return log;
    }

    public HydrationLog logHydration(Patient patient, Integer waterGoal, Integer waterIntake) {
        HydrationLog log = new HydrationLog(waterGoal, waterIntake);
        patient.addHydrationLog(log);
        patientRepo.update(patient);
        scheduler.scheduleHydrationReminders(waterGoal, waterIntake);
        return log;
    }

    public Double updateWeight(Patient patient, Double weight) {
        Double newBMI = patient.updateWeightAndCalculateBMI(weight);
        WeightLog wLog = new WeightLog(weight, newBMI);
        patient.addWeightLog(wLog);
        patientRepo.update(patient);
        return newBMI;
    }

    public void deleteDietLog(Patient patient, java.util.UUID id) {
        patient.removeDietLog(id);
        patientRepo.update(patient);
    }
}
