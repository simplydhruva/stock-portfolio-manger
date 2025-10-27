import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class SidebarPanel extends JPanel implements ThemeChangeListener {
    private App app;
    private String[] sectionNames = {
        "Dashboard", "Analytics",
        "Watchlist", "Trading", "Portfolio", "Settings"
    };
    private String[] sectionKeys = {
        "DASHBOARD", "ANALYTICS",
        "WATCHLIST", "TRADING", "PORTFOLIO", "SETTINGS"
    };
    private JLabel[] labels;

    public SidebarPanel(App app) {
        this.app = app;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(200, 0));

        labels = new JLabel[sectionNames.length];
        for (int i = 0; i < sectionNames.length; i++) {
            labels[i] = createSidebarLabel(sectionNames[i], sectionKeys[i]);
            add(labels[i]);
        }

        applyTheme();

        // Register for theme changes
        ThemeManager.getInstance().addPropertyChangeListener(this);
    }

    private JLabel createSidebarLabel(String text, String key) {
        JLabel label = new JLabel(text, SwingConstants.LEFT);
        label.setOpaque(true);
        label.setForeground(Color.LIGHT_GRAY);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        label.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 10));
        label.setAlignmentX(LEFT_ALIGNMENT);

        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                app.showScreen(key);
                highlightLabel(label);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                ThemeManager.Theme theme = ThemeManager.getInstance().getCurrentTheme();
                Color hoverBg = theme == ThemeManager.Theme.DARK ? new Color(50, 50, 50) : new Color(220, 220, 220);
                label.setBackground(hoverBg);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                ThemeManager themeManager = ThemeManager.getInstance();
                Color highlightBg = themeManager.getCurrentTheme() == ThemeManager.Theme.DARK ? new Color(70, 130, 180) : new Color(100, 149, 237);
                if (!label.getBackground().equals(highlightBg)) {
                    label.setBackground(themeManager.getBackgroundColor());
                }
            }
        });

        return label;
    }

    public void highlightLabel(JLabel labelToHighlight) {
        ThemeManager.Theme theme = ThemeManager.getInstance().getCurrentTheme();
        Color highlightBg = theme == ThemeManager.Theme.DARK ? new Color(70, 130, 180) : new Color(100, 149, 237);
        Color normalBg = ThemeManager.getInstance().getBackgroundColor();
        Color normalFg = ThemeManager.getInstance().getForegroundColor();
        Color highlightFg = Color.WHITE;

        for (JLabel label : labels) {
            if (label == labelToHighlight) {
                label.setBackground(highlightBg);
                label.setForeground(highlightFg);
            } else {
                label.setBackground(normalBg);
                label.setForeground(normalFg);
            }
        }
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme newTheme) {
        applyTheme();
        // Reapply highlight to current active label
        for (JLabel label : labels) {
            if (label.getBackground().equals(new Color(70, 130, 180)) || label.getBackground().equals(new Color(100, 149, 237))) {
                highlightLabel(label);
                break;
            }
        }
    }

    private void applyTheme() {
        ThemeManager themeManager = ThemeManager.getInstance();
        setBackground(themeManager.getBackgroundColor());
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.DARK_GRAY));

        // Update all labels
        for (JLabel label : labels) {
            label.setForeground(themeManager.getForegroundColor());
            label.setBackground(themeManager.getBackgroundColor());
        }
    }
}
