# Stock Portfolio Manager v1.1.0 - Architecture Documentation

## Overview
The Stock Portfolio Manager is a Java Swing desktop application designed to provide real-time stock market data, portfolio management, trade execution, and analytics. The architecture emphasizes modularity, asynchronous data fetching, and clean separation between UI and business logic.

---

## 1. Application Layers

### 1.1 Presentation Layer (UI)
- **Technologies:** Java Swing
- **Components:**
  - `DashboardScreen`: Displays real-time market data and notifications.
  - `TradingScreen`: Allows users to place trades with live stock data.
  - `PortfolioScreen`: Shows user portfolios and positions.
  - `AnalyticsReportsScreen`: Provides portfolio analytics, optimization, and performance charts.
  - Other screens: Login, Settings, Community, Competitions, Leaderboard, Trade History.

### 1.2 Business Logic Layer
- **Services:**
  - `RealTimeStockAPI`: Fetches real-time stock and cryptocurrency data from multiple providers asynchronously.
  - `TradeExecutor`: Manages trade execution, order tracking, and transaction history.
  - `AIAnalytics`: Performs portfolio analytics, risk assessment, and optimization.
  - `NotificationService`: Handles user notifications, achievements, and gamification.

### 1.3 Data Access Layer
- **Database:** Embedded H2 database with connection pooling via HikariCP.
- **Data Models:** User, Portfolio, Position, Transaction, Competition, LeaderboardEntry.
- **DatabaseManager:** Provides CRUD operations and abstracts database interactions.

---

## 2. Key Architectural Decisions

### 2.1 Simplified Package Structure
- Default package approach to reduce complexity and ease development.

### 2.2 Asynchronous Data Fetching
- Uses `CompletableFuture` for non-blocking API calls to keep UI responsive.

### 2.3 Multi-Provider Stock Data
- Supports Alpha Vantage, Yahoo Finance, IEX Cloud, and CoinGecko with automatic failover and rate limiting.

### 2.4 Modular UI Components
- Each screen is a separate Swing JPanel, managed by a CardLayout in the main App frame.

### 2.5 Testing Strategy
- Unit tests for core services using JUnit and Mockito.
- Integration tests for service interactions.
- UI tests for navigation and component behavior.

---

## 3. Data Flow

1. User interacts with UI components.
2. UI calls business logic services asynchronously.
3. Services fetch data from APIs or database.
4. Data is processed and returned to UI.
5. UI updates views accordingly.

---

## 4. Build and Deployment

- Managed with Maven for dependency resolution.
- Uses exec-maven-plugin for running the application.
- Future plans include creating executable JARs and installers.

---

## 5. Future Enhancements

- Advanced analytics and charting.
- Export and reporting features.
- Mobile and web extensions.
- Enhanced security and performance optimizations.

---

## 6. Summary Diagram

```
+-------------------+       +--------------------+       +------------------+
|   Presentation    | <---> |   Business Logic    | <---> |   Data Access     |
|  (Swing UI Panels) |       | (Services & APIs)   |       |  (H2 Database)   |
+-------------------+       +--------------------+       +------------------+
```

---

This architecture provides a scalable and maintainable foundation for the Stock Portfolio Manager application.
