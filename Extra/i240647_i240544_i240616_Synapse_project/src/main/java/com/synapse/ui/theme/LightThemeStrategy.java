package com.synapse.ui.theme;

import javafx.scene.Scene;

/**
 * Concrete strategy for the Light Theme (default).
 */
public class LightThemeStrategy implements ThemeStrategy {
    @Override
    public void applyTheme(Scene scene) {
        if (scene != null) {
            scene.getStylesheets().removeIf(s -> s.endsWith("dark-theme.css"));
        }
    }

    @Override
    public String getStylesheetPath() {
        return null;
    }
}
