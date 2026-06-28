package com.synapse.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Core domain entity — represents a registered patient/user of Synapse.
 * Information Expert: Patient holds references to all its logs, medicines, events, etc.
 */
@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender")
    private String gender;

    @Column(name = "height_cm")
    private Double heightCm;

    @Column(name = "current_bmi")
    private Double currentBMI;

    // --- Relationships ---

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "emergency_profile_id")
    private EmergencyProfile emergencyProfile;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private List<VitalLog> vitalLogs = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private List<SymptomLog> symptomLogs = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private List<WeightLog> weightLogs = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private List<DietLog> dietLogs = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private List<HydrationLog> hydrationLogs = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private List<JournalEntry> journalEntries = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private List<Medicine> medicines = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private List<CalendarEvent> calendarEvents = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private List<MedicalRecord> medicalRecords = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private List<HealthReport> healthReports = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private List<DataBackup> dataBackups = new ArrayList<>();

    // --- Constructors ---

    public Patient() {}

    public Patient(String fullName, String email, String passwordHash,
                   LocalDate dateOfBirth, String gender, Double heightCm) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.heightCm = heightCm;
    }

    // --- Domain Logic (Information Expert) ---

    public void setEmergencyProfile(EmergencyProfile profile) {
        this.emergencyProfile = profile;
    }

    public void updateDetails(Map<String, Object> data) {
        if (data.containsKey("fullName"))    this.fullName = (String) data.get("fullName");
        if (data.containsKey("email"))       this.email = (String) data.get("email");
        if (data.containsKey("gender"))      this.gender = (String) data.get("gender");
        if (data.containsKey("dateOfBirth")) this.dateOfBirth = (LocalDate) data.get("dateOfBirth");
        if (data.containsKey("heightCm")) {
            this.heightCm = (Double) data.get("heightCm");
            calculateBMI();
        }
    }

    public Double calculateBMI() {
        if (heightCm != null && heightCm > 0 && !weightLogs.isEmpty()) {
            double latestWeight = weightLogs.get(weightLogs.size() - 1).getWeightKg();
            double heightM = heightCm / 100.0;
            this.currentBMI = Math.round((latestWeight / (heightM * heightM)) * 100.0) / 100.0;
        }
        return this.currentBMI;
    }

    public void addVitalLog(VitalLog log)           { this.vitalLogs.add(log); }
    public void removeVitalLog(UUID id)             { this.vitalLogs.removeIf(l -> l.getLogId().equals(id)); }
    
    public void addSymptomLog(SymptomLog log)        { this.symptomLogs.add(log); }
    public void removeSymptomLog(UUID id)            { this.symptomLogs.removeIf(l -> l.getLogId().equals(id)); }
    
    public void addDietLog(DietLog log)              { this.dietLogs.add(log); }
    public void removeDietLog(UUID id)               { this.dietLogs.removeIf(l -> l.getLogId().equals(id)); }
    
    public void addHydrationLog(HydrationLog log)    { this.hydrationLogs.add(log); }
    
    public void addJournalEntry(JournalEntry entry)  { this.journalEntries.add(entry); }
    public void removeJournalEntry(UUID id)          { this.journalEntries.removeIf(e -> e.getEntryId().equals(id)); }
    
    public void addCalendarEvent(CalendarEvent event){ this.calendarEvents.add(event); }
    public void removeCalendarEvent(UUID id)         { this.calendarEvents.removeIf(e -> e.getEventId().equals(id)); }
    
    public void addMedicalRecord(MedicalRecord rec)  { this.medicalRecords.add(rec); }
    public void removeMedicalRecord(UUID id)         { this.medicalRecords.removeIf(r -> r.getRecordId().equals(id)); }
    public void addHealthReport(HealthReport report) { this.healthReports.add(report); }

    public void addWeightLog(WeightLog log) {
        this.weightLogs.add(log);
    }

    public Double updateWeightAndCalculateBMI(double weight) {
        if (heightCm != null && heightCm > 0) {
            double heightM = heightCm / 100.0;
            this.currentBMI = Math.round((weight / (heightM * heightM)) * 100.0) / 100.0;
        }
        return this.currentBMI;
    }

    public boolean checkSymptomTrend(String symptomName) {
        long count = symptomLogs.stream()
                .filter(s -> s.getSymptomName().equalsIgnoreCase(symptomName))
                .count();
        return count >= 3; // trend if 3+ occurrences
    }

    public List<Medicine> getMedicineInventory() {
        return this.medicines;
    }

    public boolean checkDrugInteractions(Medicine newMed) {
        // Simplified: check if any existing medicine has same name (duplicate check)
        return medicines.stream()
                .anyMatch(m -> m.getName().equalsIgnoreCase(newMed.getName()));
    }

    public void addMedicineToInventory(Medicine med) {
        this.medicines.add(med);
    }
    public void removeMedicine(UUID id) {
        this.medicines.removeIf(m -> m.getMedicineId().equals(id));
    }

    public List<CalendarEvent> getCalendarEvents() {
        return this.calendarEvents;
    }

    public boolean checkSchedulingConflict(LocalDate date, java.time.LocalTime time) {
        return calendarEvents.stream()
                .anyMatch(e -> e.getEventDate().equals(date) && e.getEventTime().equals(time));
    }

    public EmergencyProfile getEmergencyProfile() {
        return this.emergencyProfile;
    }

    public List<Medicine> getActiveMedicines() {
        return medicines.stream()
                .filter(m -> m.getSchedule() != null
                        && Boolean.TRUE.equals(m.getSchedule().getIsPrescribed())
                        && (m.getExpiryDate() == null || m.getExpiryDate().isAfter(LocalDate.now())))
                .toList();
    }

    // --- Getters and Setters ---

    public UUID getPatientId()          { return patientId; }
    public String getFullName()         { return fullName; }
    public void setFullName(String n)   { this.fullName = n; }
    public String getEmail()            { return email; }
    public void setEmail(String e)      { this.email = e; }
    public String getPasswordHash()     { return passwordHash; }
    public void setPasswordHash(String p){ this.passwordHash = p; }
    public LocalDate getDateOfBirth()   { return dateOfBirth; }
    public void setDateOfBirth(LocalDate d) { this.dateOfBirth = d; }
    public String getGender()           { return gender; }
    public void setGender(String g)     { this.gender = g; }
    public Double getHeightCm()         { return heightCm; }
    public void setHeightCm(Double h)   { this.heightCm = h; }
    public Double getCurrentBMI()       { return currentBMI; }
    public void setCurrentBMI(Double b) { this.currentBMI = b; }

    public List<VitalLog> getVitalLogs()           { return vitalLogs; }
    public List<SymptomLog> getSymptomLogs()       { return symptomLogs; }
    public List<WeightLog> getWeightLogs()         { return weightLogs; }
    public List<DietLog> getDietLogs()             { return dietLogs; }
    public List<HydrationLog> getHydrationLogs()   { return hydrationLogs; }
    public List<JournalEntry> getJournalEntries()  { return journalEntries; }
    public List<Medicine> getMedicines()            { return medicines; }
    public List<MedicalRecord> getMedicalRecords() { return medicalRecords; }
    public List<HealthReport> getHealthReports()   { return healthReports; }
    public List<DataBackup> getDataBackups()       { return dataBackups; }
}
