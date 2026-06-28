package com.synapse.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Records a single vital signs reading (blood pressure, heart rate, temperature).
 * Information Expert: validates its own data ranges and detects abnormalities.
 */
@Entity
@Table(name = "vital_logs")
public class VitalLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "log_id")
    private UUID logId;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "blood_pressure")
    private String bloodPressure; // e.g. "120/80"

    @Column(name = "heart_rate_bpm")
    private Integer heartRateBpm;

    @Column(name = "temperature_f")
    private Double temperatureF;

    @Column(name = "is_abnormal")
    private Boolean isAbnormal = false;

    public VitalLog() {}

    public VitalLog(String bloodPressure, Integer heartRateBpm, Double temperatureF) {
        this.bloodPressure = bloodPressure;
        this.heartRateBpm = heartRateBpm;
        this.temperatureF = temperatureF;
        this.timestamp = LocalDateTime.now();
    }

    /** Validates that readings are within plausible medical ranges. */
    public boolean validateRanges() {
        boolean valid = true;
        if (heartRateBpm != null && (heartRateBpm < 20 || heartRateBpm > 250)) valid = false;
        if (temperatureF != null && (temperatureF < 90.0 || temperatureF > 110.0)) valid = false;
        if (bloodPressure != null) {
            try {
                String[] parts = bloodPressure.split("/");
                int systolic = Integer.parseInt(parts[0].trim());
                int diastolic = Integer.parseInt(parts[1].trim());
                if (systolic < 50 || systolic > 300 || diastolic < 20 || diastolic > 200) valid = false;
            } catch (Exception e) {
                valid = false;
            }
        }
        return valid;
    }

    /** Evaluates readings against medical thresholds and sets the abnormal flag. */
    public void checkAbnormal() {
        this.isAbnormal = false;
        if (heartRateBpm != null && (heartRateBpm < 60 || heartRateBpm > 100)) this.isAbnormal = true;
        if (temperatureF != null && (temperatureF < 97.0 || temperatureF > 99.5)) this.isAbnormal = true;
        if (bloodPressure != null) {
            try {
                String[] parts = bloodPressure.split("/");
                int systolic = Integer.parseInt(parts[0].trim());
                int diastolic = Integer.parseInt(parts[1].trim());
                if (systolic > 140 || systolic < 90 || diastolic > 90 || diastolic < 60) this.isAbnormal = true;
            } catch (Exception ignored) {}
        }
    }

    // --- Getters and Setters ---
    public UUID getLogId()                 { return logId; }
    public LocalDateTime getTimestamp()    { return timestamp; }
    public void setTimestamp(LocalDateTime t) { this.timestamp = t; }
    public String getBloodPressure()       { return bloodPressure; }
    public void setBloodPressure(String b) { this.bloodPressure = b; }
    public Integer getHeartRateBpm()       { return heartRateBpm; }
    public void setHeartRateBpm(Integer h) { this.heartRateBpm = h; }
    public Double getTemperatureF()        { return temperatureF; }
    public void setTemperatureF(Double t)  { this.temperatureF = t; }
    public Boolean getIsAbnormal()         { return isAbnormal; }
    public void setIsAbnormal(Boolean a)   { this.isAbnormal = a; }
}
