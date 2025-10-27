import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.stockportfolio.model.User;
import com.stockportfolio.services.api.RealTimeStockAPI;
import com.stockportfolio.utils.NotificationService;
import com.stockportfolio.utils.DatabaseManager;

public class DashboardScreen extends JPanel implements ThemeChangeListener {
    private App app;
    private DatabaseManager dbManager;
    private RealTimeStockAPI realTimeStockAPI;

    private JButton refreshButton;
    private NotificationService notificationService;
    private JLabel welcomeLabel;

    // Info card references for refreshing
    private InfoCard portfolioValueCard;
    private InfoCard todaysPnLCard;
    private InfoCard totalTradesCard;

    public DashboardScreen(App app, DatabaseManager dbManager) {
        this.app = app;
        this.dbManager = dbManager;
        this.realTimeStockAPI = new RealTimeStockAPI();
        this.notificationService = NotificationService.getInstance();
        initializeUI();
        checkAchievements();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // North panel with title and info cards
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));

        // Title panel with logo and centered welcome
        JPanel titlePanel = new JPanel(new BorderLayout());
        ImageIcon logoIcon = new ImageIcon("wealthwise image.jpg");
        // Scale the image to be bigger
        java.awt.Image scaledImage = logoIcon.getImage().getScaledInstance(100, 100, java.awt.Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);
        JLabel logoLabel = new JLabel(scaledIcon);
        titlePanel.add(logoLabel, BorderLayout.WEST);

        User currentUser = app.getCurrentUser();
        String welcomeText = "Welcome to WealthWise";
        if (currentUser != null && currentUser.getUsername() != null && !currentUser.getUsername().isEmpty()) {
            welcomeText += " - " + currentUser.getUsername();
        } else {
            welcomeText += " - Guest";
            System.out.println("Debug: currentUser is null or username is null/empty. currentUser: " + currentUser);
        }
        welcomeLabel = new JLabel(welcomeText);
        welcomeLabel.setHorizontalAlignment(JLabel.CENTER);
        welcomeLabel.setFont(new Font("SF Pro Display", Font.BOLD, 32));
        titlePanel.add(welcomeLabel, BorderLayout.CENTER);

        northPanel.add(titlePanel);

        // Info cards panel
        JPanel infoPanel = new JPanel(new FlowLayout());
        this.portfolioValueCard = new InfoCard("Portfolio Value", "$0.00");
        this.todaysPnLCard = new InfoCard("Today's P&L", "$0.00");
        this.totalTradesCard = new InfoCard("Total Trades", "0");
        infoPanel.add(this.portfolioValueCard);
        infoPanel.add(this.todaysPnLCard);
        infoPanel.add(this.totalTradesCard);
        northPanel.add(infoPanel);

        // Update info cards with real data
        updatePortfolioStats(this.portfolioValueCard, this.todaysPnLCard, this.totalTradesCard);

        add(northPanel, BorderLayout.NORTH);



        // Register for theme changes
        ThemeManager.getInstance().addPropertyChangeListener(this);

        // Control panel
        JPanel controlPanel = new JPanel(new FlowLayout());
        refreshButton = new JButton("Refresh Data");
        refreshButton.addActionListener(e -> {
            // Also refresh portfolio stats
            updatePortfolioStats(this.portfolioValueCard, this.todaysPnLCard, this.totalTradesCard);
        });
        controlPanel.add(refreshButton);

        JButton managePortfoliosButton = new JButton("Manage Portfolios");
        managePortfoliosButton.addActionListener(e -> app.showScreen("PORTFOLIO"));
        controlPanel.add(managePortfoliosButton);

        JButton analyticsButton = new JButton("Analytics & Reports");
        analyticsButton.addActionListener(e -> app.showScreen("ANALYTICS"));
        controlPanel.add(analyticsButton);

        add(controlPanel, BorderLayout.SOUTH);


    }





    private void updatePortfolioStats(InfoCard portfolioValueCard, InfoCard todaysPnLCard, InfoCard totalTradesCard) {
        if (app.getCurrentUser() == null) {
            return;
        }

        try {
            // Calculate portfolio value and P&L
            double totalValue = 0.0;
            double todaysPnL = 0.0;
            int totalTrades = 0;

            List<com.stockportfolio.model.Portfolio> portfolios = dbManager.getPortfoliosByUserId(app.getCurrentUser().getId());

            // Collect all unique stock symbols from user's positions
            java.util.Set<String> symbols = new java.util.HashSet<>();
            for (com.stockportfolio.model.Portfolio portfolio : portfolios) {
                List<com.stockportfolio.model.Position> positions = dbManager.getPositionsByPortfolioId(portfolio.getId());
                for (com.stockportfolio.model.Position position : positions) {
                    symbols.add(position.getSymbol());
                }
            }

            // Fetch real-time quotes for all symbols
            java.util.Map<String, RealTimeStockAPI.StockQuote> quotes = new java.util.HashMap<>();
            for (String symbol : symbols) {
                try {
                    RealTimeStockAPI.StockQuote quote = realTimeStockAPI.getStockQuote(symbol).get();
                    if (quote != null) {
                        quotes.put(symbol, quote);
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching quote for " + symbol + ": " + e.getMessage());
                }
            }

            for (com.stockportfolio.model.Portfolio portfolio : portfolios) {
                List<com.stockportfolio.model.Position> positions = dbManager.getPositionsByPortfolioId(portfolio.getId());

                for (com.stockportfolio.model.Position position : positions) {
                    // Get real-time price or fall back to stored price
                    double currentPrice = position.getCurrentPrice();
                    RealTimeStockAPI.StockQuote quote = quotes.get(position.getSymbol());
                    if (quote != null) {
                        currentPrice = quote.getCurrentPrice();
                        // Update position with real-time price
                        position.setCurrentPrice(currentPrice);
                        position.setTotalValue(position.getQuantity() * currentPrice);
                        position.setLastUpdated(java.time.LocalDateTime.now());
                        // Save updated position to database
                        dbManager.updatePosition(position);
                    }

                    double currentValue = position.getQuantity() * currentPrice;
                    totalValue += currentValue;

                    // Calculate today's P&L as change from previous close
                    if (quote != null) {
                        double previousClose = quote.getPreviousClose();
                        todaysPnL += (currentPrice - previousClose) * position.getQuantity();
                    } else {
                        // Fallback: if no quote, use unrealized P&L as approximation
                        double costBasis = position.getQuantity() * position.getAverageCost();
                        todaysPnL += (currentValue - costBasis);
                    }
                }

                // Count total trades
                List<com.stockportfolio.model.Transaction> transactions = dbManager.getTransactionsByPortfolioId(portfolio.getId());
                totalTrades += transactions.size();
            }

            // Update the info cards
            portfolioValueCard.updateValue(String.format("$%.2f", totalValue));
            todaysPnLCard.updateValue(String.format("$%.2f", todaysPnL));
            totalTradesCard.updateValue(String.valueOf(totalTrades));

        } catch (Exception e) {
            System.err.println("Error updating portfolio stats: " + e.getMessage());
        }
    }

    private void checkAchievements() {
        if (app.getCurrentUser() != null) {
            // Convert App.User to com.stockportfolio.model.User
            com.stockportfolio.model.User user = convertToModelUser(app.getCurrentUser());
            notificationService.checkAndAwardAchievements(user);
        }
    }

    private com.stockportfolio.model.User convertToModelUser(User appUser) {
        com.stockportfolio.model.User modelUser = new com.stockportfolio.model.User();
        modelUser.setId(appUser.getId());
        modelUser.setUsername(appUser.getUsername());
        modelUser.setEmail(appUser.getEmail());
        modelUser.setLevel(appUser.getLevel());
        modelUser.setTradingStreak(appUser.getTradingStreak());
        modelUser.setTotalPnL(appUser.getTotalPnL());
        modelUser.setTradesCount(appUser.getTradesCount());
        // Add other fields as needed
        return modelUser;
    }

    public void refreshWelcomeMessage() {
        User currentUser = app.getCurrentUser();
        String welcomeText = "Welcome to WealthWise";
        if (currentUser != null && currentUser.getUsername() != null && !currentUser.getUsername().isEmpty()) {
            welcomeText += " - " + currentUser.getUsername();
        } else {
            welcomeText += " - Guest";
        }
        if (welcomeLabel != null) {
            welcomeLabel.setText(welcomeText);
        }
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme newTheme) {
        // Apply theme to components if needed
        // For now, InfoCards handle their own themes
    }
}
