package com.stockportfolio.services;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.stockportfolio.model.Portfolio;
import com.stockportfolio.services.analytics.AIAnalytics;
import com.stockportfolio.utils.NotificationService;
import com.stockportfolio.utils.DatabaseManager;

public class RebalancingAlertService {
    private static RebalancingAlertService instance;
    private ScheduledExecutorService scheduler;
    private AIAnalytics aiAnalytics;
    private NotificationService notificationService;
    private DatabaseManager dbManager;

    private RebalancingAlertService() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.aiAnalytics = new AIAnalytics();
        this.notificationService = NotificationService.getInstance();
        try {
            this.dbManager = DatabaseManager.getInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database manager", e);
        }
    }

    public static synchronized RebalancingAlertService getInstance() {
        if (instance == null) {
            instance = new RebalancingAlertService();
        }
        return instance;
    }

    public void startMonitoring(int userId) {
        // Schedule periodic checks every hour
        scheduler.scheduleAtFixedRate(() -> checkPortfolioRebalancing(userId), 0, 1, TimeUnit.HOURS);
    }

    public void stopMonitoring() {
        scheduler.shutdownNow();
    }

    private void checkPortfolioRebalancing(int userId) {
        try {
            List<Portfolio> portfolios = dbManager.getPortfoliosByUserId(userId);
            for (Portfolio portfolio : portfolios) {
                AIAnalytics.PortfolioOptimization optimization = aiAnalytics.optimizePortfolio(String.valueOf(portfolio.getId())).join();
                if (optimization != null) {
                    boolean needsRebalancing = optimization.getRecommendations() != null && !optimization.getRecommendations().isEmpty();
                    if (needsRebalancing) {
                        String message = "Your portfolio '" + portfolio.getName() + "' requires rebalancing. Please review the recommendations.";
                        notificationService.sendNotification(String.valueOf(userId), com.stockportfolio.utils.NotificationService.NotificationType.WARNING, "Portfolio Rebalancing Alert", message);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking portfolio rebalancing: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
