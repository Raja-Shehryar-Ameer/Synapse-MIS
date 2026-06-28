package com.synapse.controller;

import com.synapse.model.HealthReport;
import com.synapse.model.Patient;
import com.synapse.repository.PatientRepository;
import com.synapse.service.DocumentGenerator;
import com.synapse.service.ReportEngine;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * UC15: Generate Health Report.
 */
public class ReportController {

    private final PatientRepository patientRepo = new PatientRepository();
    private final ReportEngine reportEngine = new ReportEngine();
    private final DocumentGenerator docGen = new DocumentGenerator();

    private Map<String, Object> cachedPreview;

    public Map<String, Object> generatePreview(Patient patient, LocalDate start, LocalDate end, List<String> categories) {
        cachedPreview = reportEngine.compileReportPreview(patient, start, end, categories);
        return cachedPreview;
    }

    public String exportReport(Patient patient, LocalDate start, LocalDate end, List<String> categories) {
        if (cachedPreview == null) {
            cachedPreview = reportEngine.compileReportPreview(patient, start, end, categories);
        }
        String pdfPath = docGen.createPDFReport(cachedPreview);
        String cats = String.join(", ", categories);
        HealthReport report = new HealthReport(start, end, cats, pdfPath);
        patient.addHealthReport(report);
        patientRepo.update(patient);
        cachedPreview = null;
        return pdfPath;
    }
}
