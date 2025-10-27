import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import com.stockportfolio.model.Portfolio;
import com.stockportfolio.model.Position;
import com.stockportfolio.services.analytics.AIAnalytics;
import com.stockportfolio.utils.DatabaseManager;

public class RiskComparisonPanel extends JPanel {
    private List<Portfolio> portfolios;
    private DatabaseManager dbManager;
    private AIAnalytics aiAnalytics;

    public RiskComparisonPanel(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.aiAnalytics = new AIAnalytics();
        this.portfolios = null;
        setBackground(Color.WHITE);
    }

    public void setPortfolios(List<Portfolio> portfolios) {
        this.portfolios = portfolios;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (portfolios == null || portfolios.isEmpty()) {
            g.drawString("Select portfolios to compare risk metrics", 50, 50);
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Draw risk comparison chart
        drawRiskChart(g2d, 50, 50, width - 100, height - 100);
    }

    private void drawRiskChart(Graphics2D g2d, int x, int y, int width, int height) {
        g2d.setColor(Color.BLACK);
        g2d.drawString("Portfolio Risk Comparison", x, y - 10);

        if (portfolios.isEmpty()) return;

        int barWidth = width / portfolios.size();
        int chartHeight = height - 40;

        int barX = x;
        Color[] colors = {Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN};
        int colorIndex = 0;

        for (Portfolio portfolio : portfolios) {
            try {
                List<Position> positions = dbManager.getPositionsByPortfolioId(portfolio.getId());
                if (!positions.isEmpty()) {
                    Map<String, Double> weights = calculateWeights(positions);
                    AIAnalytics.RiskAssessment risk = aiAnalytics.assessRisk(weights);

                    // Draw risk level indicator
                    Color riskColor = getRiskColor(risk.getRiskLevel());
                    g2d.setColor(riskColor);
                    g2d.fillRect(barX, y + 50, barWidth - 5, 30);

                    g2d.setColor(Color.BLACK);
                    g2d.drawRect(barX, y + 50, barWidth - 5, 30);
                    g2d.drawString(risk.getRiskLevel(), barX + 5, y + 70);

                    // Draw metrics
                    g2d.drawString(String.format("Vol: %.1f%%", risk.getVolatility() * 100),
                        barX, y + 100);
                    g2d.drawString(String.format("Sharpe: %.2f", risk.getSharpeRatio()),
                        barX, y + 120);

                    // Draw portfolio name
                    g2d.drawString(portfolio.getName(), barX, y + chartHeight + 15);
                }
            } catch (SQLException e) {
                g2d.setColor(Color.GRAY);
                g2d.drawString("Error", barX + 5, y + 70);
            }

            barX += barWidth;
            colorIndex++;
        }
    }

    private Color getRiskColor(String riskLevel) {
        switch (riskLevel.toLowerCase()) {
            case "low": return Color.GREEN;
            case "medium": return Color.YELLOW;
            case "high": return Color.RED;
            default: return Color.GRAY;
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
