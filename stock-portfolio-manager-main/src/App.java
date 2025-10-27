import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.stockportfolio.model.User;
import com.stockportfolio.services.api.RealTimeStockAPI;
import com.stockportfolio.utils.DatabaseManager;

public class App extends JFrame {
    private DatabaseManager dbManager;
    private User currentUser;
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private DashboardScreen dashboardScreen;

    // New service instances
    private RealTimeStockAPI realTimeStockAPI;

    public App() {
        try {
            dbManager = DatabaseManager.getInstance();

            // Initialize new services
            realTimeStockAPI = new RealTimeStockAPI();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Database connection failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        initializeUI();
    }

    private void initializeUI() {
        setTitle("Stock Portfolio Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create main panel with card layout for different screens
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Create sidebar panel and add to frame
        SidebarPanel sidebarPanel = new SidebarPanel(this);
        add(sidebarPanel, BorderLayout.WEST);

        add(mainPanel, BorderLayout.CENTER);

        // Add screens with new services injected
        LoginScreen loginScreen = new LoginScreen(this, dbManager);
        RegistrationScreen registrationScreen = new RegistrationScreen(this, dbManager);
        this.dashboardScreen = new DashboardScreen(this, dbManager);
        TradingScreen tradingScreen = new TradingScreen(this, dbManager, realTimeStockAPI);
        PortfolioScreen portfolioScreen = new PortfolioScreen(this, dbManager);
        AnalyticsReportsScreen analyticsScreen = new AnalyticsReportsScreen(this, dbManager);
        TradeHistoryScreen tradeHistoryScreen = new TradeHistoryScreen(this, dbManager);
        WatchlistScreen watchlistScreen = new WatchlistScreen(this, dbManager);
        SettingsScreen settingsScreen = new SettingsScreen(this, dbManager);

        mainPanel.add(loginScreen, "LOGIN");
        mainPanel.add(registrationScreen, "REGISTRATION");
        mainPanel.add(this.dashboardScreen, "DASHBOARD");
        mainPanel.add(tradingScreen, "TRADING");
        mainPanel.add(portfolioScreen, "PORTFOLIO");
        mainPanel.add(analyticsScreen, "ANALYTICS");
        mainPanel.add(tradeHistoryScreen, "TRADE_HISTORY");
        mainPanel.add(watchlistScreen, "WATCHLIST");
        mainPanel.add(settingsScreen, "SETTINGS");


        // Show login screen first
        cardLayout.show(mainPanel, "LOGIN");

        // Add window listener to properly close database connection on exit
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if (dbManager != null) {
                        dbManager.close();
                    }
                } catch (Exception ex) {
                    System.err.println("Error closing database connection: " + ex.getMessage());
                }
            }
        });

        setVisible(true);
    }

    public void showScreen(String screenName) {
        cardLayout.show(mainPanel, screenName);
    }

    public void showDashboard() {
        cardLayout.show(mainPanel, "DASHBOARD");
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        ((TradingScreen) mainPanel.getComponent(3)).refreshPortfolios();
        // Refresh dashboard welcome message with username
        if (dashboardScreen != null) {
            dashboardScreen.refreshWelcomeMessage();
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void logout() {
        currentUser = null;
        cardLayout.show(mainPanel, "LOGIN");
        ((LoginScreen) mainPanel.getComponent(0)).clearFields();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new App());
    }
}
