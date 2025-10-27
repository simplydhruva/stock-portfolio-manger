package com.stockportfolio.utils;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

public class ThemeManager {
    public enum Theme {
        LIGHT, DARK
    }

    private static ThemeManager instance;
    private Theme currentTheme;
    private Preferences prefs;
    private Map<String, Object> lightTheme;
    private Map<String, Object> darkTheme;

    private ThemeManager() {
        prefs = Preferences.userNodeForPackage(ThemeManager.class);
        initializeThemes();
        loadTheme();
    }

    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    private void initializeThemes() {
        lightTheme = new HashMap<>();
        darkTheme = new HashMap<>();

        // Light theme
        lightTheme.put("Panel.background", Color.WHITE);
        lightTheme.put("Panel.foreground", Color.BLACK);
        lightTheme.put("Button.background", Color.LIGHT_GRAY);
        lightTheme.put("Button.foreground", Color.BLACK);
        lightTheme.put("TextField.background", Color.WHITE);
        lightTheme.put("TextField.foreground", Color.BLACK);
        lightTheme.put("Table.background", Color.WHITE);
        lightTheme.put("Table.foreground", Color.BLACK);
        lightTheme.put("TableHeader.background", Color.LIGHT_GRAY);
        lightTheme.put("TableHeader.foreground", Color.BLACK);

        // Dark theme
        darkTheme.put("Panel.background", new Color(45, 45, 45));
        darkTheme.put("Panel.foreground", Color.WHITE);
        darkTheme.put("Button.background", new Color(70, 70, 70));
        darkTheme.put("Button.foreground", Color.WHITE);
        darkTheme.put("TextField.background", new Color(60, 60, 60));
        darkTheme.put("TextField.foreground", Color.WHITE);
        darkTheme.put("Table.background", new Color(45, 45, 45));
        darkTheme.put("Table.foreground", Color.WHITE);
        darkTheme.put("TableHeader.background", new Color(70, 70, 70));
        darkTheme.put("TableHeader.foreground", Color.WHITE);
    }

    public void setTheme(Theme theme) {
        this.currentTheme = theme;
        applyTheme();
        saveTheme();
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    private void applyTheme() {
        UIDefaults defaults = UIManager.getDefaults();
        Map<String, Object> themeColors = currentTheme == Theme.LIGHT ? lightTheme : darkTheme;

        for (Map.Entry<String, Object> entry : themeColors.entrySet()) {
            defaults.put(entry.getKey(), entry.getValue());
        }

        // Force repaint of all components
        for (java.awt.Window window : java.awt.Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
        }
    }

    private void loadTheme() {
        String themeStr = prefs.get("theme", null);
        if (themeStr == null) {
            // First run, detect system preference
            currentTheme = detectSystemTheme();
        } else {
            currentTheme = Theme.valueOf(themeStr);
        }
        applyTheme();
    }

    private Theme detectSystemTheme() {
        // Simple detection: default to LIGHT for most systems
        // Can be enhanced with OS-specific dark mode detection
        return Theme.LIGHT;
    }

    private void saveTheme() {
        prefs.put("theme", currentTheme.name());
    }

    public void toggleTheme() {
        setTheme(currentTheme == Theme.LIGHT ? Theme.DARK : Theme.LIGHT);
    }
}
