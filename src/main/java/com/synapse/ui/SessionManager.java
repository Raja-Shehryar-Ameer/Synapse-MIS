package com.synapse.ui;

import com.synapse.model.Patient;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class SessionManager {

    private static Patient currentPatient;
    private static final BooleanProperty loggedIn = new SimpleBooleanProperty(false);

    public static Patient getCurrentPatient() { return currentPatient; }

    public static void setCurrentPatient(Patient p) {
        currentPatient = p;
        loggedIn.set(p != null);
    }

    public static boolean isLoggedIn() { return currentPatient != null; }

    public static void logout() {
        currentPatient = null;
        loggedIn.set(false);
    }

    /** Observable property — listen to this in App.java to show/hide sidebar. */
    public static BooleanProperty loggedInProperty() { return loggedIn; }
}
