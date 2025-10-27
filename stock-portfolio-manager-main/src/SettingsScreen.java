import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.stockportfolio.model.User;
import com.stockportfolio.utils.DatabaseManager;

public class SettingsScreen extends JPanel implements ThemeChangeListener {
    private App app;
    private DatabaseManager dbManager;

    private JButton toggleThemeButton;
    private JLabel themeStatusLabel;
    private JTextField nameField;
    private JTextField emailField;
    private JButton saveUserInfoButton;

    public SettingsScreen(App app, DatabaseManager dbManager) {
        this.app = app;
        this.dbManager = dbManager;
        initializeUI();
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme newTheme) {
        updateThemeStatusLabel(newTheme);
    }

    private void updateThemeStatusLabel(ThemeManager.Theme theme) {
        if (theme == ThemeManager.Theme.DARK) {
            themeStatusLabel.setText("Current Theme: Complete Black Dark Mode");
        } else {
            themeStatusLabel.setText("Current Theme: Light Mode");
        }
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // Title
        add(new JLabel("Settings", JLabel.CENTER), BorderLayout.NORTH);

        // Main content panel
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Theme section
        gbc.gridx = 0; gbc.gridy = 0;
        contentPanel.add(new JLabel("Theme:"), gbc);
        gbc.gridx = 1;
        themeStatusLabel = new JLabel();
        contentPanel.add(themeStatusLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        toggleThemeButton = new JButton("Toggle to Complete Black Dark Mode");
        toggleThemeButton.addActionListener(e -> ThemeManager.getInstance().toggleTheme());
        contentPanel.add(toggleThemeButton, gbc);

        // User info section
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 2;
        contentPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(20);
        contentPanel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        contentPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        emailField = new JTextField(20);
        contentPanel.add(emailField, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        saveUserInfoButton = new JButton("Update User Info");
        saveUserInfoButton.addActionListener(e -> saveUserInfo());
        contentPanel.add(saveUserInfoButton, gbc);

        add(contentPanel, BorderLayout.CENTER);

        // Initialize theme status label
        updateThemeStatusLabel(ThemeManager.getInstance().getCurrentTheme());

        // Load user info if logged in
        loadUserInfo();

        // Register for theme changes
        ThemeManager.getInstance().addPropertyChangeListener(this);
    }

    private void loadUserInfo() {
        User user = app.getCurrentUser();
        if (user != null) {
            nameField.setText(user.getUsername());
            emailField.setText(user.getEmail());
        }
    }

    private void saveUserInfo() {
        User user = app.getCurrentUser();
        if (user == null) {
            JOptionPane.showMessageDialog(this, "Please log in to update user info.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String newName = nameField.getText().trim();
        String newEmail = emailField.getText().trim();

        if (newName.isEmpty() || newEmail.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name and email cannot be empty.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            dbManager.updateUserInfo(user.getId(), newName, newEmail);
            user.setUsername(newName);
            user.setEmail(newEmail);
            JOptionPane.showMessageDialog(this, "User info updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to update user info: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
