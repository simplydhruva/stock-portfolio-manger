package com.stockportfolio.model;

import java.time.LocalDateTime;

public class Transaction {
    private int id;
    private int userId;
    private int portfolioId;
    private String stockSymbol;
    private String type;
    private String orderType;
    private double quantity;
    private double price;
    private double totalAmount;
    private LocalDateTime timestamp;
    private String status;
    private String notes;

    public Transaction() {}

    public Transaction(int id, int userId, int portfolioId, String stockSymbol,
                      String type, String orderType, double quantity, double price) {
        this.id = id;
        this.userId = userId;
        this.portfolioId = portfolioId;
        this.stockSymbol = stockSymbol;
        this.type = type;
        this.orderType = orderType;
        this.quantity = quantity;
        this.price = price;
        this.totalAmount = quantity * price;
        this.timestamp = LocalDateTime.now();
        this.status = "PENDING";
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getPortfolioId() { return portfolioId; }
    public void setPortfolioId(int portfolioId) { this.portfolioId = portfolioId; }

    public String getStockSymbol() { return stockSymbol; }
    public void setStockSymbol(String stockSymbol) { this.stockSymbol = stockSymbol; }

    public String getSymbol() { return stockSymbol; }
    public void setSymbol(String symbol) { this.stockSymbol = symbol; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
