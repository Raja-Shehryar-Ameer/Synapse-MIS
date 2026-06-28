package com.synapse.ui;

import com.synapse.controller.AccountController;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import netscape.javascript.JSObject;

import java.time.LocalDate;

/**
 * Java-to-JavaScript bridge for the auth.html WebView.
 * Keep a strong reference to this object in AccountView to prevent GC.
 */
public class AuthBridge {

    private final AccountController controller;
    private final javafx.scene.web.WebEngine engine;
    private final Runnable onSuccess;

    public AuthBridge(AccountController controller,
                      javafx.scene.web.WebEngine engine,
                      Runnable onSuccess) {
        this.controller = controller;
        this.engine = engine;
        this.onSuccess = onSuccess;
    }

    /** Called by JS: window.bridge.login(email, pass) */
    public void login(String email, String password) {
        String err = controller.login(email, password);
        if (err != null) {
            final String msg = err;
            Platform.runLater(() ->
                engine.executeScript("showLoginError('" + escape(msg) + "')"));
        } else {
            SessionManager.setCurrentPatient(controller.getCurrentPatient());
            Platform.runLater(onSuccess);
        }
    }

    /** Called by JS: window.bridge.register(...) */
    public void register(String fullName, String email, String password,
                         String dob, String gender, String height,
                         String blood, String allergies, String chronic,
                         String contactName, String contactPhone, String storage) {
        try {
            if ("SQL_SERVER".equals(storage)) {
                com.synapse.HibernateUtil.setActiveFactory(com.synapse.HibernateUtil.getSqlEmf());
            } else {
                com.synapse.HibernateUtil.setActiveFactory(com.synapse.HibernateUtil.getFileEmf());
            }

            LocalDate dobDate = (dob == null || dob.isBlank()) ? null : LocalDate.parse(dob.trim());
            Double h = (height == null || height.isBlank()) ? null : Double.parseDouble(height.trim());
            String g = (gender == null || gender.isBlank()) ? null : gender.trim();

            String err = controller.registerPatient(
                    fullName, email, password, dobDate, g, h,
                    blood, allergies, chronic, contactName, contactPhone);

            if (err != null) {
                final String msg = err;
                Platform.runLater(() ->
                    engine.executeScript("showRegError('" + escape(msg) + "')"));
            } else {
                SessionManager.setCurrentPatient(controller.getCurrentPatient());
                Platform.runLater(onSuccess);
            }
        } catch (Exception e) {
            final String msg = e.getMessage() != null ? e.getMessage() : "Unexpected error";
            Platform.runLater(() ->
                engine.executeScript("showRegError('" + escape(msg) + "')"));
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", " ")
                .replace("\r", "");
    }
}
