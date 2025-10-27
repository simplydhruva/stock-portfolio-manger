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
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import com.stockportfolio.model.Transaction;
import com.stockportfolio.services.ExportService;
import com.stockportfolio.utils.DatabaseManager;

public class TradeHistoryScreen extends JPanel {
    private App app;
    private DatabaseManager dbManager;

    private JTable tradeTable;
    private DefaultTableModel tableModel;
    private JButton refreshButton;

    public TradeHistoryScreen(App app, DatabaseManager dbManager) {
        this.app = app;
        this.dbManager = dbManager;
        initializeUI();
        loadTradeHistory();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // Title
        String username = "Guest";
        if (app.getCurrentUser() != null && app.getCurrentUser().getUsername() != null) {
            username = app.getCurrentUser().getUsername();
        }
        add(new JLabel("Trade History - " + username), BorderLayout.NORTH);

        // Trade history table
        String[] columnNames = {"Date", "Symbol", "Type", "Quantity", "Price", "Total Value", "Portfolio", "Status"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tradeTable = new JTable(tableModel);
        tradeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(tradeTable);
        add(scrollPane, BorderLayout.CENTER);

        // Control panel
        JPanel controlPanel = new JPanel(new FlowLayout());
        refreshButton = new JButton("Refresh History");
        refreshButton.addActionListener(e -> loadTradeHistory());
        controlPanel.add(refreshButton);

        JButton exportCsvButton = new JButton("Export to CSV");
        exportCsvButton.addActionListener(e -> {
            if (app.getCurrentUser() == null) {
                JOptionPane.showMessageDialog(this, "User not logged in. Please login first.", "Error", JOptionPane.ERROR_MESSAGE);
                app.showScreen("LOGIN");
                return;
            }
            ExportService exportService = new ExportService(dbManager);
            exportService.exportTradeHistory(app.getCurrentUser().getId());
        });
        controlPanel.add(exportCsvButton);

        JButton backButton = new JButton("Back to Dashboard");
        backButton.addActionListener(e -> app.showScreen("DASHBOARD"));
        controlPanel.add(backButton);

        add(controlPanel, BorderLayout.SOUTH);
    }

    private void loadTradeHistory() {
        refreshButton.setEnabled(false);
        refreshButton.setText("Loading...");

        // Clear existing data
        tableModel.setRowCount(0);

        // Load trade history in background thread
        new Thread(() -> {
            try {
                if (app.getCurrentUser() == null) {
                    SwingUtilities.invokeLater(() -> {
                        refreshButton.setEnabled(true);
                        refreshButton.setText("Refresh History");
                        Object[] errorRow = {"No user logged in", "", "", "", "", "", "", ""};
                        tableModel.addRow(errorRow);
                    });
                    return;
                }
                List<Transaction> transactions = dbManager.getTransactionsByUserId(app.getCurrentUser().getId());

                SwingUtilities.invokeLater(() -> {
                    for (Transaction transaction : transactions) {
                        Object[] rowData = {
                                transaction.getTimestamp().toString(),
                                transaction.getSymbol(),
                                transaction.getType(),
                                transaction.getQuantity(),
                                String.format("%.2f", transaction.getPrice()),
                                String.format("%.2f", transaction.getTotalAmount()),
                                "Portfolio " + transaction.getPortfolioId(),
                                transaction.getStatus()
                        };
                        tableModel.addRow(rowData);
                    }

                    refreshButton.setEnabled(true);
                    refreshButton.setText("Refresh History");

                    if (transactions.isEmpty()) {
                        // Show message if no trades
                        Object[] emptyRow = {"No trades found", "", "", "", "", "", "", ""};
                        tableModel.addRow(emptyRow);
                    }
                });

            } catch (SQLException e) {
                SwingUtilities.invokeLater(() -> {
                    refreshButton.setEnabled(true);
                    refreshButton.setText("Refresh History");
                    Object[] errorRow = {"Error loading trade history: " + e.getMessage(), "", "", "", "", "", "", ""};
                    tableModel.addRow(errorRow);
                });
            }
        }).start();
    }
}
