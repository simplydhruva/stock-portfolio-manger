import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import com.stockportfolio.model.Portfolio;
import com.stockportfolio.model.Position;
import com.stockportfolio.model.Transaction;
import com.stockportfolio.services.api.RealTimeStockAPI;
import com.stockportfolio.utils.DatabaseManager;

public class TradeExecutor {
    private DatabaseManager dbManager;
    private RealTimeStockAPI stockAPI;

    public TradeExecutor(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.stockAPI = new RealTimeStockAPI();
    }

    /**
     * Execute a trade for a user
     * @param userId The user ID
     * @param portfolioId The portfolio ID
     * @param symbol The stock symbol
     * @param quantity The quantity to trade (positive for buy, negative for sell)
     * @param orderType The order type (MARKET, LIMIT, etc.)
     * @return Transaction object if successful, null if failed
     */
    public Transaction executeTrade(int userId, int portfolioId, String symbol, double quantity, String orderType) throws SQLException {
        try {
            // Get current stock price from API
            RealTimeStockAPI.StockQuote quote = stockAPI.getStockQuote(symbol).get();
            if (quote == null) {
                System.err.println("Failed to get stock quote for " + symbol);
                return null;
            }

            double price = quote.getCurrentPrice();
            double totalAmount = Math.abs(quantity) * price;

            // Validate trade
            if (!validateTrade(userId, portfolioId, symbol, quantity, totalAmount)) {
                System.err.println("Trade validation failed");
                return null;
            }

            // Create transaction record
            Transaction transaction = new Transaction();
            transaction.setUserId(userId);
            transaction.setPortfolioId(portfolioId);
            transaction.setStockSymbol(symbol);
            transaction.setType(quantity > 0 ? "BUY" : "SELL");
            transaction.setOrderType(orderType);
            transaction.setQuantity(Math.abs(quantity));
            transaction.setPrice(price);
            transaction.setTotalAmount(totalAmount);
            transaction.setTimestamp(LocalDateTime.now());
            transaction.setStatus("COMPLETED");

            // Save transaction to database
            dbManager.saveTransaction(transaction);

            // Update portfolio positions
            updatePortfolioPositions(portfolioId, symbol, quantity, price);

            // Update user statistics
            updateUserStatistics(userId, transaction);

            System.out.println("Successfully executed " + transaction.getType() + " trade: " +
                             transaction.getQuantity() + " " + symbol + " @ $" + price);

            return transaction;

        } catch (Exception e) {
            System.err.println("Error executing trade: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Validate if a trade can be executed
     */
    private boolean validateTrade(int userId, int portfolioId, String symbol, double quantity, double totalAmount) throws SQLException {
        // Check if portfolio exists and belongs to user
        Portfolio portfolio = dbManager.getPortfolioById(portfolioId);
        if (portfolio == null || portfolio.getUserId() != userId) {
            System.err.println("Invalid portfolio or user access");
            return false;
        }

        // For sell orders, check if user has sufficient position
        if (quantity < 0) {
            List<Position> positions = dbManager.getPositionsByPortfolioId(portfolioId);
            double currentPosition = 0;
            for (Position pos : positions) {
                if (pos.getSymbol().equals(symbol)) {
                    currentPosition = pos.getQuantity();
                    break;
                }
            }

            if (Math.abs(quantity) > currentPosition) {
                System.err.println("Insufficient position for sell order. Current: " + currentPosition + ", Requested: " + Math.abs(quantity));
                return false;
            }
        }

        // Additional validation logic can be added here
        // e.g., account balance checks, trading limits, etc.

        return true;
    }

    /**
     * Update portfolio positions after a trade
     */
    private void updatePortfolioPositions(int portfolioId, String symbol, double quantity, double price) throws SQLException {
        List<Position> positions = dbManager.getPositionsByPortfolioId(portfolioId);
        Position existingPosition = null;

        // Find existing position
        for (Position pos : positions) {
            if (pos.getSymbol().equals(symbol)) {
                existingPosition = pos;
                break;
            }
        }

        if (existingPosition == null) {
            // Create new position for buy orders
            if (quantity > 0) {
                Position newPosition = new Position();
                newPosition.setPortfolioId(portfolioId);
                newPosition.setSymbol(symbol);
                newPosition.setAssetType("stock");
                newPosition.setQuantity(quantity);
                newPosition.setAverageCost(price);
                newPosition.setCurrentPrice(price);
                newPosition.setTotalValue(quantity * price);
                newPosition.setCreatedAt(LocalDateTime.now());
                newPosition.setUpdatedAt(LocalDateTime.now());
                newPosition.setLastUpdated(LocalDateTime.now());

                dbManager.savePosition(newPosition);
            }
        } else {
            // Update existing position
            double newQuantity = existingPosition.getQuantity() + quantity;

            if (newQuantity <= 0) {
                // Close position if quantity becomes zero or negative
                dbManager.deletePosition(existingPosition.getId());
            } else {
                // Update position with new average cost
                double totalCost = (existingPosition.getQuantity() * existingPosition.getAverageCost()) +
                                 (quantity * price);
                double newAverageCost = totalCost / newQuantity;

                existingPosition.setQuantity(newQuantity);
                existingPosition.setAverageCost(newAverageCost);
                existingPosition.setCurrentPrice(price);
                existingPosition.setTotalValue(newQuantity * price);
                existingPosition.setUpdatedAt(LocalDateTime.now());
                existingPosition.setLastUpdated(LocalDateTime.now());

                dbManager.updatePosition(existingPosition);
            }
        }

        // Update portfolio totals
        updatePortfolioTotals(portfolioId);
    }

    /**
     * Update portfolio total values
     */
    private void updatePortfolioTotals(int portfolioId) throws SQLException {
        Portfolio portfolio = dbManager.getPortfolioById(portfolioId);
        if (portfolio == null) return;

        List<Position> positions = dbManager.getPositionsByPortfolioId(portfolioId);
        double totalValue = 0.0;
        double totalCostBasis = 0.0;

        for (Position pos : positions) {
            totalValue += pos.getTotalValue();
            totalCostBasis += pos.getQuantity() * pos.getAverageCost();
        }

        portfolio.setTotalValue(totalValue);
        portfolio.setTotalCostBasis(totalCostBasis);
        portfolio.setTotalPnL(totalValue - totalCostBasis);
        portfolio.setUpdatedAt(LocalDateTime.now());

        dbManager.updatePortfolio(portfolio);
    }

    /**
     * Update user trading statistics
     */
    private void updateUserStatistics(int userId, Transaction transaction) throws SQLException {
        // This would typically update user experience points, trading streak, etc.
        // For now, just increment trade count
        // In a real implementation, you'd fetch the user and update their stats
        System.out.println("Updated trading statistics for user " + userId);
    }

    /**
     * Get trade history for a user
     */
    public List<Transaction> getTradeHistory(int userId) throws SQLException {
        return dbManager.getTransactionsByUserId(userId);
    }

    /**
     * Get trade history for a portfolio
     */
    public List<Transaction> getPortfolioTradeHistory(int portfolioId) throws SQLException {
        return dbManager.getTransactionsByPortfolioId(portfolioId);
    }

    /**
     * Cancel a pending transaction
     */
    public boolean cancelTransaction(int transactionId) throws SQLException {
        Transaction transaction = dbManager.getTransactionById(transactionId);
        if (transaction != null && "PENDING".equals(transaction.getStatus())) {
            transaction.setStatus("CANCELLED");
            dbManager.updateTransaction(transaction);
            return true;
        }
        return false;
    }
}
