import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class ThemeManager {
    public enum Theme {
        LIGHT,
        DARK
    }

    private static ThemeManager instance;
    private Theme currentTheme;
    private PropertyChangeSupport pcs;

    private ThemeManager() {
        pcs = new PropertyChangeSupport(this);
        // Detect system theme or default to LIGHT
        currentTheme = detectSystemTheme();
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    private Theme detectSystemTheme() {
        // Placeholder for system theme detection logic
        // For now, default to LIGHT
        return Theme.LIGHT;
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public void setTheme(Theme newTheme) {
        Theme oldTheme = this.currentTheme;
        this.currentTheme = newTheme;
        pcs.firePropertyChange("theme", oldTheme, newTheme);
    }

    public void toggleTheme() {
        if (currentTheme == Theme.LIGHT) {
            setTheme(Theme.DARK);
        } else {
            setTheme(Theme.LIGHT);
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    // Utility colors for themes
    public Color getBackgroundColor() {
        return currentTheme == Theme.DARK ? new Color(34, 34, 34) : Color.WHITE;
    }

    public Color getForegroundColor() {
        return currentTheme == Theme.DARK ? Color.WHITE : Color.BLACK;
    }

    public Color getAccentColor() {
        return currentTheme == Theme.DARK ? new Color(70, 130, 180) : new Color(30, 144, 255);
    }
}
