package com.synapse.model;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Emergency medical profile containing critical health information.
 * Information Expert: checks its own data completeness.
 */
@Entity
@Table(name = "emergency_profiles")
public class EmergencyProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "profile_id")
    private UUID profileId;

    @Column(name = "blood_type")
    private String bloodType;

    @Column(name = "allergies", length = 1000)
    private String allergies;

    @Column(name = "chronic_conditions", length = 1000)
    private String chronicConditions;

    @Column(name = "emergency_contact_name")
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone")
    private String emergencyContactPhone;

    public EmergencyProfile() {}

    public EmergencyProfile(String bloodType, String allergies, String chronicConditions,
                            String emergencyContactName, String emergencyContactPhone) {
        this.bloodType = bloodType;
        this.allergies = allergies;
        this.chronicConditions = chronicConditions;
        this.emergencyContactName = emergencyContactName;
        this.emergencyContactPhone = emergencyContactPhone;
    }

    /** Information Expert: checks if all critical fields are filled. */
    public boolean checkCompleteness() {
        return bloodType != null && !bloodType.isBlank()
                && emergencyContactName != null && !emergencyContactName.isBlank()
                && emergencyContactPhone != null && !emergencyContactPhone.isBlank();
    }

    // --- Getters and Setters ---
    public UUID getProfileId()                    { return profileId; }
    public String getBloodType()                  { return bloodType; }
    public void setBloodType(String b)            { this.bloodType = b; }
    public String getAllergies()                   { return allergies; }
    public void setAllergies(String a)            { this.allergies = a; }
    public String getChronicConditions()          { return chronicConditions; }
    public void setChronicConditions(String c)    { this.chronicConditions = c; }
    public String getEmergencyContactName()       { return emergencyContactName; }
    public void setEmergencyContactName(String n) { this.emergencyContactName = n; }
    public String getEmergencyContactPhone()      { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String p){ this.emergencyContactPhone = p; }
}
