package com.synapse.ui.theme;

import javafx.scene.Scene;

/**
 * Concrete strategy for the Dark Theme.
 */
public class DarkThemeStrategy implements ThemeStrategy {
    @Override
    public void applyTheme(Scene scene) {
        if (scene != null) {
            String darkCss = getClass().getResource("/styles/dark-theme.css").toExternalForm();
            if (!scene.getStylesheets().contains(darkCss)) {
                scene.getStylesheets().add(darkCss);
            }
        }
    }

    @Override
    public String getStylesheetPath() {
        return "/styles/dark-theme.css";
    }
}
