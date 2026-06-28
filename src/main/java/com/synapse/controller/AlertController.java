package com.synapse.controller;

import com.synapse.model.SystemAlert;
import com.synapse.repository.GenericRepository;

import java.util.UUID;

/**
 * UC7: Send System Alert — triggers, acknowledges, and snoozes alerts.
 */
public class AlertController {

    private final GenericRepository repo = new GenericRepository();

    public SystemAlert triggerScheduledAlert(UUID alertId) {
        return repo.findAlertById(alertId);
    }

    public void acknowledgeAlert(UUID alertId) {
        SystemAlert alert = repo.findAlertById(alertId);
        if (alert != null) {
            alert.markAsAcknowledged();
            repo.update(alert);
        }
    }

    public void snoozeAlert(UUID alertId) {
        SystemAlert alert = repo.findAlertById(alertId);
        if (alert != null) {
            alert.calculateSnoozeTime();
            repo.update(alert);
        }
    }
}
