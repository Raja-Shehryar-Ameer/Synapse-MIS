package com.synapse.service;

import com.ironsoftware.ironpdf.PdfDocument;
import com.synapse.model.*;

import java.awt.Desktop;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Generates styled HTML → PDF documents via IronPDF.
 * Design: Light Minimal theme matching the Synapse UI.
 */
public class DocumentGenerator {

    private static final String OUTPUT_DIR = "synapse-data/exports/";
    private static final DateTimeFormatter TS  = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy");

    // ── Public API ────────────────────────────────────────────────

    /** Emergency SOS card — red-accented identity card layout */
    public String createEmergencyDocument(EmergencyProfile profile, List<Medicine> medicines) {
        new File(OUTPUT_DIR).mkdirs();
        String fileName  = "emergency_sos_" + System.currentTimeMillis() + ".pdf";
        String filePath  = OUTPUT_DIR + fileName;

        try {
            String html = buildEmergencyHtml(profile, medicines);
            PdfDocument pdf = PdfDocument.renderHtmlAsPdf(html);
            pdf.saveAs(filePath);
            openFileAsync(filePath);
            return filePath;
        } catch (Exception e) {
            System.err.println("[DocumentGenerator] Emergency PDF failed: " + e.getMessage());
            return null;
        }
    }

    /** Comprehensive health report — sectioned table layout */
    public String createPDFReport(Map<String, Object> reportData) {
        new File(OUTPUT_DIR).mkdirs();
        String fileName  = "health_report_" + System.currentTimeMillis() + ".pdf";
        String filePath  = OUTPUT_DIR + fileName;

        try {
            String html = buildReportHtml(reportData);
            PdfDocument pdf = PdfDocument.renderHtmlAsPdf(html);
            pdf.saveAs(filePath);
            openFileAsync(filePath);
            return filePath;
        } catch (Exception e) {
            System.err.println("[DocumentGenerator] Health report PDF failed: " + e.getMessage());
            return null;
        }
    }

    // ── HTML builders ─────────────────────────────────────────────

    private String buildEmergencyHtml(EmergencyProfile p, List<Medicine> meds) {
        String name  = p != null ? safe(p.getEmergencyContactName()) : "—";
        String phone = p != null ? safe(p.getEmergencyContactPhone()) : "—";
        String blood = p != null ? safe(p.getBloodType()) : "—";
        String allergy  = p != null ? safe(p.getAllergies()) : "None known";
        String chronic  = p != null ? safe(p.getChronicConditions()) : "None";

        StringBuilder medRows = new StringBuilder();
        if (meds != null && !meds.isEmpty()) {
            for (Medicine m : meds) {
                String exp   = m.getExpiryDate() != null ? m.getExpiryDate().format(DAY) : "—";
                String qty   = m.getInventoryQuantity() != null ? m.getInventoryQuantity().toString() : "—";
                String dose  = (m.getSchedule() != null && m.getSchedule().getDosageAmount() != null)
                        ? m.getSchedule().getDosageAmount() : "—";
                String freq  = (m.getSchedule() != null && m.getSchedule().getFrequency() != null)
                        ? m.getSchedule().getFrequency() : "—";
                medRows.append("<tr>")
                        .append("<td>").append(esc(m.getName())).append("</td>")
                        .append("<td>").append(esc(dose)).append("</td>")
                        .append("<td>").append(esc(freq)).append("</td>")
                        .append("<td>").append(qty).append("</td>")
                        .append("<td>").append(exp).append("</td>")
                        .append("</tr>");
            }
        } else {
            medRows.append("<tr><td colspan='5' class='empty'>No active medications on record</td></tr>");
        }

        return baseTemplate("Emergency Clinical Profile")
            + """
            <div class="doc-header">
              <div class="doc-title">EMERGENCY CLINICAL PROFILE</div>
              <div class="doc-sub">CONFIDENTIAL MEDICAL INFORMATION</div>
            </div>

            <div class="two-col">
              <div class="section-box">
                <div class="section-title">CRITICAL IDENTIFIERS</div>
                <div class="info-row"><span class="lbl">Blood Type:</span>
                  <span class="val blood">""" + esc(blood) + """
                </span></div>
                <div class="info-row"><span class="lbl">Known Allergies:</span><span class="val">""" + esc(allergy) + """
                </span></div>
                <div class="info-row"><span class="lbl">Chronic Conditions:</span><span class="val">""" + esc(chronic) + """
                </span></div>
              </div>
              <div class="section-box">
                <div class="section-title">EMERGENCY CONTACT</div>
                <div class="contact-name">""" + esc(name) + """
                </div>
                <div class="contact-phone">""" + esc(phone) + """
                </div>
                <div class="info-row" style="margin-top:16px"><span class="lbl">Generated:</span>
                  <span class="val">""" + LocalDateTime.now().format(TS) + """
                </span></div>
              </div>
            </div>

            <div class="section-box" style="margin-top:20px">
              <div class="section-title">ACTIVE MEDICATIONS</div>
              <table>
                <thead>
                  <tr>
                    <th>Medication Name</th><th>Dosage</th><th>Frequency</th><th>Stock</th><th>Expiry</th>
                  </tr>
                </thead>
                <tbody>
            """ + medRows + """
                </tbody>
              </table>
            </div>

            <div class="footer">
              Generated by Synapse Clinical Information System  |  """ + LocalDateTime.now().format(TS) + """
            </div>
            </body></html>
            """;
    }

