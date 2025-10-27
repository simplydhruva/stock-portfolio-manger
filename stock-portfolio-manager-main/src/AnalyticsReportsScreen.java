import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.stockportfolio.model.Portfolio;
import com.stockportfolio.model.Position;
import com.stockportfolio.services.ExportService;
import com.stockportfolio.services.analytics.AIAnalytics;
import com.stockportfolio.utils.DatabaseManager;

public class AnalyticsReportsScreen extends JPanel {
    private App app;
    private DatabaseManager dbManager;
    private AIAnalytics aiAnalytics;

    private JTabbedPane tabbedPane;
    private JTextArea portfolioAnalysisArea;
    private JTextArea optimizationArea;
    private JTextArea rebalanceArea;

    public AnalyticsReportsScreen(App app, DatabaseManager dbManager) {
        this.app = app;
        this.dbManager = dbManager;
        this.aiAnalytics = new AIAnalytics();
        initializeUI();
        loadAnalyticsData();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // Title
        add(new JLabel("Portfolio Analytics & Reports", JLabel.CENTER), BorderLayout.NORTH);

        // Tabbed pane for different analytics views
        tabbedPane = new JTabbedPane();

        // Portfolio Analysis Tab
        JPanel analysisPanel = new JPanel(new BorderLayout());
        portfolioAnalysisArea = new JTextArea(20, 50);
        portfolioAnalysisArea.setEditable(false);
        JScrollPane analysisScroll = new JScrollPane(portfolioAnalysisArea);
        analysisPanel.add(analysisScroll, BorderLayout.CENTER);

        JButton refreshAnalysisBtn = new JButton("Refresh Analysis");
        refreshAnalysisBtn.addActionListener(e -> loadPortfolioAnalysis());
        analysisPanel.add(refreshAnalysisBtn, BorderLayout.SOUTH);

        tabbedPane.addTab("Portfolio Analysis", analysisPanel);

        // Optimization Tab
        JPanel optimizationPanel = new JPanel(new BorderLayout());
        optimizationArea = new JTextArea(20, 50);
        optimizationArea.setEditable(false);
        JScrollPane optimizationScroll = new JScrollPane(optimizationArea);
        optimizationPanel.add(optimizationScroll, BorderLayout.CENTER);

        JButton optimizeBtn = new JButton("Run Optimization");
        optimizeBtn.addActionListener(e -> runPortfolioOptimization());
        optimizationPanel.add(optimizeBtn, BorderLayout.SOUTH);

        tabbedPane.addTab("Portfolio Optimization", optimizationPanel);

        // Rebalancing Tab
        JPanel rebalancePanel = new JPanel(new BorderLayout());
        rebalanceArea = new JTextArea(20, 50);
        rebalanceArea.setEditable(false);
        JScrollPane rebalanceScroll = new JScrollPane(rebalanceArea);
        rebalancePanel.add(rebalanceScroll, BorderLayout.CENTER);

        JButton rebalanceBtn = new JButton("Check Rebalancing");
        rebalanceBtn.addActionListener(e -> checkRebalancingRecommendations());
        rebalancePanel.add(rebalanceBtn, BorderLayout.SOUTH);

        tabbedPane.addTab("Rebalancing", rebalancePanel);

        // Performance Charts Tab
        JPanel chartsPanel = new JPanel(new BorderLayout());
        AdvancedChartPanel chartPanel = new AdvancedChartPanel();
        chartPanel.setApp(app);
        chartPanel.setDbManager(dbManager);
        chartsPanel.add(chartPanel, BorderLayout.CENTER);

        JButton refreshChartsBtn = new JButton("Refresh Charts");
        refreshChartsBtn.addActionListener(e -> chartPanel.refreshCharts());
        chartsPanel.add(refreshChartsBtn, BorderLayout.SOUTH);

        tabbedPane.addTab("Advanced Charts", chartsPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // Bottom panel with navigation
        JPanel bottomPanel = new JPanel(new FlowLayout());

        JButton exportPortfolioCsvBtn = new JButton("Export Portfolio CSV");
        exportPortfolioCsvBtn.addActionListener(e -> {
            if (app.getCurrentUser() != null) {
                ExportService exportService = new ExportService(dbManager);
                exportService.exportPortfolioReport(app.getCurrentUser().getId());
            }
        });
        bottomPanel.add(exportPortfolioCsvBtn);

        JButton exportAnalyticsCsvBtn = new JButton("Export Analytics CSV");
        exportAnalyticsCsvBtn.addActionListener(e -> {
            if (app.getCurrentUser() != null) {
                ExportService exportService = new ExportService(dbManager);
                exportService.exportAnalyticsReport(app.getCurrentUser().getId());
            }
        });
        bottomPanel.add(exportAnalyticsCsvBtn);

        JButton backBtn = new JButton("Back to Dashboard");
        backBtn.addActionListener(e -> app.showScreen("DASHBOARD"));
        bottomPanel.add(backBtn);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void loadAnalyticsData() {
        loadPortfolioAnalysis();
        runPortfolioOptimization();
        checkRebalancingRecommendations();
    }

    private void loadPortfolioAnalysis() {
        try {
            if (app.getCurrentUser() == null) {
                portfolioAnalysisArea.setText("Please log in to view portfolio analysis.");
                return;
            }
            // Get current user's portfolios
            List<Portfolio> portfolios = dbManager.getPortfoliosByUserId(app.getCurrentUser().getId());

            if (portfolios.isEmpty()) {
                portfolioAnalysisArea.setText("No portfolios found. Create a portfolio first.");
                return;
            }

            StringBuilder analysis = new StringBuilder();
            analysis.append("PORTFOLIO ANALYSIS REPORT\n");
            analysis.append("========================\n\n");

            // Collect all unique stock symbols from user's positions for real-time quotes
            java.util.Set<String> symbols = new java.util.HashSet<>();
            for (Portfolio portfolio : portfolios) {
                List<Position> positions = dbManager.getPositionsByPortfolioId(portfolio.getId());
                for (Position position : positions) {
                    symbols.add(position.getSymbol());
                }
            }

            // Fetch real-time quotes for all symbols
            java.util.Map<String, com.stockportfolio.services.api.RealTimeStockAPI.StockQuote> quotes = new java.util.HashMap<>();
            for (String symbol : symbols) {
                try {
                    com.stockportfolio.services.api.RealTimeStockAPI.StockQuote quote = new com.stockportfolio.services.api.RealTimeStockAPI().getStockQuote(symbol).get();
                    if (quote != null) {
                        quotes.put(symbol, quote);
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching quote for " + symbol + ": " + e.getMessage());
                }
            }

            for (Portfolio portfolio : portfolios) {
                // Calculate real-time portfolio metrics
                double realTimeTotalValue = 0.0;
                double totalCostBasis = 0.0;
                double todaysPnL = 0.0;

                List<Position> positions = dbManager.getPositionsByPortfolioId(portfolio.getId());

                for (Position position : positions) {
                    double currentPrice = position.getCurrentPrice();
                    com.stockportfolio.services.api.RealTimeStockAPI.StockQuote quote = quotes.get(position.getSymbol());
                    if (quote != null) {
                        currentPrice = quote.getCurrentPrice();
                        // Calculate today's P&L as change from previous close
                        double previousClose = quote.getPreviousClose();
                        todaysPnL += (currentPrice - previousClose) * position.getQuantity();
                    }

                    realTimeTotalValue += position.getQuantity() * currentPrice;
                    totalCostBasis += position.getQuantity() * position.getAverageCost();
                }

                double totalPnL = realTimeTotalValue - totalCostBasis;
                double pnlPercentage = totalCostBasis > 0 ? (totalPnL / totalCostBasis) * 100 : 0.0;

                analysis.append("Portfolio: ").append(portfolio.getName()).append("\n");
                analysis.append("Real-time Total Value: $").append(String.format("%.2f", realTimeTotalValue)).append("\n");
                analysis.append("Total Cost Basis: $").append(String.format("%.2f", totalCostBasis)).append("\n");
                analysis.append("Total P&L: $").append(String.format("%.2f", totalPnL)).append("\n");
                analysis.append("P&L %: ").append(String.format("%.2f%%", pnlPercentage)).append("\n");
                analysis.append("Today's P&L: $").append(String.format("%.2f", todaysPnL)).append("\n\n");

                if (!positions.isEmpty()) {
                    // Run AI analysis
                    Map<String, Object> aiAnalysis = aiAnalytics.analyzePortfolio(portfolio, positions);
                    analysis.append("AI Analysis:\n");
                    analysis.append("- Risk Level: ").append(aiAnalysis.get("riskLevel")).append("\n");
                    analysis.append("- Recommendations:\n");

                    @SuppressWarnings("unchecked")
                    List<String> recommendations = (List<String>) aiAnalysis.get("recommendations");
                    for (String rec : recommendations) {
                        analysis.append("  • ").append(rec).append("\n");
                    }
                }
                analysis.append("\n");
            }

            portfolioAnalysisArea.setText(analysis.toString());

        } catch (Exception e) {
            portfolioAnalysisArea.setText("Error loading portfolio analysis: " + e.getMessage());
        }
    }

    private void runPortfolioOptimization() {
        try {
            if (app.getCurrentUser() == null) {
                optimizationArea.setText("Please log in to run portfolio optimization.");
                return;
            }
            List<Portfolio> portfolios = dbManager.getPortfoliosByUserId(app.getCurrentUser().getId());

            if (portfolios.isEmpty()) {
                optimizationArea.setText("No portfolios found for optimization.");
                return;
            }

            StringBuilder optimization = new StringBuilder();
            optimization.append("PORTFOLIO OPTIMIZATION RESULTS\n");
            optimization.append("==============================\n\n");

            for (Portfolio portfolio : portfolios) {
                optimization.append("Optimizing Portfolio: ").append(portfolio.getName()).append("\n");

                // Run AI optimization asynchronously
                CompletableFuture<AIAnalytics.PortfolioOptimization> futureOptimization =
                    aiAnalytics.optimizePortfolio(String.valueOf(portfolio.getId()));

                futureOptimization.thenAccept(opt -> {
                    SwingUtilities.invokeLater(() -> {
                        StringBuilder result = new StringBuilder();
                        result.append("Reasoning: ").append(opt.getReasoning()).append("\n");
                        result.append("Expected Return: ").append(String.format("%.2f%%", opt.getExpectedReturn() * 100)).append("\n");
                        result.append("Expected Risk: ").append(String.format("%.2f%%", opt.getExpectedRisk() * 100)).append("\n");
                        result.append("Sharpe Ratio: ").append(String.format("%.2f", opt.getSharpeRatio())).append("\n\n");

                        // Show optimized allocation
                        result.append("Optimized Allocation:\n");
                        opt.getOptimizedAllocation().forEach((symbol, percentage) ->
                            result.append("- ").append(symbol).append(": ")
                                .append(String.format("%.1f%%", percentage * 100)).append("\n")
                        );
                        result.append("\n");

                        List<AIAnalytics.RebalanceRecommendation> recommendations = opt.getRecommendations();
                        if (recommendations != null && !recommendations.isEmpty()) {
                            result.append("Recommendations:\n");
                            for (AIAnalytics.RebalanceRecommendation rec : recommendations) {
                                result.append("- ").append(rec.getSymbol()).append(" (")
                                    .append(rec.getAction()).append("): ").append(rec.getDescription()).append("\n");
                            }
                        }
                        result.append("\n");

                        optimizationArea.setText(optimizationArea.getText() + result.toString());
                    });
                }).exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> {
                        optimizationArea.setText(optimizationArea.getText() +
                            "Error running optimization: " + ex.getMessage() + "\n\n");
                    });
                    return null;
                });
            }

            optimizationArea.setText(optimization.toString());

        } catch (Exception e) {
            optimizationArea.setText("Error running portfolio optimization: " + e.getMessage());
        }
    }

