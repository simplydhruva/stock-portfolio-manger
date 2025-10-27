import java.time.LocalDateTime;

public class Competition {
    private int id;
    private String name;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String rules; // JSON string of competition rules
    private String status; // "UPCOMING", "ACTIVE", "COMPLETED"
    private int maxParticipants;
    private LocalDateTime createdAt;

    // Constructors
    public Competition() {}

    public Competition(String name, String description, LocalDateTime startDate, LocalDateTime endDate, String rules, int maxParticipants) {
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.rules = rules;
        this.maxParticipants = maxParticipants;
        this.status = "UPCOMING";
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public String getRules() { return rules; }
    public void setRules(String rules) { this.rules = rules; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(int maxParticipants) { this.maxParticipants = maxParticipants; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