    private String buildReportHtml(Map<String, Object> data) {
        // Extract header fields
        String patientName = data != null ? safe(data.get("Patient Name")) : "—";
        String dateRange   = data != null ? safe(data.get("Date Range"))   : "—";
        String categories  = data != null ? safe(data.get("Categories"))   : "—";

        StringBuilder sections = new StringBuilder();
        if (data != null) {
            String currentSection = null;
            boolean inTable = false;

            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String key = entry.getKey();
                String val = String.valueOf(entry.getValue());

                // Skip meta fields already in header
                if (key.equals("Patient Name") || key.equals("Date Range") || key.equals("Categories")) continue;

                if (key.startsWith("---")) {
                    if (inTable) { sections.append("</tbody></table></div>"); inTable = false; }
                    // Parse section title from "--- VITALS (3 readings) ---"
                    String sectionTitle = key.replace("-", "").trim();
                    String icon = sectionIcon(sectionTitle);
                    String color = sectionColor(sectionTitle);
                    sections.append("<div class='section-box' style='margin-top:20px'>")
                            .append("<div class='section-title'>")
                            .append(sectionTitle).append("</div>")
                            .append("<table><thead><tr><th>Log Entry</th><th>Data & Details</th></tr></thead><tbody>");
                    inTable = true;
                } else if (inTable) {
                    String displayKey = key.trim().replaceFirst("^  ", "");
                    // Highlight abnormal entries
                    String rowClass = val.contains("[ABNORMAL]") ? " class='abnormal'" : "";
                    String displayVal = esc(val).replace("[ABNORMAL]",
                            "<span class='badge red'>⚠ Abnormal</span>");
                    sections.append("<tr").append(rowClass).append(">")
                            .append("<td>").append(esc(displayKey)).append("</td>")
                            .append("<td>").append(displayVal).append("</td>")
                            .append("</tr>");
                }
            }
            if (inTable) sections.append("</tbody></table></div>");
        }

