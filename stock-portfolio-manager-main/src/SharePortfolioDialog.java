import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.stockportfolio.model.Portfolio;
import com.stockportfolio.model.SharedPortfolio;
import com.stockportfolio.utils.DatabaseManager;

public class SharePortfolioDialog extends JDialog {
    private App app;
    private JComboBox<Portfolio> portfolioComboBox;
    private JCheckBox publicCheckBox;
    private JButton shareButton;
    private JButton cancelButton;

    public SharePortfolioDialog(JFrame parent, App app) {
        super(parent, "Share Portfolio", true);
        this.app = app;
        initializeComponents();
        layoutComponents();
        addEventListeners();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initializeComponents() {
        portfolioComboBox = new JComboBox<>();
        publicCheckBox = new JCheckBox("Share publicly");
        publicCheckBox.setSelected(true); // Default to public
        shareButton = new JButton("Share");
        cancelButton = new JButton("Cancel");

        loadPortfolios();
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        contentPanel.add(new JLabel("Select Portfolio:"));
        contentPanel.add(portfolioComboBox);
        contentPanel.add(new JLabel("Public Sharing:"));
        contentPanel.add(publicCheckBox);

        add(contentPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(shareButton);
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void addEventListeners() {
        shareButton.addActionListener(e -> sharePortfolio());
        cancelButton.addActionListener(e -> dispose());
    }

    private void loadPortfolios() {
        try {
            if (app.getCurrentUser() == null) {
                JOptionPane.showMessageDialog(this, "No user logged in. Please login first.", "Error", JOptionPane.ERROR_MESSAGE);
                dispose();
                return;
            }
            DatabaseManager db = DatabaseManager.getInstance();
            List<Portfolio> portfolios = db.getPortfoliosByUserId(app.getCurrentUser().getId());
            for (Portfolio portfolio : portfolios) {
                portfolioComboBox.addItem(portfolio);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading portfolios: " + ex.getMessage());
        }
    }

    private void sharePortfolio() {
        if (app.getCurrentUser() == null) {
            JOptionPane.showMessageDialog(this, "No user logged in. Please login first.", "Error", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }
        Portfolio selectedPortfolio = (Portfolio) portfolioComboBox.getSelectedItem();
        if (selectedPortfolio == null) {
            JOptionPane.showMessageDialog(this, "Please select a portfolio to share.");
            return;
        }

        boolean isPublic = publicCheckBox.isSelected();

        try {
            DatabaseManager db = DatabaseManager.getInstance();
            // Check if already shared
            if (db.isPortfolioShared(selectedPortfolio.getId())) {
                JOptionPane.showMessageDialog(this, "This portfolio is already shared.");
                return;
            }

            SharedPortfolio sharedPortfolio = new SharedPortfolio(selectedPortfolio.getId(), app.getCurrentUser().getId(), isPublic);
            db.sharePortfolio(sharedPortfolio);
            JOptionPane.showMessageDialog(this, "Portfolio shared successfully!");
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error sharing portfolio: " + ex.getMessage());
        }
    }
}
