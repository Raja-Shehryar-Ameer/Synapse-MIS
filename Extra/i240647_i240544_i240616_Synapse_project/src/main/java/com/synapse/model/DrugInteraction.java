package com.synapse.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "drug_interactions")
public class DrugInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "interaction_id")
    private UUID interactionId;

    @Column(name = "drug_a", nullable = false)
    private String drugA;

    @Column(name = "drug_b", nullable = false)
    private String drugB;

    @Column(name = "severity_level")
    private String severityLevel;

    @Column(name = "warning_message", length = 1000)
    private String warningMessage;

    public DrugInteraction() {}

    public DrugInteraction(String drugA, String drugB, String severityLevel, String warningMessage) {
        this.drugA = drugA;
        this.drugB = drugB;
        this.severityLevel = severityLevel;
        this.warningMessage = warningMessage;
    }

    public UUID getInteractionId()                 { return interactionId; }
    public String getDrugA()                       { return drugA; }
    public void setDrugA(String a)                 { this.drugA = a; }
    public String getDrugB()                       { return drugB; }
    public void setDrugB(String b)                 { this.drugB = b; }
    public String getSeverityLevel()               { return severityLevel; }
    public void setSeverityLevel(String s)         { this.severityLevel = s; }
    public String getWarningMessage()              { return warningMessage; }
    public void setWarningMessage(String w)        { this.warningMessage = w; }
}
