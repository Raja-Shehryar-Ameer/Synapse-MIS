package com.synapse.service;

import com.synapse.model.HealthcareFacility;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pure Fabrication: Loads and serves healthcare facility data from a mock JSON file.
 */
public class FacilityDirectoryService {

    private List<HealthcareFacility> facilities;

    public List<HealthcareFacility> fetchAllFacilities() {
        if (facilities == null) {
            try {
                InputStream is = getClass().getResourceAsStream("/data/hospitals.json");
                if (is != null) {
                    Type listType = new TypeToken<List<HealthcareFacility>>() {}.getType();
                    facilities = new Gson().fromJson(new InputStreamReader(is), listType);
                }
            } catch (Exception e) {
                System.err.println("Could not load hospitals: " + e.getMessage());
            }
            if (facilities == null) facilities = new ArrayList<>();
        }
        return facilities;
    }

    public HealthcareFacility getFacilityById(UUID facilityId) {
        fetchAllFacilities();
        return facilities.stream()
                .filter(f -> f.getFacilityId() != null && f.getFacilityId().equals(facilityId))
                .findFirst().orElse(null);
    }
}
