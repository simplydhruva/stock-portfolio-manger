package com.stockportfolio.model;

import java.time.LocalDateTime;

public class SharedPortfolio {
    private int id;
    private int portfolioId;
    private int userId;
    private boolean isPublic;
    private LocalDateTime sharedAt;
    private int views;
    private int likes;

    // Constructors
    public SharedPortfolio() {}

    public SharedPortfolio(int portfolioId, int userId, boolean isPublic) {
        this.portfolioId = portfolioId;
        this.userId = userId;
        this.isPublic = isPublic;
        this.sharedAt = LocalDateTime.now();
        this.views = 0;
        this.likes = 0;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(int portfolioId) {
        this.portfolioId = portfolioId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public LocalDateTime getSharedAt() {
        return sharedAt;
    }

    public void setSharedAt(LocalDateTime sharedAt) {
        this.sharedAt = sharedAt;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    @Override
    public String toString() {
        return "SharedPortfolio{" +
                "id=" + id +
                ", portfolioId=" + portfolioId +
                ", userId=" + userId +
                ", isPublic=" + isPublic +
                ", sharedAt=" + sharedAt +
                ", views=" + views +
                ", likes=" + likes +
                '}';
    }
}
