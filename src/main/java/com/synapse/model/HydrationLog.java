package com.synapse.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "hydration_logs")
public class HydrationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "log_id")
    private UUID logId;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "water_consumed_ml")
    private Integer waterConsumedMl;

    @Column(name = "daily_goal_ml")
    private Integer dailyGoalMl;

    public HydrationLog() {}

    public HydrationLog(Integer dailyGoalMl, Integer waterConsumedMl) {
        this.dailyGoalMl = dailyGoalMl;
        this.waterConsumedMl = waterConsumedMl;
        this.logDate = LocalDate.now();
    }

    /** Information Expert: evaluates progress toward daily water goal. */
    public double checkGoalProgress() {
        if (dailyGoalMl == null || dailyGoalMl == 0) return 0;
        return (double) waterConsumedMl / dailyGoalMl * 100.0;
    }

    public UUID getLogId()                        { return logId; }
    public LocalDate getLogDate()                 { return logDate; }
    public void setLogDate(LocalDate d)           { this.logDate = d; }
    public Integer getWaterConsumedMl()           { return waterConsumedMl; }
    public void setWaterConsumedMl(Integer w)     { this.waterConsumedMl = w; }
    public Integer getDailyGoalMl()               { return dailyGoalMl; }
    public void setDailyGoalMl(Integer g)         { this.dailyGoalMl = g; }
}
