package com.synapse.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "health_reports")
public class HealthReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "report_id")
    private UUID reportId;

    @Column(name = "generation_date")
    private LocalDateTime generationDate;

    @Column(name = "date_range_start")
    private LocalDate dateRangeStart;

    @Column(name = "date_range_end")
    private LocalDate dateRangeEnd;

    @Column(name = "included_categories")
    private String includedCategories;

    @Column(name = "pdf_file_path")
    private String pdfFilePath;

    public HealthReport() {}

    public HealthReport(LocalDate dateRangeStart, LocalDate dateRangeEnd,
                        String includedCategories, String pdfFilePath) {
        this.generationDate = LocalDateTime.now();
        this.dateRangeStart = dateRangeStart;
        this.dateRangeEnd = dateRangeEnd;
        this.includedCategories = includedCategories;
        this.pdfFilePath = pdfFilePath;
    }

    public UUID getReportId()                          { return reportId; }
    public LocalDateTime getGenerationDate()           { return generationDate; }
    public void setGenerationDate(LocalDateTime d)     { this.generationDate = d; }
    public LocalDate getDateRangeStart()               { return dateRangeStart; }
    public void setDateRangeStart(LocalDate d)         { this.dateRangeStart = d; }
    public LocalDate getDateRangeEnd()                 { return dateRangeEnd; }
    public void setDateRangeEnd(LocalDate d)           { this.dateRangeEnd = d; }
    public String getIncludedCategories()              { return includedCategories; }
    public void setIncludedCategories(String c)        { this.includedCategories = c; }
    public String getPdfFilePath()                     { return pdfFilePath; }
    public void setPdfFilePath(String p)               { this.pdfFilePath = p; }
}
