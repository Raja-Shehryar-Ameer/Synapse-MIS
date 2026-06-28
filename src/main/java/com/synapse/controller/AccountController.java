package com.synapse.controller;

import com.synapse.model.EmergencyProfile;
import com.synapse.model.Patient;
import com.synapse.repository.PatientRepository;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Map;

/**
 * Handles registration, login, and profile updates.
 */
public class AccountController {

    private static final String HASH_PREFIX = "pbkdf2";
    private static final int HASH_ITERATIONS = 65_536;
    private static final int KEY_LENGTH = 256;

    private final PatientRepository patientRepo = new PatientRepository();
    private Patient currentPatient;

    public Patient getCurrentPatient() {
        return currentPatient;
    }

    public void setCurrentPatient(Patient patient) {
        currentPatient = patient;
    }

    public String registerPatient(String fullName, String email, String password,
                                  LocalDate dob, String gender, Double heightCm,
                                  String bloodType, String allergies, String chronicConditions,
                                  String emergencyContactName, String emergencyContactPhone) {
        if (patientRepo.existsByEmail(email)) {
            return "Email already registered.";
        }

        Patient patient = new Patient(fullName, email, hashPassword(password), dob, gender, heightCm);
        EmergencyProfile profile = new EmergencyProfile(
                bloodType, allergies, chronicConditions,
                emergencyContactName, emergencyContactPhone
        );
        patient.setEmergencyProfile(profile);

        patientRepo.save(patient);
        currentPatient = patient;
        return null;
    }

    public String login(String email, String password) {
        Patient patient = patientRepo.findByEmail(email);
        if (patient == null) {
            return "No account found with this email.";
        }
        if (!passwordMatches(patient, password)) {
            return "Incorrect password.";
        }

        if (isLegacyHash(patient.getPasswordHash())) {
            patient.setPasswordHash(hashPassword(password));
            patient = patientRepo.update(patient);
        }

        currentPatient = patient;
        return null;
    }

    public String updatePatientProfile(Map<String, Object> data) {
        if (currentPatient == null) {
            return "No active account.";
        }

        if (data.containsKey("email")) {
            String email = (String) data.get("email");
            if (email != null && !email.equalsIgnoreCase(currentPatient.getEmail())
                    && patientRepo.existsByEmailForOtherPatient(email, currentPatient.getPatientId())) {
                return "Email already registered to another account.";
            }
        }

        currentPatient.updateDetails(data);
        currentPatient = patientRepo.update(currentPatient);
        return null;
    }

    public String updateEmergencyProfile(String bloodType, String allergies,
                                         String chronicConditions, String contactName, String contactPhone) {
        if (currentPatient == null) {
            return "No active account.";
        }

        EmergencyProfile profile = currentPatient.getEmergencyProfile();
        if (profile == null) {
            profile = new EmergencyProfile();
            currentPatient.setEmergencyProfile(profile);
        }

        profile.setBloodType(bloodType);
        profile.setAllergies(allergies);
        profile.setChronicConditions(chronicConditions);
        profile.setEmergencyContactName(contactName);
        profile.setEmergencyContactPhone(contactPhone);

        currentPatient = patientRepo.update(currentPatient);
        return null;
    }

    public boolean verifyPassword(String rawPassword) {
        if (currentPatient == null) {
            return false;
        }

        boolean matches = passwordMatches(currentPatient, rawPassword);
        if (matches && isLegacyHash(currentPatient.getPasswordHash())) {
            currentPatient.setPasswordHash(hashPassword(rawPassword));
            currentPatient = patientRepo.update(currentPatient);
        }
        return matches;
    }

    public void logout() {
        currentPatient = null;
    }

    public void deleteAccount() {
        if (currentPatient == null) {
            return;
        }
        patientRepo.delete(currentPatient);
        currentPatient = null;
    }

    private boolean passwordMatches(Patient patient, String rawPassword) {
        String stored = patient.getPasswordHash();
        return isLegacyHash(stored) ? stored.equals(legacyHash(rawPassword)) : verifySecureHash(rawPassword, stored);
    }

    private boolean isLegacyHash(String storedHash) {
        return storedHash != null && !storedHash.startsWith(HASH_PREFIX + "$");
    }

    private String hashPassword(String password) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);

            byte[] hash = pbkdf2(password.toCharArray(), salt, HASH_ITERATIONS, KEY_LENGTH);
            return HASH_PREFIX + "$" + HASH_ITERATIONS + "$"
                    + Base64.getEncoder().encodeToString(salt) + "$"
                    + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Password hashing failed", e);
        }
    }

    private boolean verifySecureHash(String password, String storedHash) {
        try {
            String[] parts = storedHash.split("\\$");
            if (parts.length != 4 || !HASH_PREFIX.equals(parts[0])) {
                return false;
            }

            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(password.toCharArray(), salt, iterations, expected.length * 8);
            return java.security.MessageDigest.isEqual(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return factory.generateSecret(spec).getEncoded();
    }

    private String legacyHash(String password) {
        return Integer.toHexString(password.hashCode());
    }
}
