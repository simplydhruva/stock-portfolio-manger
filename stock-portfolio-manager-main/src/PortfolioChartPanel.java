import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import com.stockportfolio.model.Portfolio;
import com.stockportfolio.model.Position;
import com.stockportfolio.services.analytics.AIAnalytics;
import com.stockportfolio.utils.DatabaseManager;

public class PortfolioChartPanel extends JPanel {
    private App app;
    private DatabaseManager dbManager;
    private AIAnalytics aiAnalytics;

    private Map<String, Double> portfolioWeights;
    private Map<String, Double> performanceData;

    public PortfolioChartPanel() {
        this.app = null; // Will be set by parent
        this.dbManager = null; // Will be set by parent
        this.aiAnalytics = new AIAnalytics();
        this.portfolioWeights = new HashMap<>();
        this.performanceData = new HashMap<>();
        setLayout(new BorderLayout());
    }

    public void setApp(App app) {
        this.app = app;
    }

    public void setDbManager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void refreshCharts() {
        if (app != null && dbManager != null) {
            loadChartData();
            repaint();
        }
    }

    private void loadChartData() {
        try {
            if (app.getCurrentUser() == null) {
                System.err.println("No user logged in. Cannot load chart data.");
                return;
            }
            List<Portfolio> portfolios = dbManager.getPortfoliosByUserId(app.getCurrentUser().getId());

            portfolioWeights.clear();
            performanceData.clear();

            for (Portfolio portfolio : portfolios) {
                List<Position> positions = dbManager.getPositionsByPortfolioId(portfolio.getId());

                double totalValue = positions.stream()
                    .mapToDouble(p -> p.getQuantity() * p.getCurrentPrice())
                    .sum();

                // Calculate weights for each position
                for (Position position : positions) {
                    double positionValue = position.getQuantity() * position.getCurrentPrice();
                    double weight = totalValue > 0 ? positionValue / totalValue : 0.0;
                    portfolioWeights.put(position.getStockSymbol(), weight);
                }

                // Sample performance data (in a real app, this would come from historical data)
                performanceData.put(portfolio.getName(), calculatePnLPercentage(portfolio));
            }

        } catch (Exception e) {
            System.err.println("Error loading chart data: " + e.getMessage());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Draw portfolio allocation pie chart
        if (!portfolioWeights.isEmpty()) {
            drawPieChart(g2d, width / 2 - 150, 50, 200, portfolioWeights, "Portfolio Allocation");
        }

        // Draw performance bar chart
        if (!performanceData.isEmpty()) {
            drawBarChart(g2d, width / 2 + 100, 50, 200, 200, performanceData, "Portfolio Performance (%)");
        }

        // Draw risk assessment
        if (!portfolioWeights.isEmpty()) {
            AIAnalytics.RiskAssessment risk = aiAnalytics.assessRisk(portfolioWeights);
            drawRiskIndicator(g2d, 50, height - 100, 300, 80, risk);
        }
    }

    private void drawPieChart(Graphics2D g2d, int x, int y, int size, Map<String, Double> data, String title) {
        g2d.setColor(Color.BLACK);
        g2d.drawString(title, x, y - 10);

        double total = data.values().stream().mapToDouble(Double::doubleValue).sum();
        double startAngle = 0;

        Color[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA, Color.CYAN};
        int colorIndex = 0;

        int legendY = y + size + 20;

        for (Map.Entry<String, Double> entry : data.entrySet()) {
            double percentage = entry.getValue();
            double angle = (percentage / total) * 360;

            g2d.setColor(colors[colorIndex % colors.length]);
            g2d.fillArc(x, y, size, size, (int) startAngle, (int) angle);

            // Draw legend
            g2d.fillRect(x + size + 20, legendY, 15, 15);
            g2d.setColor(Color.BLACK);
            g2d.drawString(String.format("%s: %.1f%%", entry.getKey(), percentage * 100),
                          x + size + 45, legendY + 12);

            legendY += 20;
            startAngle += angle;
            colorIndex++;
        }

        // Draw border
        g2d.setColor(Color.BLACK);
        g2d.drawArc(x, y, size, size, 0, 360);
    }

    private void drawBarChart(Graphics2D g2d, int x, int y, int width, int height, Map<String, Double> data, String title) {
        g2d.setColor(Color.BLACK);
        g2d.drawString(title, x, y - 10);

        int barWidth = width / data.size();
        int maxBarHeight = height - 40;
        double maxValue = Math.max(Math.abs(data.values().stream().mapToDouble(Math::abs).max().orElse(1)), 1);

        int barX = x;
        Color[] colors = {Color.BLUE, Color.GREEN, Color.ORANGE, Color.RED};
        int colorIndex = 0;

        for (Map.Entry<String, Double> entry : data.entrySet()) {
            double value = entry.getValue();
            int barHeight = (int) ((Math.abs(value) / maxValue) * maxBarHeight);

            g2d.setColor(colors[colorIndex % colors.length]);
            if (value >= 0) {
                g2d.fillRect(barX, y + height - barHeight, barWidth - 5, barHeight);
            } else {
                g2d.fillRect(barX, y + height - 20, barWidth - 5, barHeight);
            }

            // Draw value label
            g2d.setColor(Color.BLACK);
            g2d.drawString(String.format("%.1f%%", value), barX, y + height - barHeight - 5);

            // Draw bar label
            g2d.drawString(entry.getKey(), barX, y + height + 15);

            barX += barWidth;
            colorIndex++;
        }

        // Draw axes
        g2d.drawLine(x, y, x, y + height);
        g2d.drawLine(x, y + height, x + width, y + height);
    }

    private void drawRiskIndicator(Graphics2D g2d, int x, int y, int width, int height, AIAnalytics.RiskAssessment risk) {
        g2d.setColor(Color.BLACK);
        g2d.drawString("Risk Assessment", x, y - 10);

        // Draw risk level indicator
        String riskLevel = risk.getRiskLevel();
        Color riskColor = getRiskColor(riskLevel);

        g2d.setColor(riskColor);
        g2d.fillRect(x, y, width, height / 2);

        g2d.setColor(Color.BLACK);
        g2d.drawRect(x, y, width, height / 2);
        g2d.drawString("Risk Level: " + riskLevel, x + 10, y + 20);

        // Draw metrics
        g2d.drawString(String.format("Volatility: %.2f%%", risk.getVolatility() * 100), x, y + height / 2 + 20);
        g2d.drawString(String.format("Sharpe Ratio: %.2f", risk.getSharpeRatio()), x, y + height / 2 + 40);
    }

    private Color getRiskColor(String riskLevel) {
        switch (riskLevel.toLowerCase()) {
            case "low": return Color.GREEN;
            case "medium": return Color.YELLOW;
            case "high": return Color.RED;
            default: return Color.GRAY;
        }
    }

    private double calculatePnLPercentage(Portfolio portfolio) {
        if (portfolio.getTotalCostBasis() == 0) return 0.0;
        return (portfolio.getTotalPnL() / portfolio.getTotalCostBasis()) * 100;
    }
}
