package com.synapse.ui;

/**
 * A simple utility to allow views to request navigation changes
 * without needing a direct reference to the App or its layout.
 */
public class Navigation {
    
    private static Runnable onNavigate;
    private static String targetViewId;

    public static void setNavigationHandler(Runnable handler) {
        onNavigate = handler;
    }

    public static void navigateTo(String viewId) {
        targetViewId = viewId;
        if (onNavigate != null) {
            onNavigate.run();
        }
    }

    public static String getTargetViewId() {
        return targetViewId;
    }
}
