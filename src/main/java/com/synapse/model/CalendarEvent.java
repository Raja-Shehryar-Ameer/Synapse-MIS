package com.synapse.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "calendar_events")
public class CalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "event_time")
    private LocalTime eventTime;

    @Column(name = "reminder_preferences")
    private String reminderPreferences;

    public CalendarEvent() {}

    public CalendarEvent(String title, String eventType, LocalDate eventDate,
                         LocalTime eventTime, String reminderPreferences) {
        this.title = title;
        this.eventType = eventType;
        this.eventDate = eventDate;
        this.eventTime = eventTime;
        this.reminderPreferences = reminderPreferences;
    }

    public UUID getEventId()                          { return eventId; }
    public String getTitle()                          { return title; }
    public void setTitle(String t)                    { this.title = t; }
    public String getEventType()                      { return eventType; }
    public void setEventType(String t)                { this.eventType = t; }
    public LocalDate getEventDate()                   { return eventDate; }
    public void setEventDate(LocalDate d)             { this.eventDate = d; }
    public LocalTime getEventTime()                   { return eventTime; }
    public void setEventTime(LocalTime t)             { this.eventTime = t; }
    public String getReminderPreferences()            { return reminderPreferences; }
    public void setReminderPreferences(String r)      { this.reminderPreferences = r; }
}
