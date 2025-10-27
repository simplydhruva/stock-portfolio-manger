package com.stockportfolio.model;

import java.time.LocalDateTime;

public class WatchlistItem {
    private int id;
    private int userId;
    private String symbol;
    private LocalDateTime addedAt;
    private double targetPrice;
    private String notes;

    public WatchlistItem() {}

    public WatchlistItem(int userId, String symbol) {
        this.userId = userId;
        this.symbol = symbol;
        this.addedAt = LocalDateTime.now();
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }

    public double getTargetPrice() { return targetPrice; }
    public void setTargetPrice(double targetPrice) { this.targetPrice = targetPrice; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
