package com.synapse.model;

import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "medication_schedules")
public class MedicationSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "schedule_id")
    private UUID scheduleId;

    @Column(name = "dosage_amount")
    private String dosageAmount;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "medication_times", joinColumns = @JoinColumn(name = "schedule_id"))
    @Column(name = "scheduled_time")
    private java.util.List<LocalTime> scheduledTimes = new java.util.ArrayList<>();

    @Column(name = "frequency")
    private String frequency;

    @Column(name = "is_prescribed")
    private Boolean isPrescribed = false;

    public MedicationSchedule() {}

    public MedicationSchedule(String dosageAmount, java.util.List<LocalTime> scheduledTimes, String frequency) {
        this.dosageAmount = dosageAmount;
        this.scheduledTimes = scheduledTimes;
        this.frequency = frequency;
        this.isPrescribed = true;
    }

    public UUID getScheduleId()                    { return scheduleId; }
    public String getDosageAmount()                { return dosageAmount; }
    public void setDosageAmount(String d)          { this.dosageAmount = d; }
    public java.util.List<LocalTime> getScheduledTimes() { return scheduledTimes; }
    public void setScheduledTimes(java.util.List<LocalTime> t) { this.scheduledTimes = t; }
    public String getFrequency()                   { return frequency; }
    public void setFrequency(String f)             { this.frequency = f; }
    public Boolean getIsPrescribed()               { return isPrescribed; }
    public void setIsPrescribed(Boolean p)         { this.isPrescribed = p; }
}
