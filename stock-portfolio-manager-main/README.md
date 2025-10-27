# Stock Portfolio Manager v1.1.0

A comprehensive Java Swing-based stock portfolio management application with real-time market data, advanced charting, and complete trading features.

## Features

### Core Functionality
- **User Authentication**: Secure login with demo user (username: "demo", password: "demo123")
- **Portfolio Management**: Create and manage investment portfolios with real-time valuation
- **Real-Time Stock Data**: Live market quotes from Yahoo Finance API with 30-second updates
- **Trading Interface**: Execute buy/sell orders with comprehensive transaction tracking
- **Trade History**: Complete transaction history with detailed records and P&L analysis
- **Advanced Charts**: Bar charts showing buy price, current price, and sell price for owned stocks
- **Comprehensive Dashboard**: Top 50 Fortune 500 stocks with real-time market data

### User Experience
- **Dashboard**: Real-time market overview with portfolio value, P&L, and trade metrics
- **Watchlist**: Track favorite stocks with price alerts
- **Settings**: Clean theme toggle (light/dark mode)
- **Multi-Screen Navigation**: Intuitive sidebar navigation between all features

### Technical Features
- **Multi-Provider API Support**: Yahoo Finance with automatic error handling and rate limiting
- **Asynchronous Operations**: Non-blocking UI with CompletableFuture for smooth performance
- **Rate Limiting**: Smart API management (60 requests/minute) with semaphore-based control
- **Error Handling**: Robust error handling with graceful degradation and user feedback
- **Database Integration**: H2 embedded database with HikariCP connection pooling
- **Build System**: Maven-based project with comprehensive dependency management

## Architecture

### Project Structure
```
src/
├── App.java                          # Main application entry point with screen management
├── DashboardScreen.java              # Main dashboard with Fortune 500 stocks overview
├── TradingScreen.java                # Stock trading interface with order execution
├── WatchlistScreen.java              # Stock watchlist management and alerts
├── SettingsScreen.java               # Theme toggle and basic preferences
├── SidebarPanel.java                 # Navigation sidebar with screen switching
├── AdvancedChartPanel.java           # Portfolio price visualization (bar charts)
├── RealTimeStockAPI.java             # Yahoo Finance API integration with rate limiting
├── DatabaseManager.java              # H2 database operations with connection pooling
└── model/                            # Data models
    ├── User.java                     # User authentication and profile data
    ├── Portfolio.java                # Portfolio container with metadata
    ├── Position.java                 # Individual stock holdings with pricing
    ├── Transaction.java              # Trade records with timestamps
    └── Stock.java                    # Stock metadata and market data
```

### Key Components

#### RealTimeStockAPI
- **Primary Provider**: Yahoo Finance API for comprehensive market data
- **Update Frequency**: 30-second intervals for real-time pricing
- **Rate Limiting**: Semaphore-based control (60 requests/minute)
- **Error Handling**: Automatic retry logic with exponential backoff
- **Asynchronous**: CompletableFuture-based operations for UI responsiveness
- **Data Coverage**: Top 50 Fortune 500 stocks plus user-specified symbols

#### AdvancedChartPanel
- **Visualization**: Bar charts for portfolio positions (Buy/Current/Sell prices)
- **Data Source**: Real portfolio positions and transaction history
- **Color Coding**: Blue (Buy), Green (Current), Red (Sell) with legend
- **Real-Time Updates**: Charts refresh with latest market data
- **Error Handling**: Graceful display when no portfolio data available

#### DatabaseManager
- **Database Engine**: H2 embedded database (file-based)
- **Connection Pooling**: HikariCP for efficient connection management
- **Operations**: CRUD operations for users, portfolios, positions, transactions
- **Initialization**: Automatic schema creation and demo data seeding
- **Thread Safety**: Proper synchronization for concurrent access

#### DashboardScreen
- **Stock Display**: All top 50 Fortune 500 stocks with real-time data
- **Portfolio Metrics**: Value, P&L, and trade count info cards
- **Data Loading**: Asynchronous loading with progress indication
- **Navigation**: Direct access to portfolios, analytics, and trading

## Getting Started

### Prerequisites
- **Java**: JDK 8 or higher (recommended: Java 11+)
- **Maven**: 3.6 or higher (recommended: 3.8+)
- **Internet**: Stable connection for real-time stock data
- **OS**: Windows 10+, macOS 10.14+, Linux (Ubuntu 18.04+)

### Installation Steps

#### Windows Installation
1. **Install Java JDK**:
   - Download from Adoptium: https://adoptium.net/
   - Set `JAVA_HOME` environment variable
   - Add `%JAVA_HOME%\bin` to PATH