    private void checkRebalancingRecommendations() {
        try {
            if (app.getCurrentUser() == null) {
                rebalanceArea.setText("Please log in to check rebalancing recommendations.");
                return;
            }
            List<Portfolio> portfolios = dbManager.getPortfoliosByUserId(app.getCurrentUser().getId());

            if (portfolios.isEmpty()) {
                rebalanceArea.setText("No portfolios found for rebalancing analysis.");
                return;
            }

            StringBuilder rebalance = new StringBuilder();
            rebalance.append("REBALANCING RECOMMENDATIONS\n");
            rebalance.append("=============================\n\n");

            for (Portfolio portfolio : portfolios) {
                List<Position> positions = dbManager.getPositionsByPortfolioId(portfolio.getId());

                if (!positions.isEmpty()) {
                    AIAnalytics.RebalanceRecommendation recommendation =
                        aiAnalytics.getRebalanceRecommendation(positions);

                    rebalance.append("Portfolio: ").append(portfolio.getName()).append("\n");
                    rebalance.append("Symbol: ").append(recommendation.getSymbol()).append("\n");
                    rebalance.append("Action: ").append(recommendation.getAction()).append("\n");
                    rebalance.append("Description: ").append(recommendation.getDescription()).append("\n");
                    rebalance.append("Difference: ").append(String.format("%.2f", recommendation.getDifference())).append("\n\n");
                }
            }

            rebalanceArea.setText(rebalance.toString());

        } catch (Exception e) {
            rebalanceArea.setText("Error checking rebalancing recommendations: " + e.getMessage());
        }
    }

    private double calculatePnLPercentage(Portfolio portfolio) {
        if (portfolio.getTotalCostBasis() == 0) return 0.0;
        return (portfolio.getTotalPnL() / portfolio.getTotalCostBasis()) * 100;
    }
}
