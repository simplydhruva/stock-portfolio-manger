import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public interface ThemeChangeListener extends PropertyChangeListener {
    @Override
    default void propertyChange(PropertyChangeEvent evt) {
        if ("theme".equals(evt.getPropertyName())) {
            onThemeChanged((ThemeManager.Theme) evt.getNewValue());
        }
    }

    void onThemeChanged(ThemeManager.Theme newTheme);
}
