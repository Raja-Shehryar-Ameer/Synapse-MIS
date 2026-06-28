package com.synapse.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "system_alerts")
public class SystemAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "alert_id")
    private UUID alertId;

    @Column(name = "alert_type")
    private String alertType;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "scheduled_date_time")
    private LocalDateTime scheduledDateTime;

    @Column(name = "is_acknowledged")
    private Boolean isAcknowledged = false;

    public SystemAlert() {}

    public SystemAlert(String alertType, String message, LocalDateTime scheduledDateTime) {
        this.alertType = alertType;
        this.message = message;
        this.scheduledDateTime = scheduledDateTime;
    }

    public void markAsAcknowledged() {
        this.isAcknowledged = true;
    }

    public LocalDateTime calculateSnoozeTime() {
        this.scheduledDateTime = LocalDateTime.now().plusMinutes(15);
        return this.scheduledDateTime;
    }

    public UUID getAlertId()                             { return alertId; }
    public String getAlertType()                         { return alertType; }
    public void setAlertType(String t)                   { this.alertType = t; }
    public String getMessage()                           { return message; }
    public void setMessage(String m)                     { this.message = m; }
    public LocalDateTime getScheduledDateTime()          { return scheduledDateTime; }
    public void setScheduledDateTime(LocalDateTime d)    { this.scheduledDateTime = d; }
    public Boolean getIsAcknowledged()                   { return isAcknowledged; }
    public void setIsAcknowledged(Boolean a)             { this.isAcknowledged = a; }
}
