package com.synapse.ui.theme;

import javafx.scene.Scene;

/**
 * Context class for managing the active ThemeStrategy (GoF Strategy Pattern).
 */
public class ThemeManager {
    private static ThemeManager instance;
    private ThemeStrategy currentStrategy;
    private Scene scene;
    private boolean isDark;

    private ThemeManager() {
        this.currentStrategy = new LightThemeStrategy();
        this.isDark = false;
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
        currentStrategy.applyTheme(this.scene);
    }

    public void setStrategy(ThemeStrategy strategy) {
        this.currentStrategy = strategy;
        if (this.scene != null) {
            this.currentStrategy.applyTheme(this.scene);
        }
    }

    public ThemeStrategy getStrategy() {
        return currentStrategy;
    }

    public String getAdditionalStylesheet() {
        return currentStrategy != null ? currentStrategy.getStylesheetPath() : null;
    }

    public void toggleTheme() {
        isDark = !isDark;
        setStrategy(isDark ? new DarkThemeStrategy() : new LightThemeStrategy());
    }

    public boolean isDarkMode() {
        return isDark;
    }
}
