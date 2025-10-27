import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.JPanel;

import com.stockportfolio.model.Portfolio;

public class PerformanceComparisonPanel extends JPanel {
    private List<Portfolio> portfolios;

    public PerformanceComparisonPanel() {
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
            g.drawString("Select portfolios to compare performance", 50, 50);
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Draw performance bar chart
        drawPerformanceChart(g2d, 50, 50, width - 100, height - 100);
    }

    private void drawPerformanceChart(Graphics2D g2d, int x, int y, int width, int height) {
        g2d.setColor(Color.BLACK);
        g2d.drawString("Portfolio Performance Comparison (%)", x, y - 10);

        if (portfolios.isEmpty()) return;

        int barWidth = width / portfolios.size();
        int chartHeight = height - 40;

        // Find max absolute return for scaling
        double maxReturn = portfolios.stream()
            .mapToDouble(p -> Math.abs(p.getTotalCost() > 0 ? (p.getTotalPnL() / p.getTotalCost()) * 100 : 0))
            .max().orElse(100);

        int barX = x;
        Color[] colors = {Color.BLUE, Color.GREEN, Color.ORANGE, Color.RED, Color.MAGENTA};
        int colorIndex = 0;

        for (Portfolio portfolio : portfolios) {
            double returnPct = portfolio.getTotalCost() > 0 ?
                (portfolio.getTotalPnL() / portfolio.getTotalCost()) * 100 : 0;

            int barHeight = (int) ((Math.abs(returnPct) / maxReturn) * (chartHeight - 40));
            int barY = y + chartHeight - barHeight;

            g2d.setColor(colors[colorIndex % colors.length]);
            g2d.fillRect(barX, barY, barWidth - 5, barHeight);

            // Draw value label
            g2d.setColor(Color.BLACK);
            g2d.drawString(String.format("%.1f%%", returnPct), barX, barY - 5);

            // Draw portfolio name
            g2d.drawString(portfolio.getName(), barX, y + chartHeight + 15);

            barX += barWidth;
            colorIndex++;
        }

        // Draw axes
        g2d.drawLine(x, y, x, y + chartHeight);
        g2d.drawLine(x, y + chartHeight, x + width, y + chartHeight);
    }
}
