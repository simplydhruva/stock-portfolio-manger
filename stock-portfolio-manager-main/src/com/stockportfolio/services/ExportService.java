package com.stockportfolio.services;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.stockportfolio.model.Portfolio;
import com.stockportfolio.model.Position;
import com.stockportfolio.model.Transaction;
import com.stockportfolio.services.analytics.AIAnalytics;
import com.stockportfolio.utils.DatabaseManager;

public class ExportService {
    private DatabaseManager dbManager;
    private AIAnalytics aiAnalytics;

    public ExportService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.aiAnalytics = new AIAnalytics();
    }

    public void exportTradeHistory(int userId) {
        try {
            List<Transaction> transactions = dbManager.getTransactionsByUserId(userId);

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Export Trade History");
            fileChooser.setSelectedFile(new java.io.File("trade_history.csv"));
            FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV Files", "csv");
            fileChooser.setFileFilter(filter);

            int result = fileChooser.showSaveDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                String filePath = fileChooser.getSelectedFile().getAbsolutePath();
                if (!filePath.toLowerCase().endsWith(".csv")) {
                    filePath += ".csv";
                }

                exportTradeHistoryToCSV(transactions, filePath);
                JOptionPane.showMessageDialog(null,
                    "Trade history exported successfully to: " + filePath,
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (SQLException | IOException e) {
            JOptionPane.showMessageDialog(null,
                "Error exporting trade history: " + e.getMessage(),
                "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void exportPortfolioReport(int userId) {
        try {
            List<Portfolio> portfolios = dbManager.getPortfoliosByUserId(userId);

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Export Portfolio Report");
            fileChooser.setSelectedFile(new java.io.File("portfolio_report.csv"));
            FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV Files", "csv");
            fileChooser.setFileFilter(filter);

            int result = fileChooser.showSaveDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                String filePath = fileChooser.getSelectedFile().getAbsolutePath();
                if (!filePath.toLowerCase().endsWith(".csv")) {
                    filePath += ".csv";
                }

                exportPortfolioReportToCSV(portfolios, filePath);
                JOptionPane.showMessageDialog(null,
                    "Portfolio report exported successfully to: " + filePath,
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (SQLException | IOException e) {
            JOptionPane.showMessageDialog(null,
                "Error exporting portfolio report: " + e.getMessage(),
                "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void exportAnalyticsReport(int userId) {
        try {
            List<Portfolio> portfolios = dbManager.getPortfoliosByUserId(userId);

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Export Analytics Report");
            fileChooser.setSelectedFile(new java.io.File("analytics_report.csv"));
            FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV Files", "csv");
            fileChooser.setFileFilter(filter);

            int result = fileChooser.showSaveDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                String filePath = fileChooser.getSelectedFile().getAbsolutePath();
                if (!filePath.toLowerCase().endsWith(".csv")) {
                    filePath += ".csv";
                }

                exportAnalyticsReportToCSV(portfolios, filePath);
                JOptionPane.showMessageDialog(null,
                    "Analytics report exported successfully to: " + filePath,
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (SQLException | IOException e) {
            JOptionPane.showMessageDialog(null,
                "Error exporting analytics report: " + e.getMessage(),
                "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportTradeHistoryToCSV(List<Transaction> transactions, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write CSV header
            writer.write("Date,Symbol,Type,Quantity,Price,Total Value,Portfolio ID,Status\n");

            // Write transaction data
            for (Transaction transaction : transactions) {
                writer.write(String.format("%s,%s,%s,%d,%.2f,%.2f,%d,%s\n",
                    transaction.getTimestamp().toString(),
                    transaction.getSymbol(),
                    transaction.getType(),
                    transaction.getQuantity(),
                    transaction.getPrice(),
                    transaction.getTotalAmount(),
                    transaction.getPortfolioId(),
                    transaction.getStatus()));
            }
        }
    }

    private void exportPortfolioReportToCSV(List<Portfolio> portfolios, String filePath) throws IOException, SQLException {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write CSV header
            writer.write("Portfolio Name,Total Value,Total Cost Basis,Total P&L,P&L %,Position Count\n");

            // Write portfolio data
            for (Portfolio portfolio : portfolios) {
                double pnlPercentage = portfolio.getTotalCostBasis() > 0 ?
                    (portfolio.getTotalPnL() / portfolio.getTotalCostBasis()) * 100 : 0.0;

                List<Position> positions = dbManager.getPositionsByPortfolioId(portfolio.getId());

                writer.write(String.format("%s,%.2f,%.2f,%.2f,%.2f%%,%d\n",
                    portfolio.getName(),
                    portfolio.getTotalValue(),
                    portfolio.getTotalCostBasis(),
                    portfolio.getTotalPnL(),
                    pnlPercentage,
                    positions.size()));
            }

            // Add detailed positions section
            writer.write("\n\nDetailed Positions\n");
            writer.write("Portfolio,Symbol,Quantity,Current Price,Total Value,Cost Basis,P&L,P&L %\n");

            for (Portfolio portfolio : portfolios) {
                List<Position> positions = dbManager.getPositionsByPortfolioId(portfolio.getId());

                for (Position position : positions) {
                    double totalValue = position.getQuantity() * position.getCurrentPrice();
                    double pnl = totalValue - position.getTotalCost();
                    double pnlPercentage = position.getTotalCost() > 0 ?
                        (pnl / position.getTotalCost()) * 100 : 0.0;

                    writer.write(String.format("%s,%s,%d,%.2f,%.2f,%.2f,%.2f,%.2f%%\n",
                        portfolio.getName(),
                        position.getStockSymbol(),
                        position.getQuantity(),
                        position.getCurrentPrice(),
                        totalValue,
                        position.getTotalCost(),
                        pnl,
                        pnlPercentage));
                }
            }
        }
    }

    private void exportAnalyticsReportToCSV(List<Portfolio> portfolios, String filePath) throws IOException, SQLException {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write CSV header
            writer.write("Portfolio Name,Risk Level,Volatility,Sharpe Ratio,Expected Return,Expected Risk\n");

            // Write analytics data
            for (Portfolio portfolio : portfolios) {
                List<Position> positions = dbManager.getPositionsByPortfolioId(portfolio.getId());

                if (!positions.isEmpty()) {
                    // Calculate weights for risk assessment
                    Map<String, Double> weights = calculateWeights(positions);
                    AIAnalytics.RiskAssessment risk = aiAnalytics.assessRisk(weights);

                    // Get optimization data
                    AIAnalytics.PortfolioOptimization optimization =
                        aiAnalytics.optimizePortfolio(String.valueOf(portfolio.getId())).join();

                    writer.write(String.format("%s,%s,%.2f,%.2f,%.2f,%.2f\n",
                        portfolio.getName(),
                        risk.getRiskLevel(),
                        risk.getVolatility() * 100,
                        risk.getSharpeRatio(),
                        optimization.getExpectedReturn() * 100,
                        optimization.getExpectedRisk() * 100));
                } else {
                    writer.write(String.format("%s,No positions,No data,No data,No data,No data\n",
                        portfolio.getName()));
                }
            }

            // Add recommendations section
            writer.write("\n\nOptimization Recommendations\n");
            writer.write("Portfolio,Symbol,Action,Description\n");

            for (Portfolio portfolio : portfolios) {
                List<Position> positions = dbManager.getPositionsByPortfolioId(portfolio.getId());

                if (!positions.isEmpty()) {
                    AIAnalytics.PortfolioOptimization optimization =
                        aiAnalytics.optimizePortfolio(String.valueOf(portfolio.getId())).join();

                    List<AIAnalytics.RebalanceRecommendation> recommendations = optimization.getRecommendations();
                    if (recommendations != null) {
                        for (AIAnalytics.RebalanceRecommendation rec : recommendations) {
                            writer.write(String.format("%s,%s,%s,%s\n",
                                portfolio.getName(),
                                rec.getSymbol(),
                                rec.getAction(),
                                rec.getDescription().replace(",", ";"))); // Replace commas to avoid CSV issues
                        }
                    }
                }
            }
        }
    }

    private Map<String, Double> calculateWeights(List<Position> positions) {
        Map<String, Double> weights = new java.util.HashMap<>();
        double totalValue = positions.stream()
            .mapToDouble(p -> p.getQuantity() * p.getCurrentPrice())
            .sum();

        for (Position position : positions) {
            double positionValue = position.getQuantity() * position.getCurrentPrice();
            double weight = totalValue > 0 ? positionValue / totalValue : 0.0;
            weights.put(position.getStockSymbol(), weight);
        }

        return weights;
    }


}
