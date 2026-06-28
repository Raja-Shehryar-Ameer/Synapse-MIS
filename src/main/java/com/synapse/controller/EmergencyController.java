package com.synapse.controller;

import com.synapse.model.EmergencyProfile;
import com.synapse.model.Medicine;
import com.synapse.model.Patient;
import com.synapse.repository.GenericRepository;
import com.synapse.service.DocumentGenerator;
import java.util.List;

/**
 * UC11: Trigger Emergency Protocol.
 */
public class EmergencyController {

    private final GenericRepository repo = new GenericRepository();
    private final DocumentGenerator docGen = new DocumentGenerator();

    private EmergencyProfile cachedProfile;
    private List<Medicine> cachedMedicines;
    private boolean isComplete;

    public boolean initiateEmergencyProtocol(Patient patient) {
        cachedProfile = patient.getEmergencyProfile();
        cachedMedicines = patient.getActiveMedicines();
        isComplete = cachedProfile != null && cachedProfile.checkCompleteness();
        return isComplete;
    }

    public EmergencyProfile getCachedProfile()       { return cachedProfile; }
    public List<Medicine> getCachedMedicines()        { return cachedMedicines; }
    public boolean isProfileComplete()               { return isComplete; }

    public String generateEmergencyExport() {
        String path = docGen.createEmergencyDocument(cachedProfile, cachedMedicines);
        repo.logEvent("Emergency Protocol Activated");
        return path;
    }
}
