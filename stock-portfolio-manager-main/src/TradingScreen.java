import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import com.stockportfolio.model.Portfolio;
import com.stockportfolio.model.Stock;
import com.stockportfolio.model.Transaction;
import com.stockportfolio.services.api.RealTimeStockAPI;
import com.stockportfolio.utils.DatabaseManager;

public class TradingScreen extends JPanel {
    private App app;
    private DatabaseManager dbManager;
    private TradeExecutor tradeExecutor;
    private RealTimeStockAPI realTimeStockAPI;

    private JComboBox<Portfolio> portfolioComboBox;
    private JTable stocksTable;
    private DefaultTableModel stocksTableModel;
    private JTable transactionsTable;
    private DefaultTableModel transactionsTableModel;
    private JComboBox<String> symbolComboBox;
    private JTextField quantityField;
    private JButton buyButton;
    private JButton sellButton;
    private JButton refreshButton;

    public TradingScreen(App app, DatabaseManager dbManager, RealTimeStockAPI realTimeStockAPI) {
        this.app = app;
        this.dbManager = dbManager;
        this.tradeExecutor = new TradeExecutor(dbManager);
        this.realTimeStockAPI = realTimeStockAPI;
        initializeUI();
        loadStockQuotes();
        loadStockSymbols();
        // Remove loadUserPortfolios call here to avoid NPE on app.getCurrentUser() being null
        // loadUserPortfolios();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // Portfolio selection panel
        JPanel portfolioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        portfolioPanel.add(new JLabel("Select Portfolio:"));
        portfolioComboBox = new JComboBox<>();
        portfolioPanel.add(portfolioComboBox);
        portfolioComboBox.addActionListener(e -> loadTransactionsForSelectedPortfolio());

        // Buy/Sell panel
        JPanel tradePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tradePanel.add(new JLabel("Symbol:"));
        symbolComboBox = new JComboBox<>();
        tradePanel.add(symbolComboBox);
        tradePanel.add(new JLabel("Quantity:"));
        quantityField = new JTextField(4);
        tradePanel.add(quantityField);
        buyButton = new JButton("Buy");
        sellButton = new JButton("Sell");
        tradePanel.add(buyButton);
        tradePanel.add(sellButton);

        buyButton.addActionListener(e -> executeTrade(true));
        sellButton.addActionListener(e -> executeTrade(false));

        // Stocks table
        String[] stocksColumnNames = {"Symbol", "Price", "Change", "Change %", "Volume"};
        stocksTableModel = new DefaultTableModel(stocksColumnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        stocksTable = new JTable(stocksTableModel);
        JScrollPane stocksScrollPane = new JScrollPane(stocksTable);

        // Transactions table
        String[] transactionsColumnNames = {"ID", "Symbol", "Type", "Quantity", "Price", "Total", "Order Type", "Timestamp", "Status"};
        transactionsTableModel = new DefaultTableModel(transactionsColumnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        transactionsTable = new JTable(transactionsTableModel);
        JScrollPane transactionsScrollPane = new JScrollPane(transactionsTable);

        // Split pane to show stocks and transactions side by side
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, stocksScrollPane, transactionsScrollPane);
        splitPane.setDividerLocation(250);

        // Refresh button
        refreshButton = new JButton("Refresh Quotes");
        refreshButton.addActionListener(e -> loadStockQuotes());

        // Top panel to hold portfolio selection and trade controls
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlsPanel.add(portfolioPanel);
        controlsPanel.add(tradePanel);
        controlsPanel.add(refreshButton);
        topPanel.add(controlsPanel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    private void loadStockQuotes() {
        stocksTableModel.setRowCount(0);

        // Get all top 50 Fortune 500 stocks from the API
        List<String> popularStocks = realTimeStockAPI.getPopularStocks();

        // Fetch data for each stock asynchronously
        CompletableFuture<Void>[] futures = popularStocks.stream()
                .map(symbol -> realTimeStockAPI.getStockQuote(symbol)
                        .thenAccept(quote -> SwingUtilities.invokeLater(() -> {
                            Object[] rowData = {
                                quote.getSymbol(),
                                String.format("%.2f", quote.getCurrentPrice()),
                                String.format("%.2f", quote.getChange()),
                                String.format("%s%%", quote.getChangePercent()),
                                quote.getVolume()
                            };
                            stocksTableModel.addRow(rowData);
                        }))
                        .exceptionally(throwable -> {
                            System.err.println("Error loading stock " + symbol + ": " + throwable.getMessage());
                            return null;
                        }))
                .toArray(CompletableFuture[]::new);

        // When all futures complete, re-enable refresh button if needed
        CompletableFuture.allOf(futures).thenRun(() -> SwingUtilities.invokeLater(() -> {
            // Optional: could add a loading indicator here
        }));
    }

    private void loadStockSymbols() {
        try {
            List<Stock> stocks = dbManager.getAllStocks();
            symbolComboBox.removeAllItems();
            for (Stock stock : stocks) {
                symbolComboBox.addItem(stock.getSymbol());
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load stock symbols: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadUserPortfolios() {
        portfolioComboBox.removeAllItems();
        try {
            if (app.getCurrentUser() == null) {
                return;
            }
            List<Portfolio> portfolios = dbManager.getPortfoliosByUserId(app.getCurrentUser().getId());
            for (Portfolio p : portfolios) {
                portfolioComboBox.addItem(p);
            }
            if (portfolios.size() > 0) {
                portfolioComboBox.setSelectedIndex(0);
                loadTransactionsForSelectedPortfolio();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load portfolios: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refreshPortfolios() {
        if (app.getCurrentUser() != null) {
            loadUserPortfolios();
        }
    }

    private void loadTransactionsForSelectedPortfolio() {
        Portfolio selectedPortfolio = (Portfolio) portfolioComboBox.getSelectedItem();
        if (selectedPortfolio == null) {
            transactionsTableModel.setRowCount(0);
            return;
        }
        try {
            List<Transaction> transactions = tradeExecutor.getPortfolioTradeHistory(selectedPortfolio.getId());
            transactionsTableModel.setRowCount(0);
            for (Transaction t : transactions) {
                Object[] rowData = {
                    t.getId(),
                    t.getStockSymbol(),
                    t.getType(),
                    t.getQuantity(),
                    String.format("%.2f", t.getPrice()),
                    String.format("%.2f", t.getTotalAmount()),
                    t.getOrderType(),
                    t.getTimestamp(),
                    t.getStatus()
                };
                transactionsTableModel.addRow(rowData);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load transactions: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void executeTrade(boolean isBuy) {
        Portfolio selectedPortfolio = (Portfolio) portfolioComboBox.getSelectedItem();
        if (selectedPortfolio == null) {
            JOptionPane.showMessageDialog(this, "Please select a portfolio first.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String symbol = (String) symbolComboBox.getSelectedItem();
        if (symbol == null || symbol.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a stock symbol.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double quantity;
        try {
            quantity = Double.parseDouble(quantityField.getText().trim());
            if (quantity <= 0) {
                JOptionPane.showMessageDialog(this, "Quantity must be positive.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid quantity.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!isBuy) {
            quantity = -quantity;
        }

        try {
            Transaction transaction = tradeExecutor.executeTrade(app.getCurrentUser().getId(), selectedPortfolio.getId(), symbol, quantity, "MARKET");
            if (transaction != null) {
                JOptionPane.showMessageDialog(this, "Trade executed successfully.", "Info", JOptionPane.INFORMATION_MESSAGE);
                loadTransactionsForSelectedPortfolio();
                loadStockQuotes();
            } else {
                JOptionPane.showMessageDialog(this, "Trade execution failed.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error executing trade: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
