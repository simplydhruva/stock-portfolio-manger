package com.stockportfolio.model;

import java.time.LocalDateTime;

public class User {
    private int id;
    private String username;
    private String email;
    private String hashedPassword;
    private String salt;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private boolean isActive;
    private String role;
    private int experiencePoints;
    private int level;
    private int tradingStreak;
    private double totalPnL;
    private int tradesCount;
    private int failedLoginAttempts;
    private LocalDateTime accountLockedUntil;
    private String avatarPath;
    private String bio;
    private String location;
    private String website;

    // Default constructor
    public User() {}

    // Constructor with parameters
    public User(String username, String email, String hashedPassword, String salt) {
        this.username = username;
        this.email = email;
        this.hashedPassword = hashedPassword;
        this.salt = salt;
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
        this.role = "basic";
        this.experiencePoints = 0;
        this.level = 1;
        this.tradingStreak = 0;
        this.totalPnL = 0.0;
        this.tradesCount = 0;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getExperiencePoints() {
        return experiencePoints;
    }

    public void setExperiencePoints(int experiencePoints) {
        this.experiencePoints = experiencePoints;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getTradingStreak() {
        return tradingStreak;
    }

    public void setTradingStreak(int tradingStreak) {
        this.tradingStreak = tradingStreak;
    }

    public double getTotalPnL() {
        return totalPnL;
    }

    public void setTotalPnL(double totalPnL) {
        this.totalPnL = totalPnL;
    }

    public int getTradesCount() {
        return tradesCount;
    }

    public void setTradesCount(int tradesCount) {
        this.tradesCount = tradesCount;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public LocalDateTime getAccountLockedUntil() {
        return accountLockedUntil;
    }

    public void setAccountLockedUntil(LocalDateTime accountLockedUntil) {
        this.accountLockedUntil = accountLockedUntil;
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", createdAt=" + createdAt +
                ", lastLogin=" + lastLogin +
                ", isActive=" + isActive +
                ", role='" + role + '\'' +
                ", experiencePoints=" + experiencePoints +
                ", level=" + level +
                ", tradingStreak=" + tradingStreak +
                ", totalPnL=" + totalPnL +
                ", tradesCount=" + tradesCount +
                '}';
    }
}
