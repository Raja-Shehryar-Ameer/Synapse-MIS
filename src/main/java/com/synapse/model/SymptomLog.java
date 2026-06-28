package com.synapse.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Records a single symptom entry with severity and optional notes.
 */
@Entity
@Table(name = "symptom_logs")
public class SymptomLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "log_id")
    private UUID logId;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "symptom_name", nullable = false)
    private String symptomName;

    @Column(name = "severity_level")
    private Integer severityLevel; // 1-10

    @Column(name = "notes", length = 2000)
    private String notes;

    public SymptomLog() {}

    public SymptomLog(String symptomName, Integer severityLevel, String notes) {
        this.symptomName = symptomName;
        this.severityLevel = severityLevel;
        this.notes = notes;
        this.timestamp = LocalDateTime.now();
    }

    // --- Getters and Setters ---
    public UUID getLogId()                      { return logId; }
    public LocalDateTime getTimestamp()          { return timestamp; }
    public void setTimestamp(LocalDateTime t)    { this.timestamp = t; }
    public String getSymptomName()              { return symptomName; }
    public void setSymptomName(String s)        { this.symptomName = s; }
    public Integer getSeverityLevel()           { return severityLevel; }
    public void setSeverityLevel(Integer s)     { this.severityLevel = s; }
    public String getNotes()                    { return notes; }
    public void setNotes(String n)              { this.notes = n; }
}
