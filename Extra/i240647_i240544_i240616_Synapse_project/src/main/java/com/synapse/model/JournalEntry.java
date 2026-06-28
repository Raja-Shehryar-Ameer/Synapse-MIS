package com.synapse.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "journal_entries")
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "entry_id")
    private UUID entryId;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "content_text", length = 5000)
    private String contentText;

    @Column(name = "mood_tag")
    private String moodTag;

    public JournalEntry() {}

    public JournalEntry(String contentText, String moodTag) {
        this.contentText = contentText;
        this.moodTag = moodTag;
        this.timestamp = LocalDateTime.now();
    }

    public UUID getEntryId()                    { return entryId; }
    public LocalDateTime getTimestamp()          { return timestamp; }
    public void setTimestamp(LocalDateTime t)    { this.timestamp = t; }
    public String getContentText()              { return contentText; }
    public void setContentText(String c)        { this.contentText = c; }
    public String getMoodTag()                  { return moodTag; }
    public void setMoodTag(String m)            { this.moodTag = m; }
}
