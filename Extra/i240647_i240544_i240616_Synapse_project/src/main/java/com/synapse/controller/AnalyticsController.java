package com.synapse.controller;

import com.synapse.model.Patient;
import com.synapse.service.AnalyticsEngine;
import java.util.Map;

/**
 * UC14: View Health Analytics.
 */
public class AnalyticsController {

    private final AnalyticsEngine engine = new AnalyticsEngine();

    public Map<String, Object> initialize(Patient patient) {
        return engine.generateDefaultReport(patient, "7days");
    }

    public Map<String, Object> updateAnalytics(Patient patient, String category, String timeRange) {
        return engine.generateFilteredReport(patient, category, timeRange);
    }
}
