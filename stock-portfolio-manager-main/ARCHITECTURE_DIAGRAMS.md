# Stock Portfolio Manager - Architecture Diagrams

## Table of Contents
1. [System Architecture Overview](#system-architecture-overview)
2. [Package Structure](#package-structure)
3. [Data Flow Diagrams](#data-flow-diagrams)
4. [Component Interaction Diagrams](#component-interaction-diagrams)
5. [Database Schema](#database-schema)
6. [API Integration Architecture](#api-integration-architecture)

## System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           STOCK PORTFOLIO MANAGER                           │
│                           Java Swing Application                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐             │
│  │   UI Layer      │  │ Business Logic  │  │  Data Layer     │             │
│  │                 │  │                 │  │                 │             │
│  │  • Dashboard    │  │  • TradeExecutor│  │  • Database     │             │
│  │  • Trading      │  │  • AIAnalytics │  │  • Models        │             │
│  │  • Portfolio    │  │  • ExportService│  │  • SQLite DB    │             │
│  │  • Analytics    │  │  • Notification │  │                 │             │
│  │  • Settings     │  │                 │  │                 │             │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘             │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐             │
│  │ External APIs   │  │   Services      │  │   Utilities     │             │
│  │                 │  │                 │  │                 │             │
│  │ • RealTimeStock │  │ • Notifications │  │ • DatabaseMgr   │             │
│  │ • Alpha Vantage │  │ • Export        │  │ • File I/O      │             │
│  │ • Yahoo Finance │  │ • Analytics     │  │ • UI Helpers    │             │
│  │ • IEX Cloud     │  │                 │  │                 │             │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Package Structure

```
com.stockportfolio
├── model/                          # Data Models
│   ├── User.java                   # User entity
│   ├── Portfolio.java              # Portfolio entity
│   ├── Position.java               # Stock position entity
│   ├── Transaction.java            # Trade transaction entity
│   ├── Competition.java            # Trading competition entity
│   ├── LeaderboardEntry.java       # Competition ranking entity
│   └── Stock.java                  # Stock data model
│
├── services/                       # Business Services
│   ├── analytics/
│   │   └── AIAnalytics.java        # AI-powered analytics engine
│   ├── notifications/
│   │   └── NotificationService.java # User notifications & gamification
│   └── ExportService.java          # Data export functionality
│
├── utils/                          # Utility Classes
│   └── DatabaseManager.java        # Database operations
│
└── screens/                        # UI Screens (Default Package)
    ├── App.java                    # Main application class
    ├── DashboardScreen.java        # Main dashboard
    ├── TradingScreen.java          # Stock trading interface
    ├── PortfolioScreen.java        # Portfolio management
    ├── AnalyticsReportsScreen.java # Analytics & reports
    ├── TradeHistoryScreen.java     # Trade history viewer
    ├── SettingsScreen.java         # Application settings
    ├── LoginScreen.java            # User authentication
    ├── CommunityScreen.java        # Community features
    ├── CompetitionsScreen.java     # Trading competitions
    ├── LeaderboardScreen.java      # Competition rankings
    ├── TradeExecutor.java          # Trade execution service
    └── AdvancedChartPanel.java     # Chart visualization
```

## Data Flow Diagrams

### User Authentication Flow
```
User Login Request
        ↓
    LoginScreen
        ↓
  DatabaseManager
        ↓
   User Validation
        ↓
   Session Creation
        ↓
   Dashboard Display
```

### Stock Trading Flow
```
User Trade Request
        ↓
   TradingScreen
        ↓
  RealTimeStockAPI
        ↓
   Price Validation
        ↓
   TradeExecutor
        ↓
  DatabaseManager
        ↓
 Transaction Record
        ↓
Portfolio Update
        ↓
   UI Refresh
```

### Analytics Data Flow
```
Portfolio Data Request
        ↓
AnalyticsReportsScreen
        ↓
   AIAnalytics Service
        ↓
 DatabaseManager (Positions)
        ↓
   AI Analysis Engine
        ↓
Optimization Algorithms
        ↓
   Results Display
```

## Component Interaction Diagrams

### Real-Time Stock Data Integration
```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   TradingScreen │────▶│ RealTimeStockAPI │────▶│  External APIs  │
│                 │◀────│                 │◀────│                 │
│  Display Quotes │     │ Rate Limiting   │     │ Alpha Vantage   │
│  Update Prices  │     │ Async Processing│     │ Yahoo Finance   │
└─────────────────┘     └─────────────────┘     │ IEX Cloud       │
                                               │ CoinGecko       │
                                               └─────────────────┘
```

### Database Interaction Pattern
```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   UI Screens    │────▶│ DatabaseManager │────▶│   SQLite DB     │
│                 │◀────│                 │◀────│                 │
│  Data Requests  │     │ CRUD Operations │     │ Tables:         │
│  Display Data   │     │ Connection Pool │     │ • users         │
└─────────────────┘     └─────────────────┘     │ • portfolios    │
                                               │ • positions      │
                                               │ • transactions   │
                                               │ • competitions   │
                                               └─────────────────┘
```

### Service Layer Architecture
```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   UI Controllers│────▶│  Service Layer  │────▶│   Data Access   │
│                 │◀────│                 │◀────│                 │
│  User Actions   │     │ Business Logic  │     │   Database      │
│  Event Handling │     │ Validation      │     │   External APIs │
└─────────────────┘     │ Processing      │     └─────────────────┘
                       │ Notifications    │
                       │ Export Services  │
                       └─────────────────┘
```

## Database Schema

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                               DATABASE SCHEMA                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  users (User Authentication)                                                │
│  ├─ id (PRIMARY KEY)                                                        │
│  ├─ username (UNIQUE)                                                       │
│  ├─ password_hash                                                            │
│  ├─ email                                                                   │
│  ├─ created_at                                                              │
│  └─ last_login                                                              │
│                                                                             │
│  portfolios (Portfolio Management)                                          │
│  ├─ id (PRIMARY KEY)                                                        │
│  ├─ user_id (FOREIGN KEY → users.id)                                        │
│  ├─ name                                                                    │
│  ├─ description                                                             │
│  ├─ total_value                                                             │
│  ├─ total_cost_basis                                                        │
│  ├─ total_pnl                                                               │
│  └─ created_at                                                              │
│                                                                             │
│  positions (Stock Positions)                                                │
│  ├─ id (PRIMARY KEY)                                                        │
│  ├─ portfolio_id (FOREIGN KEY → portfolios.id)                              │
│  ├─ symbol                                                                  │
│  ├─ quantity                                                                │
│  ├─ average_cost                                                            │
│  ├─ current_price                                                           │
│  ├─ total_cost                                                              │
│  ├─ market_value                                                            │
│  └─ last_updated                                                            │
│                                                                             │
│  transactions (Trade History)                                               │
│  ├─ id (PRIMARY KEY)                                                        │
│  ├─ user_id (FOREIGN KEY → users.id)                                        │
│  ├─ portfolio_id (FOREIGN KEY → portfolios.id)                              │
│  ├─ symbol                                                                  │
│  ├─ type (BUY/SELL)                                                         │
│  ├─ quantity                                                                │
│  ├─ price                                                                   │
│  ├─ total_amount                                                            │
│  ├─ timestamp                                                               │
│  └─ status                                                                  │
│                                                                             │
│  competitions (Trading Competitions)                                        │
│  ├─ id (PRIMARY KEY)                                                        │
│  ├─ name                                                                    │
│  ├─ description                                                             │
│  ├─ start_date                                                              │
│  ├─ end_date                                                                │
│  ├─ status                                                                  │
│  └─ rules                                                                   │
│                                                                             │
│  competition_participants                                                   │
│  ├─ competition_id (FOREIGN KEY → competitions.id)                          │
│  ├─ user_id (FOREIGN KEY → users.id)                                        │
│  ├─ joined_at                                                               │
│  └─ final_rank                                                              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## API Integration Architecture

### RealTimeStockAPI Architecture
```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        REAL TIME STOCK API                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐             │
│  │   API Manager   │  │  Rate Limiter   │  │ Token Manager   │             │
│  │                 │  │                 │  │                 │             │
│  │ • Provider      │  │ • 60 req/min    │  │ • API Keys      │             │
│  │   Selection     │  │ • Queue Mgmt    │  │ • Rotation      │             │
│  │ • Fallback      │  │ • Async Queue   │  │ • Validation    │             │
│  │ • Error Handling│  └─────────────────┘  └─────────────────┘             │
│  └─────────────────┘                                                        │
│           │                                                                 │
│           ▼                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐             │
│  │ Alpha Vantage   │  │ Yahoo Finance   │  │   IEX Cloud     │             │
│  │                 │  │                 │  │                 │             │
│  │ • Intraday      │  │ • Real-time     │  │ • Real-time     │             │
│  │ • Historical    │  │ • Historical    │  │ • Historical    │             │
│  │ • Fundamentals  │  │ • Fundamentals  │  │ • Fundamentals  │             │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘             │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐             │
│  │   Data Parser   │  │ Error Handler   │  │ Cache Manager   │             │
│  │                 │  │                 │  │                 │             │
│  │ • JSON/XML      │  │ • Retry Logic   │  │ • In-memory     │             │
│  │ • Normalization │  │ • Fallback      │  │ • TTL           │             │
│  │ • Validation    │  │ • User Alerts   │  │ • Performance   │             │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Service Integration Flow
```
UI Request → Service Layer → Business Logic → Data Access → External APIs
    ↑              ↑              ↑              ↑              ↑
Response ← UI Update ← Result Processing ← Data Retrieval ← API Response
```

## Component Dependencies

```
App (Main)
├── DashboardScreen
├── TradingScreen
│   └── RealTimeStockAPI
├── PortfolioScreen
├── AnalyticsReportsScreen
│   └── AIAnalytics
├── TradeHistoryScreen
│   └── ExportService
├── SettingsScreen
├── LoginScreen
├── CommunityScreen
├── CompetitionsScreen
├── LeaderboardScreen
├── TradeExecutor
├── NotificationService
├── DatabaseManager
└── ExportService
```

## Security Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           SECURITY LAYERS                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐             │
│  │ Authentication  │  │ Authorization   │  │ Data Validation │             │
│  │                 │  │                 │  │                 │             │
│  │ • Password Hash │  │ • User Sessions │  │ • Input Sanitize│             │
│  │ • Session Mgmt  │  │ • Access Ctrl   │  │ • SQL Injection │             │
│  │ • Login Attempts│  │ • Portfolio Own │  │ • XSS Prevention│             │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘             │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐             │
│  │ API Security    │  │ Database Sec    │  │ File Security   │             │
│  │                 │  │                 │  │                 │             │
│  │ • API Keys      │  │ • Prepared Stmt │  │ • Safe Paths    │             │
│  │ • Rate Limiting │  │ • Connection    │  │ • Permissions   │             │
│  │ • Request Valid │  │ • Encryption    │  │ • Export Sec    │             │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Performance Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         PERFORMANCE OPTIMIZATION                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐             │
│  │ Async Processing│  │ Caching Layer   │  │ Database Opt    │             │
│  │                 │  │                 │  │                 │             │
│  │ • CompletableFut│  │ • Stock Prices  │  │ • Connection Pool│             │
│  │ • UI Non-block  │  │ • API Responses │  │ • Prepared Stmt  │             │
│  │ • Background    │  │ • TTL Strategy  │  │ • Indexing       │             │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘             │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐             │
│  │ Memory Mgmt     │  │ UI Optimization │  │ Resource Mgmt   │             │
│  │                 │  │                 │  │                 │             │
│  │ • Object Pool   │  │ • Lazy Loading  │  │ • Connection Cls│             │
│  │ • GC Friendly   │  │ • Virtual Scroll│  │ • API Limits    │             │
│  │ • Cache Size    │  │ • Progressive UI│  │ • Error Recovery│             │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Deployment Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          DEPLOYMENT ARCHITECTURE                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐             │
│  │ Development     │  │ Testing         │  │ Production      │             │
│  │ Environment     │  │ Environment     │  │ Environment     │             │
│  │                 │  │                 │  │                 │             │
│  │ • Local SQLite  │  │ • Test Database │  │ • Production DB │             │
│  │ • Debug Mode    │  │ • Mock APIs     │  │ • Live APIs      │             │
│  │ • Dev Tools     │  │ • Unit Tests    │  │ • Monitoring     │             │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘             │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐             │
│  │ Build Process   │  │ Distribution    │  │ Runtime Config  │             │
│  │                 │  │                 │  │                 │             │
│  │ • Maven/Ant     │  │ • JAR File      │  │ • Config Files  │             │
│  │ • Dependencies  │  │ • Lib Folder    │  │ • Environment   │             │
│  │ • Resource Bundl│  │ • Executable    │  │ • API Keys      │             │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

*This architecture documentation provides a comprehensive view of the Stock Portfolio Manager application's design, components, and interactions. The diagrams show the layered architecture, data flows, and integration patterns that make up the system.*
