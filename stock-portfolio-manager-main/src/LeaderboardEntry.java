import java.time.LocalDateTime;

public class LeaderboardEntry {
    private int id;
    private int userId;
    private String username;
    private double totalReturn;
    private double totalValue;
    private int rank;
    private String period; // "DAILY", "WEEKLY", "MONTHLY", "ALL_TIME"
    private LocalDateTime calculatedAt;

    // Constructors
    public LeaderboardEntry() {}

    public LeaderboardEntry(int userId, String username, double totalReturn, double totalValue, int rank, String period) {
        this.userId = userId;
        this.username = username;
        this.totalReturn = totalReturn;
        this.totalValue = totalValue;
        this.rank = rank;
        this.period = period;
        this.calculatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public double getTotalReturn() { return totalReturn; }
    public void setTotalReturn(double totalReturn) { this.totalReturn = totalReturn; }

    public double getTotalValue() { return totalValue; }
    public void setTotalValue(double totalValue) { this.totalValue = totalValue; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }
}
