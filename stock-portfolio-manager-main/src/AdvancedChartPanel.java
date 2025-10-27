import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.stockportfolio.model.Portfolio;
import com.stockportfolio.model.Position;
import com.stockportfolio.model.Transaction;
import com.stockportfolio.services.api.RealTimeStockAPI;
import com.stockportfolio.utils.DatabaseManager;

public class AdvancedChartPanel extends JPanel {
    // Inner class for candlestick data
    private static class CandlestickData {
        LocalDateTime timestamp;
        double open;
        double high;
        double low;
        double close;
        String symbol;

        CandlestickData(LocalDateTime timestamp, double open, double high, double low, double close, String symbol) {
            this.timestamp = timestamp;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.symbol = symbol;
        }
    }

    // Inner class for historical data
    private static class HistoricalData {
        LocalDateTime timestamp;
        double price;

        HistoricalData(LocalDateTime timestamp, double price) {
            this.timestamp = timestamp;
            this.price = price;
        }
    }

    private App app;
    private DatabaseManager dbManager;
    private RealTimeStockAPI stockAPI;

    private List<CandlestickData> candlestickData;
    private JButton refreshBtn;
    private ChartCanvas chartCanvas;
    private JComboBox<String> portfolioComboBox;
    private JComboBox<String> stockComboBox;
    private JComboBox<String> timeFrameComboBox;
    private List<Portfolio> portfolios;
    private List<String> stockSymbols;
    private List<HistoricalData> historicalData;

