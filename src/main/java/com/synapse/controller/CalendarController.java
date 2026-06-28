package com.synapse.controller;

import com.synapse.model.CalendarEvent;
import com.synapse.model.Patient;
import com.synapse.repository.PatientRepository;
import com.synapse.service.NotificationScheduler;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * UC9: Track Calendar — event creation with conflict detection.
 */
public class CalendarController {

    private final PatientRepository patientRepo = new PatientRepository();
    private final NotificationScheduler scheduler = new NotificationScheduler();

    public boolean checkConflict(Patient patient, LocalDate date, LocalTime time) {
        return patient.checkSchedulingConflict(date, time);
    }

    public CalendarEvent addEvent(Patient patient, String title, String eventType,
                                   LocalDate date, LocalTime time, String reminderPrefs) {
        if (patient.checkSchedulingConflict(date, time)) {
            return null; // conflict
        }
        CalendarEvent event = new CalendarEvent(title, eventType, date, time, reminderPrefs);
        patient.addCalendarEvent(event);
        patientRepo.update(patient);
        scheduler.scheduleEventReminder(event);
        return event;
    }

    public void deleteCalendarEvent(Patient patient, java.util.UUID id) {
        patient.removeCalendarEvent(id);
        patientRepo.update(patient);
    }
}
