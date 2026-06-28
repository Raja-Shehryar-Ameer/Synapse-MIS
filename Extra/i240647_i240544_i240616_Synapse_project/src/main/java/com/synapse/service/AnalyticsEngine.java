package com.synapse.service;

import com.synapse.model.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Pure Fabrication: Aggregates health data for analytics dashboards.
 */
public class AnalyticsEngine {

    public Map<String, Object> generateDefaultReport(Patient patient, String timeRange) {
        return generateFilteredReport(patient, "all", timeRange);
    }

    public Map<String, Object> generateFilteredReport(Patient patient, String category, String timeRange) {
        Map<String, Object> report = new LinkedHashMap<>();
        LocalDate cutoff = getCutoffDate(timeRange);

        if ("all".equals(category) || "vitals".equals(category)) {
            List<VitalLog> vitals = patient.getVitalLogs().stream()
                    .filter(v -> v.getTimestamp().toLocalDate().isAfter(cutoff))
                    .collect(Collectors.toList());
            report.put("vitalCount", vitals.size());
            report.put("vitals", vitals);
            if (!vitals.isEmpty()) {
                double avgHR = vitals.stream().filter(v -> v.getHeartRateBpm() != null)
                        .mapToInt(VitalLog::getHeartRateBpm).average().orElse(0);
                report.put("avgHeartRate", Math.round(avgHR));
            }
        }

        if ("all".equals(category) || "symptoms".equals(category)) {
            List<SymptomLog> symptoms = patient.getSymptomLogs().stream()
                    .filter(s -> s.getTimestamp().toLocalDate().isAfter(cutoff))
                    .collect(Collectors.toList());
            report.put("symptomCount", symptoms.size());
            report.put("symptoms", symptoms);
        }

        if ("all".equals(category) || "diet".equals(category)) {
            List<DietLog> diets = patient.getDietLogs().stream()
                    .filter(d -> d.getLogDate().isAfter(cutoff))
                    .collect(Collectors.toList());
            report.put("dietCount", diets.size());
            if (!diets.isEmpty()) {
                double avgCal = diets.stream().filter(d -> d.getTotalCalories() != null)
                        .mapToInt(DietLog::getTotalCalories).average().orElse(0);
                report.put("avgCalories", Math.round(avgCal));
            }
        }

        if ("all".equals(category) || "hydration".equals(category)) {
            List<HydrationLog> hydration = patient.getHydrationLogs().stream()
                    .filter(h -> h.getLogDate().isAfter(cutoff))
                    .collect(Collectors.toList());
            report.put("hydrationCount", hydration.size());
        }

        report.put("bmi", patient.getCurrentBMI());
        return report;
    }

    private LocalDate getCutoffDate(String timeRange) {
        return switch (timeRange) {
            case "30days" -> LocalDate.now().minusDays(30);
            case "90days" -> LocalDate.now().minusDays(90);
            case "1year"  -> LocalDate.now().minusYears(1);
            default       -> LocalDate.now().minusDays(7); // 7days default
        };
    }
}
