import java.awt.BorderLayout;
import java.sql.SQLException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.stockportfolio.model.Stock;
import com.stockportfolio.model.WatchlistItem;
import com.stockportfolio.utils.DatabaseManager;

public class WatchlistScreen extends JPanel {
    private App app;
    private DatabaseManager dbManager;
    private JTable watchlistTable;
    private DefaultTableModel tableModel;
    private JButton addButton;
    private JButton removeButton;

    public WatchlistScreen(App app, DatabaseManager dbManager) {
        this.app = app;
        this.dbManager = dbManager;
        initializeUI();
        loadWatchlist();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // Table setup
        String[] columnNames = {"Symbol", "Target Price", "Notes", "Added At"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table cells non-editable
            }
        };
        watchlistTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(watchlistTable);
        add(scrollPane, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonPanel = new JPanel();
        addButton = new JButton("Add");
        removeButton = new JButton("Remove");
        removeButton.setEnabled(false);

        JButton backButton = new JButton("Back to Dashboard");
        backButton.addActionListener(e -> app.showScreen("DASHBOARD"));

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(backButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Button actions
        addButton.addActionListener(e -> showAddDialog());
        removeButton.addActionListener(e -> removeSelectedItem());

        // Enable remove button only when a row is selected
        watchlistTable.getSelectionModel().addListSelectionListener(e -> {
            boolean selected = watchlistTable.getSelectedRow() != -1;
            removeButton.setEnabled(selected);
        });
    }

    private void loadWatchlist() {
        try {
            tableModel.setRowCount(0); // Clear existing rows
            if (app.getCurrentUser() == null) {
                JOptionPane.showMessageDialog(this, "User not logged in. Please login first.", "Error", JOptionPane.ERROR_MESSAGE);
                app.showScreen("LOGIN");
                return;
            }
            int userId = app.getCurrentUser().getId();
            List<WatchlistItem> items = dbManager.getWatchlistByUserId(userId);
            for (WatchlistItem item : items) {
                tableModel.addRow(new Object[]{
                    item.getSymbol(),
                    item.getTargetPrice(),
                    item.getNotes(),
                    item.getAddedAt()
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading watchlist: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showAddDialog() {
        if (app.getCurrentUser() == null) {
            JOptionPane.showMessageDialog(this, "User not logged in. Please login first.", "Error", JOptionPane.ERROR_MESSAGE);
            app.showScreen("LOGIN");
            return;
        }
        JComboBox<String> symbolComboBox = new JComboBox<>();
        try {
            List<Stock> stocks = dbManager.getAllStocks();
            for (Stock stock : stocks) {
                symbolComboBox.addItem(stock.getSymbol());
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load stock symbols: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JTextField targetPriceField = new JTextField();
        JTextField notesField = new JTextField();

        Object[] message = {
            "Symbol:", symbolComboBox,
            "Target Price:", targetPriceField,
            "Notes:", notesField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Add Watchlist Item", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String symbol = (String) symbolComboBox.getSelectedItem();
            String targetPriceStr = targetPriceField.getText().trim();
            String notes = notesField.getText().trim();

            if (symbol == null || symbol.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Symbol is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double targetPrice = 0.0;
            if (!targetPriceStr.isEmpty()) {
                try {
                    targetPrice = Double.parseDouble(targetPriceStr);
                    if (targetPrice < 0) {
                        JOptionPane.showMessageDialog(this, "Target price must be non-negative.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid target price.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            try {
                int userId = app.getCurrentUser().getId();
                WatchlistItem newItem = new WatchlistItem(userId, symbol);
                newItem.setTargetPrice(targetPrice);
                newItem.setNotes(notes);
                dbManager.saveWatchlistItem(newItem);
                loadWatchlist();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error adding watchlist item: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void removeSelectedItem() {
        if (app.getCurrentUser() == null) {
            JOptionPane.showMessageDialog(this, "User not logged in. Please login first.", "Error", JOptionPane.ERROR_MESSAGE);
            app.showScreen("LOGIN");
            return;
        }
        int selectedRow = watchlistTable.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Remove selected item from watchlist?", "Confirm Remove", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // Get the id from the table model (assuming id is stored in a hidden column or we need to fetch it)
                // For simplicity, we'll assume the table has id in column 4 (hidden)
                // Actually, let's modify to store id in the table model
                // For now, we'll need to get the symbol and find the item
                String symbol = (String) tableModel.getValueAt(selectedRow, 0);
                int userId = app.getCurrentUser().getId();
                // Find the item by symbol and userId
                List<WatchlistItem> items = dbManager.getWatchlistByUserId(userId);
                for (WatchlistItem item : items) {
                    if (item.getSymbol().equals(symbol)) {
                        dbManager.deleteWatchlistItem(item.getId());
                        break;
                    }
                }
                loadWatchlist();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error removing watchlist item: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
