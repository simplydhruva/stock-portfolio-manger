import java.time.LocalDateTime;

public class Position {
    private int id;
    private int portfolioId;
    private String stockSymbol;
    private String symbol;
    private String assetType;
    private double quantity;
    private double averagePrice;
    private double averageCost;
    private double currentPrice;
    private double totalValue;
    private double totalCost;
    private double unrealizedPnL;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastUpdated;

    public Position() {}

    public Position(int id, int portfolioId, String stockSymbol, double quantity, double averagePrice) {
        this.id = id;
        this.portfolioId = portfolioId;
        this.stockSymbol = stockSymbol;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
        this.currentPrice = averagePrice;
        this.totalValue = quantity * averagePrice;
        this.totalCost = quantity * averagePrice;
        this.unrealizedPnL = 0.0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPortfolioId() { return portfolioId; }
    public void setPortfolioId(int portfolioId) { this.portfolioId = portfolioId; }

    public String getStockSymbol() { return stockSymbol; }
    public void setStockSymbol(String stockSymbol) { this.stockSymbol = stockSymbol; }

    public String getSymbol() { return symbol != null ? symbol : stockSymbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public double getAveragePrice() { return averagePrice; }
    public void setAveragePrice(double averagePrice) { this.averagePrice = averagePrice; }

    public double getAverageCost() { return averageCost != 0.0 ? averageCost : averagePrice; }
    public void setAverageCost(double averageCost) { this.averageCost = averageCost; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public double getTotalValue() { return totalValue; }
    public void setTotalValue(double totalValue) { this.totalValue = totalValue; }

    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }

    public double getUnrealizedPnL() { return unrealizedPnL; }
    public void setUnrealizedPnL(double unrealizedPnL) { this.unrealizedPnL = unrealizedPnL; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getLastUpdated() { return lastUpdated != null ? lastUpdated : updatedAt; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}
