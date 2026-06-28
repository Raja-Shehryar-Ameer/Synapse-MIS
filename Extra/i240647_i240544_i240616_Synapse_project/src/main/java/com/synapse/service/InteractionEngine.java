package com.synapse.service;

import com.synapse.model.DrugInteraction;
import com.synapse.model.Medicine;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure Fabrication: Evaluates drug interactions against a local reference database.
 */
public class InteractionEngine {

    private List<DrugInteraction> knownInteractions;

    public InteractionEngine() {
        loadInteractionDatabase();
    }

    private void loadInteractionDatabase() {
        try {
            InputStream is = getClass().getResourceAsStream("/data/drug_interactions.json");
            if (is != null) {
                Type listType = new TypeToken<List<DrugInteraction>>() {}.getType();
                knownInteractions = new Gson().fromJson(new InputStreamReader(is), listType);
            }
        } catch (Exception e) {
            System.err.println("Could not load drug interactions: " + e.getMessage());
        }
        if (knownInteractions == null) knownInteractions = new ArrayList<>();
    }

    public List<DrugInteraction> evaluateInteractions(Medicine newMed, List<Medicine> inventory) {
        List<DrugInteraction> results = new ArrayList<>();
        String newName = newMed.getName().toLowerCase();

        for (Medicine existing : inventory) {
            String existingName = existing.getName().toLowerCase();
            for (DrugInteraction interaction : knownInteractions) {
                String a = interaction.getDrugA().toLowerCase();
                String b = interaction.getDrugB().toLowerCase();
                if ((a.equals(newName) && b.equals(existingName)) ||
                    (b.equals(newName) && a.equals(existingName))) {
                    results.add(interaction);
                }
            }
        }
        return results;
    }
}
