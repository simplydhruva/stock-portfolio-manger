import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class InfoCard extends JPanel implements ThemeChangeListener {
    private JLabel titleLabel;
    private JLabel valueLabel;
    private String title;
    private String value;

    public InfoCard(String title, String value) {
        this.title = title;
        this.value = value;
        initializeUI();
        ThemeManager.getInstance().addPropertyChangeListener(this);
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SF Pro Display", Font.BOLD, 20));
        add(titleLabel, BorderLayout.NORTH);

        valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SF Pro Display", Font.BOLD, 32));
        add(valueLabel, BorderLayout.CENTER);

        applyTheme();
    }

    public void setValue(String value) {
        this.value = value;
        valueLabel.setText(value);
    }

    public void updateValue(String value) {
        setValue(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme newTheme) {
        applyTheme();
    }

    private void applyTheme() {
        ThemeManager themeManager = ThemeManager.getInstance();
        setBackground(themeManager.getBackgroundColor());
        titleLabel.setForeground(themeManager.getForegroundColor());
        valueLabel.setForeground(themeManager.getForegroundColor());
    }
}
