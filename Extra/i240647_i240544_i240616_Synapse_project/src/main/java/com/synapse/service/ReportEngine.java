package com.synapse.service;

import com.synapse.model.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Pure Fabrication: Compiles structured report previews from raw patient data.
 */
public class ReportEngine {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public Map<String, Object> compileReportPreview(Patient patient, LocalDate start, LocalDate end, List<String> categories) {
        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("Patient Name", patient.getFullName());
        preview.put("Date Range", start + " to " + end);
        preview.put("Categories", String.join(", ", categories));

        for (String cat : categories) {
            switch (cat.toLowerCase()) {
                case "vitals" -> {
                    List<VitalLog> logs = patient.getVitalLogs().stream()
                            .filter(v -> isInRange(v.getTimestamp().toLocalDate(), start, end))
                            .sorted(Comparator.comparing(VitalLog::getTimestamp))
                            .collect(Collectors.toList());
                    preview.put("--- VITALS (" + logs.size() + " readings) ---", "");
                    for (int i = 0; i < logs.size(); i++) {
                        VitalLog v = logs.get(i);
                        String abnormal = Boolean.TRUE.equals(v.getIsAbnormal()) ? " [ABNORMAL]" : "";
                        preview.put("  Vital #" + (i + 1) + " (" + v.getTimestamp().format(DT_FMT) + ")",
                                "BP: " + v.getBloodPressure() + ", HR: " + v.getHeartRateBpm() + " bpm, Temp: " + v.getTemperatureF() + "°F" + abnormal);
                    }
                    if (logs.isEmpty()) preview.put("  Vitals", "No readings in this period");
                }
                case "symptoms" -> {
                    List<SymptomLog> logs = patient.getSymptomLogs().stream()
                            .filter(s -> isInRange(s.getTimestamp().toLocalDate(), start, end))
                            .sorted(Comparator.comparing(SymptomLog::getTimestamp))
                            .collect(Collectors.toList());
                    preview.put("--- SYMPTOMS (" + logs.size() + " entries) ---", "");
                    for (int i = 0; i < logs.size(); i++) {
                        SymptomLog s = logs.get(i);
                        String notes = (s.getNotes() != null && !s.getNotes().isBlank()) ? " — " + s.getNotes() : "";
                        preview.put("  Symptom #" + (i + 1) + " (" + s.getTimestamp().format(DT_FMT) + ")",
                                s.getSymptomName() + " | Severity: " + s.getSeverityLevel() + "/10" + notes);
                    }
                    if (logs.isEmpty()) preview.put("  Symptoms", "No entries in this period");
                }
                case "diet" -> {
                    List<DietLog> logs = patient.getDietLogs().stream()
                            .filter(d -> isInRange(d.getLogDate(), start, end))
                            .sorted(Comparator.comparing(DietLog::getLogDate))
                            .collect(Collectors.toList());
                    preview.put("--- DIET (" + logs.size() + " days logged) ---", "");
                    for (int i = 0; i < logs.size(); i++) {
                        DietLog d = logs.get(i);
                        StringBuilder items = new StringBuilder();
                        items.append(d.getTotalCalories()).append(" cal total");
                        if (d.getFoodItems() != null) {
                            for (FoodItem fi : d.getFoodItems()) {
                                items.append(" | ").append(fi.getName()).append(" (").append(fi.getEstimatedCalories()).append(" cal)");
                            }
                        }
                        preview.put("  Diet #" + (i + 1) + " (" + d.getLogDate() + ")", items.toString());
                    }
                    if (logs.isEmpty()) preview.put("  Diet", "No logs in this period");
                }
                case "hydration" -> {
                    List<HydrationLog> logs = patient.getHydrationLogs().stream()
                            .filter(h -> isInRange(h.getLogDate(), start, end))
                            .sorted(Comparator.comparing(HydrationLog::getLogDate))
                            .collect(Collectors.toList());
                    preview.put("--- HYDRATION (" + logs.size() + " days logged) ---", "");
                    for (int i = 0; i < logs.size(); i++) {
                        HydrationLog h = logs.get(i);
                        double pct = h.checkGoalProgress();
                        preview.put("  Hydration #" + (i + 1) + " (" + h.getLogDate() + ")",
                                h.getWaterConsumedMl() + " ml / " + h.getDailyGoalMl() + " ml goal (" + String.format("%.0f%%", pct) + ")");
                    }
                    if (logs.isEmpty()) preview.put("  Hydration", "No logs in this period");
                }
                case "weight" -> {
                    preview.put("--- WEIGHT & BMI ---", "");
                    preview.put("  Current BMI", patient.getCurrentBMI() != null ? patient.getCurrentBMI() : "N/A");
                    List<WeightLog> wLogs = patient.getWeightLogs().stream()
                            .filter(w -> isInRange(w.getTimestamp().toLocalDate(), start, end))
                            .sorted(Comparator.comparing(WeightLog::getTimestamp))
                            .collect(Collectors.toList());
                    for (int i = 0; i < wLogs.size(); i++) {
                        WeightLog w = wLogs.get(i);
                        preview.put("  Weight #" + (i + 1) + " (" + w.getTimestamp().format(DT_FMT) + ")",
                                w.getWeightKg() + " kg (BMI: " + w.getCalculatedBMI() + ")");
                    }
                }
            }
        }
        return preview;
    }

    private boolean isInRange(LocalDate date, LocalDate start, LocalDate end) {
        return !date.isBefore(start) && !date.isAfter(end);
    }
}
