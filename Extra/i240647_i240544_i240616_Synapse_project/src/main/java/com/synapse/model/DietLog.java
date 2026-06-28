package com.synapse.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Records a daily diet log containing food items.
 * Information Expert: calculates its own total calories from contained food items.
 */
@Entity
@Table(name = "diet_logs")
public class DietLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "log_id")
    private UUID logId;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "total_calories")
    private Integer totalCalories;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "diet_log_id")
    private List<FoodItem> foodItems = new ArrayList<>();

    public DietLog() {}

    public DietLog(List<FoodItem> foodItems) {
        this.foodItems = foodItems;
        this.logDate = LocalDate.now();
        calculateTotalCalories();
    }

    /** Information Expert: sums calories from all food items. */
    public void calculateTotalCalories() {
        this.totalCalories = foodItems.stream()
                .mapToInt(FoodItem::getEstimatedCalories)
                .sum();
    }

    public void addFoodItem(FoodItem item) {
        this.foodItems.add(item);
        calculateTotalCalories();
    }

    // --- Getters and Setters ---
    public UUID getLogId()                    { return logId; }
    public LocalDate getLogDate()             { return logDate; }
    public void setLogDate(LocalDate d)       { this.logDate = d; }
    public Integer getTotalCalories()         { return totalCalories; }
    public void setTotalCalories(Integer c)   { this.totalCalories = c; }
    public List<FoodItem> getFoodItems()      { return foodItems; }
    public void setFoodItems(List<FoodItem> f){ this.foodItems = f; }
}
