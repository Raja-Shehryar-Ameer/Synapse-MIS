package com.synapse.controller;

import com.synapse.model.EmergencyProfile;
import com.synapse.model.Patient;
import com.synapse.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AccountControllerTest {

    private AccountController controller;
    private PatientRepository repository;

    @BeforeEach
    void setUp() {
        controller = new AccountController();
        repository = new PatientRepository();
    }

    @Test
    void testRegisterAndLoginSuccess() {
        String email = "test" + System.currentTimeMillis() + "@test.com";
        String error = controller.registerPatient("Test User", email, "securePassword123", LocalDate.of(1990, 1, 1), "Male", 175.0, "O+", "None", "None", "Contact", "123");
        assertNull(error, "Registration should succeed");

        Patient patient = controller.getCurrentPatient();
        assertNotNull(patient);
        assertEquals("Test User", patient.getFullName());

        controller.logout();
        assertNull(controller.getCurrentPatient());

        String loginError = controller.login(email, "securePassword123");
        assertNull(loginError, "Login should succeed");
        assertNotNull(controller.getCurrentPatient());
        assertEquals(email, controller.getCurrentPatient().getEmail());

        repository.delete(controller.getCurrentPatient());
    }

    @Test
    void testLoginInvalidPassword() {
        String email = "test" + System.currentTimeMillis() + "@test.com";
        controller.registerPatient("Test User", email, "securePassword123", LocalDate.of(1990, 1, 1), "Male", 175.0, "O+", "None", "None", "Contact", "123");
        controller.logout();

        String loginError = controller.login(email, "wrongPassword");
        assertNotNull(loginError, "Login should fail with wrong password");
        assertEquals("Incorrect password.", loginError);

        controller.login(email, "securePassword123");
        repository.delete(controller.getCurrentPatient());
    }

    @Test
    void testVerifyPassword() {
        String email = "test" + System.currentTimeMillis() + "@test.com";
        controller.registerPatient("Test User", email, "securePassword123", LocalDate.of(1990, 1, 1), "Male", 175.0, "O+", "None", "None", "Contact", "123");
        
        assertTrue(controller.verifyPassword("securePassword123"));
        assertFalse(controller.verifyPassword("wrongPassword"));

        repository.delete(controller.getCurrentPatient());
    }
}
