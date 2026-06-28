package com.synapse.controller;

import com.synapse.model.HealthcareFacility;
import com.synapse.service.FacilityDirectoryService;
import java.util.List;
import java.util.UUID;

/**
 * UC10: Browse Hospitals — fetches and filters healthcare facilities.
 */
public class HospitalsController {

    private final FacilityDirectoryService service = new FacilityDirectoryService();
    private List<HealthcareFacility> allFacilities;

    public List<HealthcareFacility> initialize() {
        allFacilities = service.fetchAllFacilities();
        return allFacilities;
    }

    public List<HealthcareFacility> filterFacilities(String location, String specialty, String services) {
        if (allFacilities == null) initialize();
        return allFacilities.stream()
                .filter(f -> (location == null || location.isBlank() ||
                        f.getAddress().toLowerCase().contains(location.toLowerCase())))
                .filter(f -> (specialty == null || specialty.isBlank() ||
                        f.getSpecialty().toLowerCase().contains(specialty.toLowerCase())))
                .filter(f -> (services == null || services.isBlank() ||
                        f.getServices().toLowerCase().contains(services.toLowerCase())))
                .toList();
    }

    public HealthcareFacility getFacilityDetails(UUID facilityId) {
        return service.getFacilityById(facilityId);
    }
}
