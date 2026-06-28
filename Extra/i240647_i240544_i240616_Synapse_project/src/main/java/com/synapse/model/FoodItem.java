package com.synapse.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "food_items")
public class FoodItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "item_id")
    private UUID itemId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "portion_size")
    private String portionSize;

    @Column(name = "estimated_calories")
    private Integer estimatedCalories;

    public FoodItem() {}

    public FoodItem(String name, String portionSize, Integer estimatedCalories) {
        this.name = name;
        this.portionSize = portionSize;
        this.estimatedCalories = estimatedCalories;
    }

    public UUID getItemId()                        { return itemId; }
    public String getName()                        { return name; }
    public void setName(String n)                  { this.name = n; }
    public String getPortionSize()                 { return portionSize; }
    public void setPortionSize(String p)           { this.portionSize = p; }
    public Integer getEstimatedCalories()          { return estimatedCalories; }
    public void setEstimatedCalories(Integer c)    { this.estimatedCalories = c; }
}
