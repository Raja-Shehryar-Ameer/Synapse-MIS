package com.synapse.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "data_backups")
public class DataBackup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "backup_id")
    private UUID backupId;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_size")
    private Double fileSize;

    @Column(name = "status")
    private String status;

    public DataBackup() {}

    public DataBackup(String filePath, Double fileSize, String status) {
        this.creationDate = LocalDateTime.now();
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.status = status;
    }

    public UUID getBackupId()                      { return backupId; }
    public LocalDateTime getCreationDate()         { return creationDate; }
    public void setCreationDate(LocalDateTime d)   { this.creationDate = d; }
    public String getFilePath()                    { return filePath; }
    public void setFilePath(String p)              { this.filePath = p; }
    public Double getFileSize()                    { return fileSize; }
    public void setFileSize(Double s)              { this.fileSize = s; }
    public String getStatus()                      { return status; }
    public void setStatus(String s)                { this.status = s; }
}
