import java.time.LocalDateTime;
import java.util.List;

public class Portfolio {
    private int id;
    private int userId;
    private String name;
    private String description;
    private double totalValue;
    private double totalCost;
    private double totalCostBasis;
    private double totalPnL;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Position> positions;

    public Portfolio() {}

    public Portfolio(int id, int userId, String name) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.totalValue = 0.0;
        this.totalCost = 0.0;
        this.totalPnL = 0.0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getTotalValue() { return totalValue; }
    public void setTotalValue(double totalValue) { this.totalValue = totalValue; }

    public double getTotalCostBasis() { return totalCostBasis; }
    public void setTotalCostBasis(double totalCostBasis) { this.totalCostBasis = totalCostBasis; }

    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }

    public double getTotalPnL() { return totalPnL; }
    public void setTotalPnL(double totalPnL) { this.totalPnL = totalPnL; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<Position> getPositions() { return positions; }
    public void setPositions(List<Position> positions) { this.positions = positions; }
}
