package com.synapse.ui.theme;

import javafx.scene.Scene;

/**
 * Strategy interface for applying UI themes.
 */
public interface ThemeStrategy {
    void applyTheme(Scene scene);
    String getStylesheetPath();
}
