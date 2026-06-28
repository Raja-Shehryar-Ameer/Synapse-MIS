package com.synapse.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "healthcare_facilities")
public class HealthcareFacility {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "facility_id")
    private UUID facilityId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "facility_type")
    private String facilityType;

    @Column(name = "address")
    private String address;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "specialty")
    private String specialty;

    @Column(name = "services", length = 1000)
    private String services;

    public HealthcareFacility() {}

    public HealthcareFacility(String name, String facilityType, String address,
                              String contactNumber, String specialty, String services) {
        this.name = name;
        this.facilityType = facilityType;
        this.address = address;
        this.contactNumber = contactNumber;
        this.specialty = specialty;
        this.services = services;
    }

    public UUID getFacilityId()                    { return facilityId; }
    public String getName()                        { return name; }
    public void setName(String n)                  { this.name = n; }
    public String getFacilityType()                { return facilityType; }
    public void setFacilityType(String t)          { this.facilityType = t; }
    public String getAddress()                     { return address; }
    public void setAddress(String a)               { this.address = a; }
    public String getContactNumber()               { return contactNumber; }
    public void setContactNumber(String c)         { this.contactNumber = c; }
    public String getSpecialty()                    { return specialty; }
    public void setSpecialty(String s)             { this.specialty = s; }
    public String getServices()                    { return services; }
    public void setServices(String s)              { this.services = s; }
}
