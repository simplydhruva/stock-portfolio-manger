import javax.swing.*;

import com.stockportfolio.utils.DatabaseManager;
import com.stockportfolio.utils.Logger;
import com.stockportfolio.model.User;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;

public class LoginScreen extends JPanel {
    private App app;
    private DatabaseManager dbManager;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    public LoginScreen(App app, DatabaseManager dbManager) {
        this.app = app;
        this.dbManager = dbManager;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        // Title
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(new JLabel("Stock Portfolio Manager Login"), gbc);

        // Username
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        usernameField = new JTextField(20);
        add(usernameField, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        add(passwordField, gbc);

        // Login Button
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        loginButton = new JButton("Login");
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                login();
            }
        });
        add(loginButton, gbc);

        // Register Button
        gbc.gridx = 1;
        gbc.gridy = 3;
        registerButton = new JButton("Register");
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                app.showScreen("REGISTRATION");
            }
        });
        add(registerButton, gbc);
    }

    private void login() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        // Input validation
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username and password are required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            User user = dbManager.getUserByUsername(username);
            if (user == null) {
                Logger.warn("Failed login attempt for non-existent user: " + username);
                JOptionPane.showMessageDialog(this, "Invalid username or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Check if account is locked
            LocalDateTime now = LocalDateTime.now();
            if (user.getAccountLockedUntil() != null && now.isBefore(user.getAccountLockedUntil())) {
                Logger.warn("Login attempt for locked account: " + username);
                JOptionPane.showMessageDialog(this, "Account is locked until " + user.getAccountLockedUntil() + ". Please try again later.", "Account Locked", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (com.stockportfolio.utils.PasswordSecurity.verifyPassword(password, user.getHashedPassword(), user.getSalt())) {
                // Successful login: reset failed attempts and lock, update last login
                dbManager.updateUserLoginSecurity(user.getId(), 0, null);
                dbManager.updateLastLogin(user.getId());
                Logger.info("Successful login for user: " + username);
                app.setCurrentUser(user);
                app.showScreen("DASHBOARD");
            } else {
                // Increment failed attempts
                int failedAttempts = user.getFailedLoginAttempts() + 1;
                LocalDateTime lockUntil = null;
                if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                    lockUntil = now.plusMinutes(LOCK_DURATION_MINUTES);
                    Logger.warn("Account locked for user: " + username + " after " + failedAttempts + " failed attempts");
                    JOptionPane.showMessageDialog(this, "Account locked due to too many failed login attempts. Try again after " + lockUntil + ".", "Account Locked", JOptionPane.ERROR_MESSAGE);
                } else {
                    Logger.warn("Failed login attempt for user: " + username + " (attempt " + failedAttempts + ")");
                    JOptionPane.showMessageDialog(this, "Invalid username or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                }
                dbManager.updateUserLoginSecurity(user.getId(), failedAttempts, lockUntil);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Login error: " + e.getMessage(), "Login Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
    }
}
