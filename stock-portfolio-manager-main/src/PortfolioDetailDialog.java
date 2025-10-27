import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import com.stockportfolio.model.Portfolio;
import com.stockportfolio.model.Position;
import com.stockportfolio.model.Transaction;
import com.stockportfolio.services.analytics.AIAnalytics;
import com.stockportfolio.utils.DatabaseManager;

public class PortfolioDetailDialog extends JDialog {
    private App app;
    private DatabaseManager dbManager;
    private AIAnalytics aiAnalytics;
    private int portfolioId;
    private String portfolioName;
    private Portfolio portfolio;
    private AIAnalytics.PortfolioOptimization currentOptimization;

    private JTabbedPane tabbedPane;
    private JTextArea performanceArea;
    private JTable positionsTable;
    private JTextArea rebalancingArea;

    public PortfolioDetailDialog(App app, DatabaseManager dbManager, int portfolioId, String portfolioName) {
        super(app, "Portfolio Details: " + portfolioName, true);
        this.app = app;
        this.dbManager = dbManager;
        this.aiAnalytics = new AIAnalytics();
        this.portfolioId = portfolioId;
        this.portfolioName = portfolioName;

        try {
            this.portfolio = dbManager.getPortfolioById(portfolioId);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading portfolio: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        initializeUI();
        loadPortfolioData();
        setSize(800, 600);
        setLocationRelativeTo(app);
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // Title
        add(new JLabel("Portfolio: " + portfolioName, SwingConstants.CENTER), BorderLayout.NORTH);

        // Tabbed pane
        tabbedPane = new JTabbedPane();

        // Performance Tab
        JPanel performancePanel = createPerformancePanel();
        tabbedPane.addTab("Performance", performancePanel);

        // Positions Tab
        JPanel positionsPanel = createPositionsPanel();
        tabbedPane.addTab("Positions", positionsPanel);

        // Rebalancing Tab
        JPanel rebalancingPanel = createRebalancingPanel();
        tabbedPane.addTab("Rebalancing", rebalancingPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // Control buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadPortfolioData());
        buttonPanel.add(refreshButton);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createPerformancePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Performance metrics
        JPanel metricsPanel = new JPanel(new GridLayout(2, 4, 10, 5));
        metricsPanel.add(new JLabel("Total Value:"));
        metricsPanel.add(new JLabel("$" + String.format("%.2f", portfolio.getTotalValue())));
        metricsPanel.add(new JLabel("Total Cost:"));
        metricsPanel.add(new JLabel("$" + String.format("%.2f", portfolio.getTotalCost())));
        metricsPanel.add(new JLabel("Total P&L:"));
        metricsPanel.add(new JLabel("$" + String.format("%.2f", portfolio.getTotalPnL())));
        metricsPanel.add(new JLabel("Return %:"));
        double returnPct = portfolio.getTotalCost() > 0 ?
            (portfolio.getTotalPnL() / portfolio.getTotalCost()) * 100 : 0;
        metricsPanel.add(new JLabel(String.format("%.2f%%", returnPct)));

        panel.add(metricsPanel, BorderLayout.NORTH);

        // Performance chart area (text-based for now)
        performanceArea = new JTextArea(15, 50);
        performanceArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(performanceArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createPositionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Positions table
        String[] columnNames = {"Symbol", "Shares", "Avg Price", "Current Price", "Market Value", "P&L", "P&L %"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        positionsTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(positionsTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createRebalancingPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Rebalancing recommendations
        rebalancingArea = new JTextArea(20, 50);
        rebalancingArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(rebalancingArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Control buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton optimizeButton = new JButton("Run AI Optimization");
        optimizeButton.addActionListener(e -> runPortfolioOptimization());
        buttonPanel.add(optimizeButton);

        JButton applyRebalancingButton = new JButton("Apply Rebalancing");
        applyRebalancingButton.addActionListener(e -> applyRebalancing());
        buttonPanel.add(applyRebalancingButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadPortfolioData() {
        try {
            // Load positions
            List<Position> positions = dbManager.getPositionsByPortfolioId(portfolioId);
            DefaultTableModel tableModel = (DefaultTableModel) positionsTable.getModel();
            tableModel.setRowCount(0);

            for (Position position : positions) {
                // Get current price (mock for now)
                double currentPrice = getCurrentPrice(position.getSymbol());
                double marketValue = position.getQuantity() * currentPrice;
                double pnl = marketValue - (position.getQuantity() * position.getAveragePrice());
                double pnlPct = (position.getAveragePrice() > 0) ?
                    (pnl / (position.getQuantity() * position.getAveragePrice())) * 100 : 0;

                Object[] rowData = {
                        position.getSymbol(),
                        position.getQuantity(),
                        String.format("%.2f", position.getAveragePrice()),
                        String.format("%.2f", currentPrice),
                        String.format("%.2f", marketValue),
                        String.format("%.2f", pnl),
                        String.format("%.2f%%", pnlPct)
                };
                tableModel.addRow(rowData);
            }

            // Load performance data
            loadPerformanceData();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading portfolio data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadPerformanceData() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== PORTFOLIO PERFORMANCE ===\n\n");

        // Daily performance (mock data)
        sb.append("DAILY PERFORMANCE:\n");
        sb.append("Today: +2.45% (+$1,250.00)\n");
        sb.append("Yesterday: -1.23% (-$625.00)\n");
        sb.append("7-Day: +5.67% (+$2,890.00)\n");
        sb.append("30-Day: +12.34% (+$6,250.00)\n\n");

        // Monthly performance
        sb.append("MONTHLY PERFORMANCE:\n");
        sb.append("January: +8.45%\n");
        sb.append("February: -2.12%\n");
        sb.append("March: +15.67%\n");
        sb.append("April: +6.23%\n\n");

        // Risk metrics
        sb.append("RISK METRICS:\n");
        sb.append("Volatility (30-day): 18.45%\n");
        sb.append("Sharpe Ratio: 1.23\n");
        sb.append("Max Drawdown: -12.34%\n");
        sb.append("Beta: 0.87\n\n");

        // Benchmark comparison
        sb.append("BENCHMARK COMPARISON (vs S&P 500):\n");
        sb.append("1-Month: Portfolio +12.34% vs S&P +8.92%\n");
        sb.append("3-Month: Portfolio +18.45% vs S&P +12.67%\n");
        sb.append("YTD: Portfolio +25.67% vs S&P +18.34%\n");

        performanceArea.setText(sb.toString());
    }

    private void runPortfolioOptimization() {
        rebalancingArea.setText("Running AI portfolio optimization... Please wait.\n");

        aiAnalytics.optimizePortfolio(String.valueOf(portfolioId)).thenAccept(optimization -> {
            currentOptimization = optimization;
            SwingUtilities.invokeLater(() -> {
                StringBuilder sb = new StringBuilder();
                sb.append("=== AI PORTFOLIO OPTIMIZATION RESULTS ===\n\n");
                sb.append("Reasoning: ").append(optimization.getReasoning()).append("\n\n");

                sb.append("CURRENT ALLOCATION:\n");
                // Show current allocation
                try {
                    List<Position> positions = dbManager.getPositionsByPortfolioId(portfolioId);
                    double totalValue = positions.stream()
                        .mapToDouble(p -> p.getQuantity() * getCurrentPrice(p.getSymbol()))
                        .sum();

                    for (Position pos : positions) {
                        double value = pos.getQuantity() * getCurrentPrice(pos.getSymbol());
                        double weight = totalValue > 0 ? (value / totalValue) * 100 : 0;
                        sb.append(String.format("%s: %.1f%%\n", pos.getSymbol(), weight));
                    }
                } catch (SQLException e) {
                    sb.append("Error loading current positions\n");
                }

                sb.append("\nOPTIMIZED ALLOCATION:\n");
                optimization.getOptimizedAllocation().forEach((symbol, weight) ->
                    sb.append(String.format("%s: %.1f%%\n", symbol, weight * 100)));

                sb.append(String.format("\nExpected Return: %.1f%%\n", optimization.getExpectedReturn() * 100));
                sb.append(String.format("Expected Risk: %.1f%%\n", optimization.getExpectedRisk() * 100));
                sb.append(String.format("Sharpe Ratio: %.2f\n\n", optimization.getSharpeRatio()));

                sb.append("REBALANCING RECOMMENDATIONS:\n");
                optimization.getRecommendations().forEach(rec ->
                    sb.append(String.format("%s: %s - %s\n", rec.getSymbol(), rec.getAction(), rec.getDescription())));

                rebalancingArea.setText(sb.toString());
            });
        }).exceptionally(throwable -> {
            SwingUtilities.invokeLater(() -> {
                rebalancingArea.setText("Error running optimization: " + throwable.getMessage());
            });
            return null;
        });
    }

    private void applyRebalancing() {
        int option = JOptionPane.showConfirmDialog(this,
                "This will generate trading orders to rebalance your portfolio according to AI recommendations.\n" +
                "Do you want to proceed?",
                "Apply Rebalancing", JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            if (currentOptimization == null) {
                JOptionPane.showMessageDialog(this,
                        "Please run AI Optimization first.",
                        "No Optimization Data", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                List<Position> positions = dbManager.getPositionsByPortfolioId(portfolioId);
                double totalValue = positions.stream()
                        .mapToDouble(p -> p.getQuantity() * getCurrentPrice(p.getSymbol()))
                        .sum();

                for (AIAnalytics.RebalanceRecommendation rec : currentOptimization.getRecommendations()) {
                    if ("HOLD".equals(rec.getAction())) {
                        continue;
                    }

                    String symbol = rec.getSymbol();
                    String action = rec.getAction();
                    double diffWeight = Math.abs(rec.getDifference());
                    double tradeAmount = diffWeight * totalValue;
                    double currentPrice = getCurrentPrice(symbol);
                    double quantity = tradeAmount / currentPrice;

                    // Find existing position or create new
                    Position position = positions.stream()
                            .filter(p -> p.getSymbol().equals(symbol))
                            .findFirst()
                            .orElse(null);

                    if (position == null && "BUY".equals(action)) {
                        position = new Position();
                        position.setPortfolioId(portfolioId);
                        position.setSymbol(symbol);
                        position.setQuantity(0);
                        position.setAveragePrice(currentPrice);
                    }

                    if (position == null) {
                        continue; // No position to sell
                    }

                    // Create transaction
                    Transaction transaction = new Transaction();
                    if (app.getCurrentUser() != null) {
                        transaction.setUserId(app.getCurrentUser().getId());
                    } else {
                        JOptionPane.showMessageDialog(this, "No user logged in.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    transaction.setPortfolioId(portfolioId);
                    transaction.setStockSymbol(symbol);
                    transaction.setType(action);
                    transaction.setOrderType("MARKET");
                    transaction.setQuantity(quantity);
                    transaction.setPrice(currentPrice);
                    transaction.setTotalAmount(quantity * currentPrice);
                    transaction.setTimestamp(java.time.LocalDateTime.now());
                    transaction.setStatus("PENDING");

                    dbManager.saveTransaction(transaction);

                    // Update position quantity and average price
                    if ("BUY".equals(action)) {
                        double totalCost = position.getAveragePrice() * position.getQuantity() + quantity * currentPrice;
                        double newQuantity = position.getQuantity() + quantity;
                        position.setAveragePrice(totalCost / newQuantity);
                        position.setQuantity(newQuantity);
                    } else if ("SELL".equals(action)) {
                        double newQuantity = position.getQuantity() - quantity;
                        if (newQuantity <= 0) {
                            // Remove position if quantity zero or less
                            dbManager.deletePosition(position.getId());
                            continue;
                        } else {
                            position.setQuantity(newQuantity);
                        }
                    }

                    position.setCurrentPrice(currentPrice);
                    position.setTotalValue(position.getQuantity() * currentPrice);
                    position.setLastUpdated(java.time.LocalDateTime.now());

                    dbManager.updatePosition(position);
                }

                // Update portfolio totals
                double newTotalValue = dbManager.getPositionsByPortfolioId(portfolioId).stream()
                        .mapToDouble(p -> p.getQuantity() * getCurrentPrice(p.getSymbol()))
                        .sum();
                portfolio.setTotalValue(newTotalValue);
                portfolio.setUpdatedAt(java.time.LocalDateTime.now());
                dbManager.updatePortfolio(portfolio);

                JOptionPane.showMessageDialog(this,
                        "Rebalancing orders have been generated and saved.\n" +
                        "Please check your trading platform for execution details.",
                        "Rebalancing Applied", JOptionPane.INFORMATION_MESSAGE);

                loadPortfolioData();

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error applying rebalancing: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private double getCurrentPrice(String symbol) {
        // Mock current price - in real app, this would call RealTimeStockAPI
        return 100 + Math.random() * 100; // Random price between 100-200
    }
}
