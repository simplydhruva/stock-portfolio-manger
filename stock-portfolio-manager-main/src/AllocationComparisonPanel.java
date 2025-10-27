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
import com.stockportfolio.utils.DatabaseManager;

public class AllocationComparisonPanel extends JPanel {
    private List<Portfolio> portfolios;
    private DatabaseManager dbManager;

    public AllocationComparisonPanel(DatabaseManager dbManager) {
        this.dbManager = dbManager;
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
            g.drawString("Select portfolios to compare allocations", 50, 50);
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Draw allocation comparison
        drawAllocationChart(g2d, 50, 50, width - 100, height - 100);
    }

    private void drawAllocationChart(Graphics2D g2d, int x, int y, int width, int height) {
        g2d.setColor(Color.BLACK);
        g2d.drawString("Portfolio Allocation Comparison", x, y - 10);

        if (portfolios.isEmpty()) return;

        int portfolioHeight = height / portfolios.size();
        int currentY = y + 20;

        Color[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA, Color.CYAN};

        for (Portfolio portfolio : portfolios) {
            try {
                List<Position> positions = dbManager.getPositionsByPortfolioId(portfolio.getId());
                if (!positions.isEmpty()) {
                    g2d.setColor(Color.BLACK);
                    g2d.drawString(portfolio.getName(), x, currentY - 5);

                    Map<String, Double> weights = calculateWeights(positions);
                    double totalValue = positions.stream()
                        .mapToDouble(p -> p.getQuantity() * p.getCurrentPrice())
                        .sum();

                    int barX = x + 100;
                    int colorIndex = 0;
                    int legendY = currentY + 15;

                    for (Map.Entry<String, Double> entry : weights.entrySet()) {
                        double weight = entry.getValue();
                        int barWidth = (int) (weight * (width - 150));

                        g2d.setColor(colors[colorIndex % colors.length]);
                        g2d.fillRect(barX, currentY, barWidth, 20);

                        // Legend
                        g2d.fillRect(x + width - 80, legendY, 15, 15);
                        g2d.setColor(Color.BLACK);
                        g2d.drawString(String.format("%s: %.1f%%", entry.getKey(), weight * 100),
                            x + width - 60, legendY + 12);

                        barX += barWidth;
                        legendY += 20;
                        colorIndex++;
                    }
                }
            } catch (SQLException e) {
                g2d.setColor(Color.BLACK);
                g2d.drawString("Error loading " + portfolio.getName(), x, currentY + 15);
            }

            currentY += portfolioHeight;
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
