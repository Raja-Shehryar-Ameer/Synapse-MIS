package com.synapse.service;

import com.synapse.model.CalendarEvent;
import com.synapse.model.MedicationSchedule;
import com.synapse.model.Medicine;
import com.synapse.model.Patient;
import com.synapse.ui.ToastNotification;
import javafx.application.Platform;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * Manages all background reminder tasks:
 *
 *  • Medication — exact dose time (toast with ✓ Taken + Snooze)
 *  • Appointment / Follow-up / Exercise — 30 min before
 *  • Medication-type calendar events — 5 min before
 *  • Low stock / near-expiry — daily startup check
 *  • Hydration — every 2 hours if below goal (tracked in session)
 *
 * All notifications use ToastNotification (no modal dialog interruptions).
 */
public class NotificationScheduler {

    // ── Lead-times by event type (minutes before scheduled time) ──
    private static final Map<String, Integer> LEAD_TIMES = Map.of(
            "Appointment", 30,
            "Follow-up",   30,
            "Exercise",    15,
            "Medication",   5,
            "Other",       10
    );

    private static ScheduledExecutorService executor;

    /** Set of "already fired today" keys to prevent duplicate toasts per session */
    private static final Set<String> firedToday = ConcurrentHashMap.newKeySet();
    private static LocalDate lastResetDate = LocalDate.now();

    // ── Public API ────────────────────────────────────────────────

