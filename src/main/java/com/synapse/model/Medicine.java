package com.synapse.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "medicines")
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "medicine_id")
    private UUID medicineId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "inventory_quantity")
    private Integer inventoryQuantity;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "schedule_id")
    private MedicationSchedule schedule;

    public Medicine() {}

    public Medicine(String name, Integer inventoryQuantity, LocalDate expiryDate) {
        this.name = name;
        this.inventoryQuantity = inventoryQuantity;
        this.expiryDate = expiryDate;
    }

    /** Information Expert: checks stock level and expiry proximity. */
    public Map<String, Boolean> checkLowStockOrExpiry() {
        Map<String, Boolean> flags = new HashMap<>();
        flags.put("isLowStock", inventoryQuantity != null && inventoryQuantity <= 5);
        flags.put("isNearExpiry", expiryDate != null && expiryDate.isBefore(LocalDate.now().plusDays(30)));
        return flags;
    }

    public void setSchedule(MedicationSchedule schedule) {
        this.schedule = schedule;
    }

    public UUID getMedicineId()                      { return medicineId; }
    public String getName()                          { return name; }
    public void setName(String n)                    { this.name = n; }
    public Integer getInventoryQuantity()            { return inventoryQuantity; }
    public void setInventoryQuantity(Integer q)      { this.inventoryQuantity = q; }
    public LocalDate getExpiryDate()                 { return expiryDate; }
    public void setExpiryDate(LocalDate d)           { this.expiryDate = d; }
    public MedicationSchedule getSchedule()          { return schedule; }
}