2. **Install Maven**:
   - Download from: https://maven.apache.org/download.cgi
   - Set `MAVEN_HOME` environment variable
   - Add `%MAVEN_HOME%\bin` to PATH

3. **Verify Installation**:
   ```cmd
   java -version
   mvn -version
   ```

#### Linux/macOS Installation
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-11-jdk maven

# Verify
java -version
mvn -version
```

### Running the Application

#### Method 1: Maven Execution (Recommended)
```bash
cd stock-portfolio-manager
mvn clean compile
mvn exec:java -Dexec.mainClass="App"
```

#### Method 2: Windows Batch Script
```cmd
RunStockPortfolio.bat
```

#### Method 3: Manual Java Execution
```bash
mvn compile
java -cp "target/classes;target/lib/*" App
```

### First Run Setup
- **Database**: Automatically created in `data/` directory
- **Demo User**: Username: "demo", Password: "demo123"
- **Demo Data**: Sample portfolios and transactions created automatically

## API Integration

### Yahoo Finance API
- **Data Source**: Primary provider for real-time stock quotes
- **Coverage**: Global stocks with comprehensive market data
- **Authentication**: No API key required (free tier)
- **Rate Limits**: 60 requests per minute with automatic throttling
- **Fallback**: Graceful error handling for unavailable symbols

### Data Provided
- **Real-time Quotes**: Current price, daily change, volume
- **Historical Data**: Price history for charting (when implemented)
- **Market Status**: Trading hours and market state
- **Error Handling**: 404 for delisted symbols, network errors, etc.

## Development Status

### ✅ Completed Features
- Real-time stock data integration (Yahoo Finance)
- Comprehensive portfolio management with P&L tracking
- Advanced bar chart visualization for portfolio positions
- Multi-screen Java Swing UI with theme support
- H2 database with connection pooling and demo data
- Asynchronous operations for UI responsiveness
- Rate limiting and error handling for API calls
- Maven-based build system with dependency management

### 📋 Future Enhancements
- WebSocket support for true real-time updates
- Advanced technical indicators and candlestick charts
- Mobile application version (Android/iOS)
- Real broker API integration (Interactive Brokers, etc.)
- Advanced analytics and portfolio optimization
- Social features and portfolio sharing
- Export functionality (PDF reports, CSV data)

## Testing & Quality Assurance

### Verified Functionality
- ✅ Application compilation and startup
- ✅ Database initialization and demo data creation
- ✅ User authentication and session management
- ✅ Dashboard loading of 50 Fortune 500 stocks
- ✅ Real-time price updates (30-second intervals)
- ✅ Portfolio value and P&L calculations
- ✅ Chart visualization with buy/current/sell prices
- ✅ Theme toggle functionality
- ✅ Navigation between all screens
- ✅ Batch script execution and error handling

### Known Behaviors
- Some Fortune 500 stocks (SQ, YY) return 404 errors (delisted symbols)
- Application gracefully handles API failures and continues operation
- Charts only display for stocks with active portfolio positions

## Contributing

1. **Fork** the repository
2. **Create** a feature branch: `git checkout -b feature/new-feature`
3. **Make** your changes with proper documentation
4. **Test** thoroughly across different scenarios
5. **Commit** with clear messages: `git commit -m "Add new feature"`
6. **Push** to your branch: `git push origin feature/new-feature`
7. **Submit** a pull request with detailed description

## Troubleshooting

### Common Issues

#### Build Problems
```bash
# Clean and rebuild
mvn clean compile

# Check for dependency issues
mvn dependency:tree
```

#### Runtime Issues
- **Database locked**: Delete `data/stockportfolio.lock.db` and restart
- **API errors**: Check internet connection and Yahoo Finance availability
- **Java version**: Ensure Java 8+ is installed and JAVA_HOME is set

#### Performance Issues
- **Slow startup**: Check available RAM (minimum 512MB recommended)
- **UI freezing**: Ensure stable internet for real-time updates
- **Memory usage**: Monitor JVM heap usage with task manager

## License

This project is licensed under the MIT License - see LICENSE file for details.

## Support

### Getting Help
- **Documentation**: Check README.md and inline code comments
- **Logs**: Review `application.log` for error details
- **Community**: Open issues on GitHub for bugs and feature requests

### System Requirements
- **Minimum RAM**: 512MB
- **Recommended RAM**: 1GB+
- **Disk Space**: 200MB free space
- **Network**: Stable internet connection required

## Version History

- **v1.1.0** (Current): Enhanced dashboard with Fortune 500 stocks, improved charts, theme support
- **v1.0.0**: Initial release with core portfolio management and basic charting
