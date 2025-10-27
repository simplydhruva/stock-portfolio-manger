package com.stockportfolio.model;

import java.time.LocalDateTime;

public class PortfolioLike {
    private int id;
    private int sharedPortfolioId;
    private int userId;
    private LocalDateTime likedAt;

    // Constructors
    public PortfolioLike() {}

    public PortfolioLike(int sharedPortfolioId, int userId) {
        this.sharedPortfolioId = sharedPortfolioId;
        this.userId = userId;
        this.likedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSharedPortfolioId() {
        return sharedPortfolioId;
    }

    public void setSharedPortfolioId(int sharedPortfolioId) {
        this.sharedPortfolioId = sharedPortfolioId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public LocalDateTime getLikedAt() {
        return likedAt;
    }

    public void setLikedAt(LocalDateTime likedAt) {
        this.likedAt = likedAt;
    }

    @Override
    public String toString() {
        return "PortfolioLike{" +
                "id=" + id +
                ", sharedPortfolioId=" + sharedPortfolioId +
                ", userId=" + userId +
                ", likedAt=" + likedAt +
                '}';
    }
}
