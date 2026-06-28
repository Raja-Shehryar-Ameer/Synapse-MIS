package com.synapse.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "medical_records")
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "record_id")
    private UUID recordId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "category")
    private String category;

    @Column(name = "file_format")
    private String fileFormat;

    @Column(name = "file_size")
    private Double fileSize;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "upload_date")
    private LocalDate uploadDate;

    public MedicalRecord() {}

    public MedicalRecord(String title, String category, String filePath) {
        this.title = title;
        this.category = category;
        this.filePath = filePath;
        this.uploadDate = LocalDate.now();
        if (filePath != null && filePath.contains(".")) {
            this.fileFormat = filePath.substring(filePath.lastIndexOf('.') + 1);
        }
    }

    public UUID getRecordId()                  { return recordId; }
    public String getTitle()                   { return title; }
    public void setTitle(String t)             { this.title = t; }
    public String getCategory()                { return category; }
    public void setCategory(String c)          { this.category = c; }
    public String getFileFormat()              { return fileFormat; }
    public void setFileFormat(String f)        { this.fileFormat = f; }
    public Double getFileSize()                { return fileSize; }
    public void setFileSize(Double s)          { this.fileSize = s; }
    public String getFilePath()                { return filePath; }
    public void setFilePath(String p)          { this.filePath = p; }
    public LocalDate getUploadDate()           { return uploadDate; }
    public void setUploadDate(LocalDate d)     { this.uploadDate = d; }
}