        return baseTemplate("Clinical Health Report")
            + """
            <div class="doc-header">
              <div class="doc-title">CLINICAL HEALTH REPORT</div>
              <div class="doc-sub">COMPREHENSIVE PATIENT DATA SUMMARY</div>
            </div>

            <div class="summary-grid">
              <div class="summary-card">
                <div class="summary-label">Patient Name</div>
                <div class="summary-value">""" + esc(patientName) + """
                </div>
              </div>
              <div class="summary-card">
                <div class="summary-label">Reporting Period</div>
                <div class="summary-value">""" + esc(dateRange) + """
                </div>
              </div>
              <div class="summary-card">
                <div class="summary-label">Included Modules</div>
                <div class="summary-value">""" + esc(categories) + """
                </div>
              </div>
            </div>

            """ + sections + """

            <div class="footer">
              Generated by Synapse Clinical Information System  |  """ + LocalDateTime.now().format(TS) + """
            </div>
            </body></html>
            """;
    }

    // ── Shared HTML/CSS base template ─────────────────────────────

    private String baseTemplate(String pageTitle) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <title>Synapse — """ + pageTitle + """
            </title>
              <style>
                @import url('https://fonts.googleapis.com/css2?family=Roboto:wght@400;500;700&display=swap');

                * { box-sizing: border-box; margin: 0; padding: 0; }

                body {
                  font-family: 'Roboto', Helvetica, Arial, sans-serif;
                  background: #ffffff;
                  color: #000000;
                  font-size: 11pt;
                  line-height: 1.5;
                  padding: 40px;
                }

                /* ── Brand / Header ── */
                .brand-bar {
                  display: flex;
                  align-items: center;
                  gap: 10px;
                  margin-bottom: 20px;
                  padding-bottom: 10px;
                  border-bottom: 2px solid #000000;
                }
                .brand-logo {
                  font-size: 24px; font-weight: 700; color: #000;
                  flex-shrink: 0;
                }
                .brand-name { font-size: 20px; font-weight: 700; color: #000; letter-spacing: 0.05em; text-transform: uppercase; }
                .brand-tag  { font-size: 10pt; color: #555; text-transform: uppercase; }

                /* ── Doc Header ── */
                .doc-header {
                  padding: 20px 0;
                  text-align: center;
                  border-bottom: 1px solid #ccc;
                  margin-bottom: 30px;
                }
                .doc-title {
                  font-size: 24pt; font-weight: 700; color: #000;
                  text-transform: uppercase; letter-spacing: 1px;
                }
                .doc-sub {
                  font-size: 11pt; color: #444; margin-top: 5px;
                  text-transform: uppercase; font-weight: 500;
                }

                /* ── Sections ── */
                .section-box {
                  margin-bottom: 30px;
                }
                .section-title {
                  font-size: 12pt; font-weight: 700;
                  color: #000;
                  border-bottom: 2px solid #000;
                  padding-bottom: 4px;
                  margin-bottom: 10px;
                  text-transform: uppercase;
                }

                /* ── Info rows (key-value) ── */
                .info-row {
                  display: flex; gap: 10px;
                  padding: 4px 0;
                }
                .lbl { font-weight: 700; color: #000; min-width: 150px; flex-shrink: 0; }
                .val { color: #222; }
                .blood {
                  font-size: 14pt; font-weight: 700;
                  color: #cc0000;
                }

                /* ── Two-column layout ── */
                .two-col { display: flex; gap: 40px; margin-bottom: 20px; }
                .two-col .section-box { flex: 1; margin-bottom: 0; }

                /* ── Contact block ── */
                .contact-name  { font-size: 14pt; font-weight: 700; color: #000; margin-bottom: 2px; }
                .contact-phone { font-size: 12pt; color: #333; margin-bottom: 10px; }

                /* ── Tables ── */
                table { width: 100%; border-collapse: collapse; margin-top: 5px; border: 1px solid #000; }
                th {
                  background: #f0f0f0; color: #000;
                  font-size: 10pt; font-weight: 700;
                  padding: 8px; text-align: left;
                  border: 1px solid #000;
                }
                td {
                  padding: 8px; color: #000;
                  border: 1px solid #000;
                  vertical-align: top;
                  font-size: 10pt;
                }
                tr.abnormal td { background: #ffe6e6; font-weight: 500; }
                td.empty { color: #555; text-align: center; padding: 20px; font-style: italic; }

                /* ── Badges ── */
                .badge { display: inline-block; padding: 2px 5px; font-size: 9pt; font-weight: 700; text-transform: uppercase; border: 1px solid #cc0000; }
                .badge.red { color: #cc0000; }

                /* ── Summary grid ── */
                .summary-grid { display: flex; border: 1px solid #000; margin-bottom: 30px; }
                .summary-card {
                  flex: 1; padding: 10px 15px; border-right: 1px solid #000;
                }
                .summary-card:last-child { border-right: none; }
                .summary-label { font-size: 9pt; font-weight: 700; color: #555; text-transform: uppercase; margin-bottom: 2px; }
                .summary-value { font-size: 11pt; font-weight: 700; color: #000; }

                /* ── Footer ── */
                .footer {
                  margin-top: 50px; padding-top: 10px;
                  border-top: 1px solid #000;
                  text-align: center; font-size: 9pt; color: #555;
                  text-transform: uppercase;
                }
              </style>
            </head>
            <body>
              <div class="brand-bar">
                <div class="brand-logo">SYNAPSE</div>
                <div>
                  <div class="brand-tag">Clinical Information Systems</div>
                </div>
              </div>
            """;
    }

    // ── Utilities ─────────────────────────────────────────────────

    private String sectionIcon(String title) {
        return ""; // Emojis removed for strict clinical reporting
    }

    private String sectionColor(String title) {
        return ""; // Colors removed for strict clinical reporting
    }

    private void openFileAsync(String filePath) {
        new Thread(() -> {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(new File(filePath));
                }
            } catch (Exception ignored) {}
        }, "pdf-open").start();
    }

    private String safe(Object val) {
        return val != null ? val.toString() : "N/A";
    }

    private String esc(String s) {
        if (s == null) return "N/A";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