    public AdvancedChartPanel() {
        this.app = null; // Will be set by parent
        this.dbManager = null; // Will be set by parent
        this.stockAPI = new RealTimeStockAPI();
        this.candlestickData = new ArrayList<>();
        this.chartCanvas = new ChartCanvas();
        this.portfolios = new ArrayList<>();
        this.stockSymbols = new ArrayList<>();
        this.historicalData = new ArrayList<>();
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // Control panel
        JPanel controlPanel = new JPanel();
        controlPanel.add(new JLabel("Stock Price Chart: Buy Price, Current Price, Sell Price"));

        // Portfolio selection
        controlPanel.add(new JLabel("Portfolio:"));
        portfolioComboBox = new JComboBox<>();
        portfolioComboBox.setPreferredSize(new Dimension(250, 35));
        portfolioComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateStockComboBox();
            }
        });
        controlPanel.add(portfolioComboBox);

        // Stock selection
        controlPanel.add(new JLabel("Stock:"));
        stockComboBox = new JComboBox<>();
        stockComboBox.setPreferredSize(new Dimension(250, 35));
        stockComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshCharts();
            }
        });
        controlPanel.add(stockComboBox);

        // Color indicators
        JPanel colorPanel = new JPanel();
        colorPanel.add(createColorIndicator(Color.GREEN, "Buy"));
        colorPanel.add(createColorIndicator(Color.RED, "Current"));
        colorPanel.add(createColorIndicator(Color.BLUE, "Sell"));
        controlPanel.add(colorPanel);

        // Refresh button
        refreshBtn = new JButton("Refresh Charts");
        refreshBtn.addActionListener(e -> refreshCharts());
        controlPanel.add(refreshBtn);

        add(controlPanel, BorderLayout.NORTH);

        // Wrap chart canvas in JScrollPane
        JScrollPane scrollPane = new JScrollPane(chartCanvas);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setApp(App app) {
        this.app = app;
    }

    public void setDbManager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        populatePortfolioComboBox();
    }

    public void refreshCharts() {
        if (app != null && dbManager != null) {
            if (portfolios.isEmpty() && app.getCurrentUser() != null) {
                populatePortfolioComboBox();
            }
            loadChartData();
            chartCanvas.revalidate();
            chartCanvas.repaint();
        }
    }

    private void loadChartData() {
        candlestickData.clear();
        try {
            if (app.getCurrentUser() == null) {
                System.out.println("No current user logged in.");
                return;
            }
            if (portfolioComboBox.getSelectedIndex() >= 0 && portfolioComboBox.getSelectedIndex() < portfolios.size()) {
                Portfolio selectedPortfolio = portfolios.get(portfolioComboBox.getSelectedIndex());
                List<Position> positions = dbManager.getPositionsByPortfolioId(selectedPortfolio.getId());
                System.out.println("Found " + positions.size() + " positions in portfolio " + selectedPortfolio.getName());

                if (stockComboBox.getSelectedIndex() >= 0 && stockComboBox.getSelectedIndex() < stockSymbols.size()) {
                    String selectedStock = stockSymbols.get(stockComboBox.getSelectedIndex());
                    for (Position position : positions) {
                        if (position.getSymbol().equals(selectedStock)) {
                            double buyPrice = position.getAverageCost(); // Use getAverageCost instead of getAveragePrice

                            // Fetch real-time current price
                            double currentPrice = position.getCurrentPrice(); // Default to stored price
                            try {
                                com.stockportfolio.services.api.RealTimeStockAPI.StockQuote quote = new com.stockportfolio.services.api.RealTimeStockAPI().getStockQuote(selectedStock).get();
                                if (quote != null) {
                                    currentPrice = quote.getCurrentPrice();
                                }
                            } catch (Exception e) {
                                System.err.println("Error fetching real-time quote for " + selectedStock + ": " + e.getMessage());
                            }

                            double sellPrice = getSellPrice(position.getSymbol(), selectedPortfolio.getId()); // Use getSymbol instead of getStockSymbol

                            System.out.println("Position: " + position.getSymbol() + " - Buy: " + buyPrice + ", Current: " + currentPrice + ", Sell: " + sellPrice);

                            // Create candlestick data for buy, current, and sell prices
                            LocalDateTime now = LocalDateTime.now();
                            candlestickData.add(new CandlestickData(now.minusDays(2), buyPrice, buyPrice, buyPrice, buyPrice, selectedStock + " (Buy)"));
                            candlestickData.add(new CandlestickData(now.minusDays(1), currentPrice, currentPrice, currentPrice, currentPrice, selectedStock + " (Current)"));
                            if (sellPrice > 0) {
                                candlestickData.add(new CandlestickData(now, sellPrice, sellPrice, sellPrice, sellPrice, selectedStock + " (Sell)"));
                            }
                            break; // Only add the selected stock
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error loading chart data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private double getSellPrice(String symbol, int portfolioId) {
        try {
            List<Transaction> transactions = dbManager.getTransactionsByPortfolioId(portfolioId);
            double lastSellPrice = 0.0;
            for (Transaction t : transactions) {
                if (t.getSymbol().equals(symbol) && "SELL".equals(t.getType())) { // Use getSymbol instead of getStockSymbol
                    lastSellPrice = t.getPrice(); // Get the last sell price
                }
            }
            return lastSellPrice;
        } catch (Exception e) {
            System.err.println("Error getting sell price: " + e.getMessage());
        }
        return 0.0; // No sell price
    }

    private void populatePortfolioComboBox() {
        if (dbManager != null && app != null && app.getCurrentUser() != null) {
            try {
                portfolios = dbManager.getPortfoliosByUserId(app.getCurrentUser().getId());
                portfolioComboBox.removeAllItems();
                for (Portfolio portfolio : portfolios) {
                    portfolioComboBox.addItem(portfolio.getName());
                }
                if (!portfolios.isEmpty()) {
                    portfolioComboBox.setSelectedIndex(0);
                    updateStockComboBox();
                }
            } catch (Exception e) {
                System.err.println("Error populating portfolio combo box: " + e.getMessage());
            }
        }
    }

    private void updateStockComboBox() {
        if (portfolioComboBox.getSelectedIndex() >= 0 && portfolioComboBox.getSelectedIndex() < portfolios.size()) {
            Portfolio selectedPortfolio = portfolios.get(portfolioComboBox.getSelectedIndex());
            try {
                List<Position> positions = dbManager.getPositionsByPortfolioId(selectedPortfolio.getId());
                stockComboBox.removeAllItems();
                stockSymbols.clear();
                for (Position position : positions) {
                    stockComboBox.addItem(position.getSymbol());
                    stockSymbols.add(position.getSymbol());
                }
                if (!stockSymbols.isEmpty()) {
                    stockComboBox.setSelectedIndex(0);
                    refreshCharts();
                }
            } catch (Exception e) {
                System.err.println("Error updating stock combo box: " + e.getMessage());
            }
        }
    }

    private JPanel createColorIndicator(Color color, String label) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        // Color square
        JPanel colorSquare = new JPanel();
        colorSquare.setBackground(color);
        colorSquare.setPreferredSize(new Dimension(20, 20));
        panel.add(colorSquare, BorderLayout.WEST);

        // Label
        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(new Font("SF Pro Display", Font.PLAIN, 12));
        panel.add(labelComponent, BorderLayout.CENTER);

        return panel;
    }

    // Inner class for chart drawing
    private class ChartCanvas extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            if (candlestickData.isEmpty()) {
                g2d.setColor(Color.BLACK);
                g2d.drawString("No portfolio data available. Please refresh or check your positions.", 50, 100);
                return;
            }

            drawPriceChart(g2d, 50, 50, width - 100, height - 100);
        }

        private void drawPriceChart(Graphics2D g2d, int x, int y, int width, int height) {
            g2d.setColor(Color.BLACK);
            g2d.drawString("Bar Chart", x, y - 10);

            if (candlestickData.isEmpty()) return;

            int barWidth = 20; // Width of each bar
            int stockSpacing = 50; // Spacing between bars
            int stockWidth = 100; // Width per stock section

            double maxPrice = candlestickData.stream()
                .mapToDouble(c -> Math.max(c.high, c.close))
                .max().orElse(100);
            double minPrice = candlestickData.stream()
                .mapToDouble(c -> Math.min(c.low, c.open))
                .min().orElse(0);
            double priceRange = maxPrice - minPrice;
            if (priceRange == 0) {
                priceRange = Math.max(maxPrice * 0.01, 1.0); // Ensure minimum range for visibility
            }
            // Add 10% padding for better visual positioning
            double padding = priceRange * 0.1;
            minPrice = Math.max(0, minPrice - padding); // Don't go below 0
            maxPrice += padding;
            priceRange = maxPrice - minPrice;

            int chartHeight = height - 40;

            // Calculate total width: width per stock + legend space
            int totalWidth = x + (candlestickData.size() * stockWidth) + 200; // Extra for legend
            setPreferredSize(new Dimension(totalWidth, getHeight()));

            // Draw axes
            g2d.drawLine(x, y, x, y + chartHeight);
            g2d.drawLine(x, y + chartHeight, x + totalWidth - 200, y + chartHeight); // Extend x-axis to total width

            // Draw price labels
            for (int i = 0; i <= 5; i++) {
                double price = minPrice + (priceRange * i / 5);
                int yPos = y + chartHeight - (int)((price - minPrice) / priceRange * chartHeight);
                g2d.drawString(String.format("%.2f", price), x - 40, yPos + 5);
                g2d.drawLine(x - 5, yPos, x, yPos);
            }

            for (int i = 0; i < candlestickData.size(); i++) {
                CandlestickData data = candlestickData.get(i);
                int stockX = x + (i * stockWidth) + (stockWidth - barWidth) / 2; // Center the bar in the stock section

                // Determine bar color based on symbol
                Color barColor;
                if (data.symbol.contains("(Buy)")) {
                    barColor = Color.GREEN;
                } else if (data.symbol.contains("(Current)")) {
                    barColor = Color.RED;
                } else {
                    barColor = Color.BLUE; // Sell
                }

                // Calculate bar height
                int barHeight = (int)((data.close - minPrice) / priceRange * chartHeight);

                // Draw bar
                g2d.setColor(barColor);
                g2d.fillRect(stockX, y + chartHeight - barHeight, barWidth, barHeight);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(stockX, y + chartHeight - barHeight, barWidth, barHeight);

                // Draw stock symbol below the bar
                g2d.setColor(Color.BLACK);
                g2d.drawString(data.symbol, stockX - 10, y + chartHeight + 20);
            }

            // Legend
            g2d.setColor(Color.GREEN);
            g2d.fillRect(totalWidth - 350, y - 30, 15, 15);
            g2d.setColor(Color.BLACK);
            g2d.drawString("Buy", totalWidth - 330, y - 15);

            g2d.setColor(Color.RED);
            g2d.fillRect(totalWidth - 350, y - 10, 15, 15);
            g2d.setColor(Color.BLACK);
            g2d.drawString("Current", totalWidth - 330, y + 5);

            g2d.setColor(Color.BLUE);
            g2d.fillRect(totalWidth - 350, y + 10, 15, 15);
            g2d.setColor(Color.BLACK);
            g2d.drawString("Sell", totalWidth - 330, y + 25);
        }
    }
}
