package com.synapse.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Records a weight measurement and the BMI calculated at that point.
 */
@Entity
@Table(name = "weight_logs")
public class WeightLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "log_id")
    private UUID logId;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "weight_kg", nullable = false)
    private Double weightKg;

    @Column(name = "calculated_bmi")
    private Double calculatedBMI;

    public WeightLog() {}

    public WeightLog(Double weightKg, Double calculatedBMI) {
        this.weightKg = weightKg;
        this.calculatedBMI = calculatedBMI;
        this.timestamp = LocalDateTime.now();
    }

    // --- Getters and Setters ---
    public UUID getLogId()                      { return logId; }
    public LocalDateTime getTimestamp()          { return timestamp; }
    public void setTimestamp(LocalDateTime t)    { this.timestamp = t; }
    public Double getWeightKg()                 { return weightKg; }
    public void setWeightKg(Double w)           { this.weightKg = w; }
    public Double getCalculatedBMI()            { return calculatedBMI; }
    public void setCalculatedBMI(Double b)      { this.calculatedBMI = b; }
}
