package com.stockportfolio.services.analytics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.stockportfolio.model.Portfolio;
import com.stockportfolio.model.Position;

public class AIAnalytics {

    // Make generateRebalanceRecommendations public for testing
    public List<RebalanceRecommendation> generateRebalanceRecommendations(Map<String, Double> currentAllocation, Map<String, Double> targetAllocation) {
        List<RebalanceRecommendation> recommendations = new java.util.ArrayList<>();

        for (String symbol : currentAllocation.keySet()) {
            double current = currentAllocation.get(symbol);
            double target = targetAllocation.getOrDefault(symbol, 0.0);
            double difference = target - current;

            String action;
            String description;

            if (Math.abs(difference) < 0.01) { // Within 1%
                action = "HOLD";
                description = "Current allocation is optimal";
            } else if (difference > 0) {
                action = "BUY";
                description = String.format("Increase allocation by %.1f%%", difference * 100);
            } else {
                action = "SELL";
                description = String.format("Reduce allocation by %.1f%%", Math.abs(difference) * 100);
            }

            recommendations.add(new RebalanceRecommendation(symbol, action, description, difference));
        }

        return recommendations;
    }
    public static class PortfolioOptimization {
        private String reasoning;
        private Map<String, Double> optimizedAllocation;
        private double expectedReturn;
        private double expectedRisk;
        private double sharpeRatio;
        private List<RebalanceRecommendation> recommendations;

        public PortfolioOptimization(String reasoning, Map<String, Double> optimizedAllocation,
                                   double expectedReturn, double expectedRisk, double sharpeRatio,
                                   List<RebalanceRecommendation> recommendations) {
            this.reasoning = reasoning;
            this.optimizedAllocation = optimizedAllocation;
            this.expectedReturn = expectedReturn;
            this.expectedRisk = expectedRisk;
            this.sharpeRatio = sharpeRatio;
            this.recommendations = recommendations;
        }

        public String getReasoning() { return reasoning; }
        public Map<String, Double> getOptimizedAllocation() { return optimizedAllocation; }
        public double getExpectedReturn() { return expectedReturn; }
        public double getExpectedRisk() { return expectedRisk; }
        public double getSharpeRatio() { return sharpeRatio; }
        public List<RebalanceRecommendation> getRecommendations() { return recommendations; }
    }

    public static class RebalanceRecommendation {
        private String symbol;
        private String action;
        private String description;
        private double difference;

        public RebalanceRecommendation(String symbol, String action, String description, double difference) {
            this.symbol = symbol;
            this.action = action;
            this.description = description;
            this.difference = difference;
        }

        public String getSymbol() { return symbol; }
        public String getAction() { return action; }
        public String getDescription() { return description; }
        public double getDifference() { return difference; }
    }

    public static class RiskAssessment {
        private double volatility;
        private double sharpeRatio;
        private String riskLevel;

        public RiskAssessment(double volatility, double sharpeRatio, String riskLevel) {
            this.volatility = volatility;
            this.sharpeRatio = sharpeRatio;
            this.riskLevel = riskLevel;
        }

        public double getVolatility() { return volatility; }
        public double getSharpeRatio() { return sharpeRatio; }
        public String getRiskLevel() { return riskLevel; }
    }

    public static class PerformanceMetrics {
        private double totalReturn;
        private double annualizedReturn;
        private double volatility;
        private double maxDrawdown;

        public PerformanceMetrics(double totalReturn, double annualizedReturn, double volatility, double maxDrawdown) {
            this.totalReturn = totalReturn;
            this.annualizedReturn = annualizedReturn;
            this.volatility = volatility;
            this.maxDrawdown = maxDrawdown;
        }

        public double getTotalReturn() { return totalReturn; }
        public double getAnnualizedReturn() { return annualizedReturn; }
        public double getVolatility() { return volatility; }
        public double getMaxDrawdown() { return maxDrawdown; }
    }

    public Map<String, Object> analyzePortfolio(Portfolio portfolio, List<Position> positions) {
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("totalValue", calculateTotalValue(positions));
        analysis.put("riskLevel", calculateRiskLevel(positions));
        analysis.put("recommendations", generateRecommendations(positions));
        return analysis;
    }

    public CompletableFuture<PortfolioOptimization> optimizePortfolio(String portfolioId) {
        // Dummy implementation - simulate async processing
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000); // Simulate processing time
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Create dummy optimized allocation
            Map<String, Double> allocation = new HashMap<>();
            allocation.put("AAPL", 0.25);
            allocation.put("GOOGL", 0.20);
            allocation.put("MSFT", 0.20);
            allocation.put("AMZN", 0.15);
            allocation.put("TSLA", 0.20);

            // Create dummy recommendations
            List<RebalanceRecommendation> recommendations = new java.util.ArrayList<>();
            recommendations.add(new RebalanceRecommendation("AAPL", "BUY", "Increase allocation to Apple", 0.05));
            recommendations.add(new RebalanceRecommendation("GOOGL", "HOLD", "Maintain current allocation", 0.0));
            recommendations.add(new RebalanceRecommendation("TSLA", "SELL", "Reduce allocation to Tesla", -0.05));

            return new PortfolioOptimization(
                "Optimized based on modern portfolio theory with risk-adjusted returns",
                allocation,
                0.12, // expected return
                0.18, // expected risk
                1.45, // sharpe ratio
                recommendations
            );
        });
    }

    public RiskAssessment assessRisk(Map<String, Double> portfolioWeights) {
        // Simple risk assessment based on portfolio weights
        double volatility = 0.15; // Default volatility
        double sharpeRatio = 1.2; // Default Sharpe ratio
        String riskLevel = "Medium";

        // Calculate basic risk metrics
        double concentration = portfolioWeights.values().stream().mapToDouble(w -> w * w).sum();
        if (concentration > 0.5) {
            riskLevel = "High";
            volatility = 0.25;
            sharpeRatio = 0.8;
        } else if (concentration < 0.1) {
            riskLevel = "Low";
            volatility = 0.10;
            sharpeRatio = 1.8;
        }

        return new RiskAssessment(volatility, sharpeRatio, riskLevel);
    }

    public PerformanceMetrics analyzePerformance(Map<String, Double> returns) {
        // Simple performance analysis
        double totalReturn = returns.values().stream().mapToDouble(r -> r).average().orElse(0.0);
        double annualizedReturn = totalReturn; // Simplified
        double volatility = 0.15; // Default
        double maxDrawdown = -0.10; // Default

        return new PerformanceMetrics(totalReturn, annualizedReturn, volatility, maxDrawdown);
    }

    public RebalanceRecommendation getRebalanceRecommendation(List<Position> positions) {
        // Dummy implementation
        return new RebalanceRecommendation("AAPL", "BUY", "Rebalance needed", 0.1);
    }

    private double calculateTotalValue(List<Position> positions) {
        return positions.stream().mapToDouble(p -> p.getQuantity() * p.getCurrentPrice()).sum();
    }

    private String calculateRiskLevel(List<Position> positions) {
        // Simple risk calculation based on diversification
        long uniqueStocks = positions.stream().map(Position::getStockSymbol).distinct().count();
        if (uniqueStocks < 5) return "High";
        if (uniqueStocks < 10) return "Medium";
        return "Low";
    }

    private List<String> generateRecommendations(List<Position> positions) {
        List<String> recommendations = new java.util.ArrayList<>();
        recommendations.add("Diversify your portfolio");
        recommendations.add("Consider long-term investments");
        return recommendations;
    }
}
