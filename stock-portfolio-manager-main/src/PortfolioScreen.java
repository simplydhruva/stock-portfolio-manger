import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import com.stockportfolio.model.Portfolio;
import com.stockportfolio.utils.DatabaseManager;

public class PortfolioScreen extends JPanel {
    private App app;
    private DatabaseManager dbManager;
    private JTable portfolioTable;
    private DefaultTableModel tableModel;
    private JButton createButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton viewButton;

    public PortfolioScreen(App app, DatabaseManager dbManager) {
        this.app = app;
        this.dbManager = dbManager;
        initializeUI();
        loadPortfolios();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // Title
        String username = "Guest";
        if (app.getCurrentUser() != null && app.getCurrentUser().getUsername() != null) {
            username = app.getCurrentUser().getUsername();
        }
        add(new JLabel("Portfolio Management - " + username, SwingConstants.CENTER), BorderLayout.NORTH);

        // Portfolio table
        String[] columnNames = {"ID", "Name", "Description", "Total Value", "Total Cost", "Total P&L", "Created At"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        portfolioTable = new JTable(tableModel);
        portfolioTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(portfolioTable);
        add(scrollPane, BorderLayout.CENTER);

        // Control panel
        JPanel controlPanel = new JPanel(new FlowLayout());

        createButton = new JButton("Create Portfolio");
        createButton.addActionListener(e -> createPortfolio());
        controlPanel.add(createButton);

        editButton = new JButton("Edit Portfolio");
        editButton.addActionListener(e -> editPortfolio());
        editButton.setEnabled(false);
        controlPanel.add(editButton);

        deleteButton = new JButton("Delete Portfolio");
        deleteButton.addActionListener(e -> deletePortfolio());
        deleteButton.setEnabled(false);
        controlPanel.add(deleteButton);

        viewButton = new JButton("View Details");
        viewButton.addActionListener(e -> viewPortfolioDetails());
        viewButton.setEnabled(false);
        controlPanel.add(viewButton);

        JButton shareButton = new JButton("Share Portfolio");
        shareButton.addActionListener(e -> sharePortfolio());
        shareButton.setEnabled(false);
        controlPanel.add(shareButton);

        JButton compareButton = new JButton("Compare Portfolios");
        compareButton.addActionListener(e -> comparePortfolios());
        controlPanel.add(compareButton);

        JButton backButton = new JButton("Back to Dashboard");
        backButton.addActionListener(e -> app.showScreen("DASHBOARD"));
        controlPanel.add(backButton);

        add(controlPanel, BorderLayout.SOUTH);

        // Enable/disable buttons based on selection
        portfolioTable.getSelectionModel().addListSelectionListener(e -> {
            boolean selected = portfolioTable.getSelectedRow() != -1;
            editButton.setEnabled(selected);
            deleteButton.setEnabled(selected);
            viewButton.setEnabled(selected);
            shareButton.setEnabled(selected);
        });
    }

    private void loadPortfolios() {
        try {
            if (app.getCurrentUser() == null) {
                JOptionPane.showMessageDialog(this, "No user logged in.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            List<Portfolio> portfolios = dbManager.getPortfoliosByUserId(app.getCurrentUser().getId());
            tableModel.setRowCount(0);

            for (Portfolio portfolio : portfolios) {
                Object[] rowData = {
                        portfolio.getId(),
                        portfolio.getName(),
                        portfolio.getDescription() != null ? portfolio.getDescription() : "",
                        String.format("%.2f", portfolio.getTotalValue()),
                        String.format("%.2f", portfolio.getTotalCost()),
                        String.format("%.2f", portfolio.getTotalPnL()),
                        portfolio.getCreatedAt().toString()
                };
                tableModel.addRow(rowData);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading portfolios: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createPortfolio() {
        JTextField nameField = new JTextField();
        JTextField descriptionField = new JTextField();

        Object[] message = {
                "Portfolio Name:", nameField,
                "Description (optional):", descriptionField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Create New Portfolio", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String description = descriptionField.getText().trim();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Portfolio name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                if (app.getCurrentUser() == null) {
                    JOptionPane.showMessageDialog(this, "No user logged in.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Portfolio portfolio = new Portfolio(0, app.getCurrentUser().getId(), name);
                portfolio.setDescription(description.isEmpty() ? null : description);
                portfolio.setCreatedAt(java.time.LocalDateTime.now());
                portfolio.setUpdatedAt(java.time.LocalDateTime.now());
                portfolio.setTotalValue(0.0);
                portfolio.setTotalCostBasis(0.0);
                dbManager.savePortfolio(portfolio);
                loadPortfolios();
                JOptionPane.showMessageDialog(this, "Portfolio created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error creating portfolio: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void editPortfolio() {
        int selectedRow = portfolioTable.getSelectedRow();
        if (selectedRow == -1) return;

        int portfolioId = (Integer) tableModel.getValueAt(selectedRow, 0);
        String currentName = (String) tableModel.getValueAt(selectedRow, 1);
        String currentDescription = (String) tableModel.getValueAt(selectedRow, 2);

        JTextField nameField = new JTextField(currentName);
        JTextField descriptionField = new JTextField(currentDescription != null ? currentDescription : "");

        Object[] message = {
                "Portfolio Name:", nameField,
                "Description (optional):", descriptionField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Edit Portfolio", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String description = descriptionField.getText().trim();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Portfolio name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                Portfolio portfolio = dbManager.getPortfolioById(portfolioId);
                portfolio.setName(name);
                portfolio.setDescription(description.isEmpty() ? null : description);
                dbManager.updatePortfolio(portfolio);
                loadPortfolios();
                JOptionPane.showMessageDialog(this, "Portfolio updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error updating portfolio: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deletePortfolio() {
        int selectedRow = portfolioTable.getSelectedRow();
        if (selectedRow == -1) return;

        int portfolioId = (Integer) tableModel.getValueAt(selectedRow, 0);
        String portfolioName = (String) tableModel.getValueAt(selectedRow, 1);

        int option = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete portfolio '" + portfolioName + "'?\nThis action cannot be undone.",
                "Delete Portfolio", JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            try {
                dbManager.deletePortfolio(portfolioId);
                loadPortfolios();
                JOptionPane.showMessageDialog(this, "Portfolio deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error deleting portfolio: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void viewPortfolioDetails() {
        int selectedRow = portfolioTable.getSelectedRow();
        if (selectedRow == -1) return;

        int portfolioId = (Integer) tableModel.getValueAt(selectedRow, 0);
        String portfolioName = (String) tableModel.getValueAt(selectedRow, 1);

        // Open detailed portfolio view
        PortfolioDetailDialog detailDialog = new PortfolioDetailDialog(app, dbManager, portfolioId, portfolioName);
        detailDialog.setVisible(true);
    }

    private void sharePortfolio() {
        int selectedRow = portfolioTable.getSelectedRow();
        if (selectedRow == -1) return;

        // Open share portfolio dialog
        SharePortfolioDialog shareDialog = new SharePortfolioDialog((javax.swing.JFrame) javax.swing.SwingUtilities.getWindowAncestor(this), app);
        shareDialog.setVisible(true);
    }

    private void comparePortfolios() {
        // Open portfolio comparison dialog
        PortfolioComparisonDialog comparisonDialog = new PortfolioComparisonDialog(app, dbManager);
        comparisonDialog.setVisible(true);
    }
}