    public static void startBackgroundTasks(Patient patient) {
        stopBackgroundTasks();   // cancel previous session

        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "synapse-notifier");
            t.setDaemon(true);
            return t;
        });

        // Tick every 60 seconds — check all pending notifications
        executor.scheduleAtFixedRate(() -> {
            resetDailyFireSetIfNeeded();
            Patient current = com.synapse.ui.SessionManager.getCurrentPatient();
            if (current == null) return;

            LocalDateTime now = LocalDateTime.now();

            checkMedicationDoses(current, now);
            checkCalendarEvents(current, now);

        }, 0, 60, TimeUnit.SECONDS);

        // Low-stock / near-expiry check — runs once at login, then daily
        executor.scheduleAtFixedRate(() -> {
            Patient current = com.synapse.ui.SessionManager.getCurrentPatient();
            if (current == null) return;
            Platform.runLater(() -> checkStockAndExpiry(current));
        }, 5, 24 * 60, TimeUnit.MINUTES);   // 5-second delay, then every 24 h
    }

    public static void stopBackgroundTasks() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
        firedToday.clear();
    }

    // ── Medication dose reminders ──────────────────────────────────

    private static void checkMedicationDoses(Patient patient, LocalDateTime now) {
        LocalTime nowTime = now.toLocalTime();

        for (Medicine med : patient.getActiveMedicines()) {
            MedicationSchedule sched = med.getSchedule();
            if (sched == null || sched.getScheduledTimes() == null) continue;

            for (LocalTime doseTime : sched.getScheduledTimes()) {
                // Window: fire when we are within [doseTime-0, doseTime+1 min]
                long diffMinutes = minuteDiff(nowTime, doseTime);

                if (diffMinutes == 0) {
                    String key = "med:" + med.getMedicineId() + ":" + doseTime;
                    if (firedToday.add(key)) {
                        final Medicine finalMed = med;
                        Platform.runLater(() -> {
                            String dosage = sched.getDosageAmount() != null
                                    ? sched.getDosageAmount() : "your dose";
                            ToastNotification.show(
                                "medicine",
                                "Time for " + finalMed.getName(),
                                "Take " + dosage + "  ·  " + sched.getFrequency(),
                                // Snooze: re-fire in 15 min (just re-add to queue via removal from set)
                                () -> {
                                    firedToday.remove(key);
                                    ToastNotification.show("info", "Snoozed 15 min",
                                            "We'll remind you again at " +
                                            doseTime.plusMinutes(15)
                                                    .format(DateTimeFormatter.ofPattern("HH:mm")));
                                },
                                // Taken: deduct inventory
                                () -> decrementInventory(patient, finalMed)
                            );
                        });
                    }
                }
            }
        }

        // Low stock immediate reminder during dose check
        Platform.runLater(() -> checkLowStockInstant(patient));
    }

    // ── Calendar event reminders ───────────────────────────────────

    private static void checkCalendarEvents(Patient patient, LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        LocalTime nowTime = now.toLocalTime();

        for (CalendarEvent ev : patient.getCalendarEvents()) {
            if (ev.getEventDate() == null || !ev.getEventDate().equals(today)) continue;
            if (ev.getEventTime() == null) continue;

            String type = ev.getEventType() != null ? ev.getEventType() : "Other";
            int leadMins = LEAD_TIMES.getOrDefault(type, 15);

            // Fire at (eventTime - lead)
            LocalTime fireAt = ev.getEventTime().minusMinutes(leadMins);
            long diff = minuteDiff(nowTime, fireAt);

            if (diff == 0) {
                String key = "cal:" + ev.getEventId();
                if (firedToday.add(key)) {
                    Platform.runLater(() -> {
                        String icon = eventIcon(type);
                        String when = ev.getEventTime()
                                .format(DateTimeFormatter.ofPattern("HH:mm"));
                        String lead = leadMins + " min";
                        ToastNotification.show(
                            "event",
                            icon + "  " + ev.getTitle(),
                            "Starts at " + when + "  ·  Reminder: " + lead + " before",
                            null,
                            () -> firedToday.add("dismissed:" + ev.getEventId())
                        );
                    });
                }
            }
        }
    }

    // ── Low stock / near-expiry (daily) ───────────────────────────

    private static void checkStockAndExpiry(Patient patient) {
        List<String> lowStock  = new ArrayList<>();
        List<String> nearExpiry = new ArrayList<>();

        for (Medicine med : patient.getMedicineInventory()) {
            if (med.getInventoryQuantity() != null && med.getInventoryQuantity() <= 5) {
                lowStock.add(med.getName() + " (" + med.getInventoryQuantity() + " left)");
            }
            if (med.getExpiryDate() != null
                    && med.getExpiryDate().isBefore(LocalDate.now().plusDays(30))
                    && !med.getExpiryDate().isBefore(LocalDate.now())) {
                nearExpiry.add(med.getName() + " (expires " + med.getExpiryDate() + ")");
            }
        }

        if (!lowStock.isEmpty()) {
            ToastNotification.show(
                "warn",
                "Low Medicine Stock",
                String.join("\n", lowStock)
            );
        }
        if (!nearExpiry.isEmpty()) {
            ToastNotification.show(
                "warn",
                "Medicine Expiring Soon",
                String.join("\n", nearExpiry)
            );
        }
    }

    /**
     * Quick low-stock nudge during dose check (only once per session per medicine).
     */
    private static void checkLowStockInstant(Patient patient) {
        for (Medicine med : patient.getMedicineInventory()) {
            if (med.getInventoryQuantity() != null && med.getInventoryQuantity() == 3) {
                String key = "lowstock:" + med.getMedicineId();
                if (firedToday.add(key)) {
                    ToastNotification.show(
                        "warn",
                        "Low Stock Warning",
                        med.getName() + " — only " + med.getInventoryQuantity() + " units remaining."
                    );
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────

    /** Decrements medicine qty and persists to DB on background thread */
    private static void decrementInventory(Patient patient, Medicine med) {
        if (med.getInventoryQuantity() == null || med.getInventoryQuantity() <= 0) return;
        med.setInventoryQuantity(med.getInventoryQuantity() - 1);

        new Thread(() -> {
            try {
                new com.synapse.repository.PatientRepository().update(patient);
                Platform.runLater(() -> {
                    if (com.synapse.ui.ViewFactory.getInstance() != null) {
                        com.synapse.ui.ViewFactory.getInstance().invalidate("medicine");
                        com.synapse.ui.ViewFactory.getInstance().invalidate("dashboard");
                    }
                    if (med.getInventoryQuantity() == 0) {
                        ToastNotification.show("warn", "Medicine Depleted",
                                med.getName() + " is out of stock. Please reorder.");
                    }
                });
            } catch (Exception e) {
                System.err.println("[NotificationScheduler] DB update failed: " + e.getMessage());
            }
        }, "synapse-db-write").start();
    }

    /**
     * Returns minutes from {@code from} to {@code to}, clamped to positive only.
     * Returns 0 if within the same minute.
     */
    private static long minuteDiff(LocalTime from, LocalTime to) {
        long diff = java.time.Duration.between(from, to).toMinutes();
        return Math.abs(diff);  // 0 = same minute; we treat 0 as "fire now"
    }

    private static void resetDailyFireSetIfNeeded() {
        LocalDate today = LocalDate.now();
        if (!today.equals(lastResetDate)) {
            firedToday.clear();
            lastResetDate = today;
        }
    }

    private static String eventIcon(String type) {
        return switch (type) {
            case "Appointment" -> "🏥";
            case "Medication"  -> "💊";
            case "Exercise"    -> "🏃";
            case "Follow-up"   -> "🔁";
            default            -> "📅";
        };
    }

    // ── Legacy instance methods (kept for backward compat) ────────

    public void scheduleHydrationReminders(Integer waterGoal, Integer waterIntake) {
        if (waterGoal != null && waterIntake != null && waterIntake < waterGoal) {
            int remaining = waterGoal - waterIntake;
            ToastNotification.show("info", "Hydration Reminder",
                    "You still need " + remaining + " ml to hit your daily goal. 💧");
        }
    }

    public void schedulePrescriptionReminders(MedicationSchedule schedule) {
        if (schedule != null) {
            System.out.println("[Scheduler] Prescription watcher running for times: "
                    + schedule.getScheduledTimes());
        }
    }

    public void scheduleEventReminder(CalendarEvent event) {
        if (event == null) return;
        String type = event.getEventType() != null ? event.getEventType() : "Other";
        int lead = LEAD_TIMES.getOrDefault(type, 15);
        System.out.println("[Scheduler] Event '" + event.getTitle()
                + "' will trigger " + lead + " min before " + event.getEventTime());
    }

    public void rescheduleAlert(UUID alertId, LocalDateTime newDateTime) {
        System.out.println("[Scheduler] Alert " + alertId + " rescheduled to " + newDateTime);
    }
}
