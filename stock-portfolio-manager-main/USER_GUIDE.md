# Stock Portfolio Manager - User Guide

## Welcome to Stock Portfolio Manager

Welcome to the Stock Portfolio Manager application! This comprehensive guide will help you get started with managing your investment portfolio, executing trades, and analyzing your performance.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Dashboard Overview](#dashboard-overview)
3. [Portfolio Management](#portfolio-management)
4. [Trading](#trading)
5. [Analytics & Reports](#analytics--reports)
6. [Settings & Preferences](#settings--preferences)
7. [Notifications](#notifications)
8. [Troubleshooting](#troubleshooting)

## Getting Started

### Installation

1. **Prerequisites**: Ensure you have Java 17 or higher and Maven 3.6+ installed
2. **Download**: Clone or download the project files
3. **Build**: Run `mvn exec:java -Dexec.mainClass="App"` or use `run_app.bat` on Windows
4. **Launch**: The application will start and display the login screen

### First Time Setup

1. **Login**: Use the default credentials or create a new account
2. **Dashboard**: Familiarize yourself with the main dashboard
3. **Portfolio**: Create your first portfolio to start tracking investments

## Dashboard Overview

The dashboard is your central hub for monitoring your investments and market data.

### Key Features

#### Market Data Table
- **Real-time Updates**: Live stock prices and changes
- **Popular Stocks**: Pre-loaded with major market indices
- **Refresh Button**: Manually update market data

#### Navigation Buttons
- **Manage Portfolios**: Access portfolio management
- **Trading**: Execute buy/sell orders
- **Analytics**: View detailed reports and analysis
- **Settings**: Configure application preferences

#### Notifications Panel
- **Recent Alerts**: Trade confirmations and system messages
- **Achievement Tracking**: Gamification badges and milestones
- **Clear Notifications**: Remove read notifications

### Tutorial: Refreshing Market Data

1. Click the **"Refresh Data"** button on the dashboard
2. Wait for the table to update with latest prices
3. Green text indicates price increases, red indicates decreases
4. Volume data shows trading activity

## Portfolio Management

### Creating a Portfolio

1. From the dashboard, click **"Manage Portfolios"**
2. Click **"Create New Portfolio"**
3. Enter portfolio name and description
4. Set initial cash balance
5. Click **"Save"**

### Adding Positions

1. Select a portfolio from the list
2. Click **"Add Position"**
3. Enter stock symbol (e.g., AAPL, GOOGL)
4. Specify quantity and purchase price
5. Click **"Add"**

### Viewing Portfolio Details

- **Total Value**: Current market value of all positions
- **Total Cost Basis**: Original investment amount
- **P&L**: Profit/Loss calculation
- **Performance**: Percentage gains/losses

### Tutorial: Creating Your First Portfolio

1. Navigate to **Portfolio Screen**
2. Click **"New Portfolio"**
3. Name: "My Investment Portfolio"
4. Initial Balance: $10,000
5. Click **"Create"**
6. Add your first position (e.g., 10 shares of AAPL at $150)
7. View updated portfolio value and P&L

## Trading

### Executing a Trade

1. From dashboard, click **"Trading"**
2. Select your portfolio from dropdown
3. Enter stock symbol
4. Choose **Buy** or **Sell**
5. Enter quantity
6. Review order details
7. Click **"Execute Trade"**

### Order Types

- **Market Order**: Execute immediately at current market price
- **Limit Order**: Execute only at specified price or better

### Trade History

1. Click **"Trade History"** from main menu
2. View all executed trades
3. Filter by portfolio or date range
4. Export trade data if needed

### Tutorial: Making Your First Trade

1. Go to **Trading Screen**
2. Select your portfolio
3. Enter symbol: "AAPL"
4. Select "Buy" action
5. Quantity: 5 shares
6. Order Type: "Market"
7. Click **"Execute"**
8. Confirm the trade in the popup
9. Check notifications for confirmation

## Analytics & Reports

### Portfolio Analysis

1. Click **"Analytics"** from dashboard
2. Select **"Portfolio Analysis"** tab
3. Choose portfolio to analyze
4. View detailed breakdown:
   - Asset allocation
   - Performance metrics
   - Risk assessment

### Optimization

1. Select **"Optimization"** tab
2. Click **"Run Optimization"**
3. Review suggested portfolio adjustments
4. Apply recommendations if desired

### Rebalancing

1. Select **"Rebalancing"** tab
2. View current vs. target allocations
3. See recommended trades for rebalancing
4. Execute suggested trades

### Performance Charts

- **Portfolio Value Over Time**: Track growth
- **Asset Allocation Pie Chart**: Visual breakdown
- **Performance Comparison**: Benchmark against indices

### Tutorial: Running Portfolio Analysis

1. Navigate to **Analytics Screen**
2. Click **"Portfolio Analysis"** tab
3. Select your portfolio
4. Click **"Refresh Analysis"**
5. Review volatility, Sharpe ratio, and risk level
6. Check performance metrics and returns

## Settings & Preferences

### User Profile

1. Click **"Settings"** from dashboard
2. Update personal information
3. Change password if needed
4. Configure notification preferences

### Application Settings

- **Theme**: Light/dark mode (future feature)
- **Data Refresh Rate**: How often to update market data
- **Default Portfolio**: Set primary portfolio
- **API Configuration**: Stock data provider settings

## Notifications

### Types of Notifications

- **Trade Confirmations**: Successful order executions
- **Achievement Unlocks**: Gamification milestones
- **System Alerts**: Application updates or errors
- **Portfolio Alerts**: Rebalancing recommendations

### Managing Notifications

1. View notifications in dashboard sidebar
2. Click on notification for details
3. Mark as read or dismiss
4. Clear all notifications with **"Clear All"** button

### Achievement System

- **Trading Milestones**: Number of trades executed
- **Portfolio Growth**: Percentage gains achieved
- **Risk Management**: Maintaining low volatility
- **Consistency**: Regular trading activity

## Advanced Features

### Portfolio Comparison

1. Click **"Portfolio Comparison"** from Portfolio screen
2. Select two portfolios to compare
3. View side-by-side performance metrics
4. Analyze differences in allocation and returns

### Competitions

1. Navigate to **Competitions** screen
2. Join or create trading competitions
3. View leaderboards and rankings
4. Track competition progress

### Community Features

- **Discussion Forums**: Share trading strategies
- **Portfolio Sharing**: Show off successful portfolios
- **Market Insights**: Community-driven analysis

## Troubleshooting

### Common Issues

#### Application Won't Start
- Verify Java 17+ is installed: `java -version`
- Check Maven installation: `mvn -version`
- Ensure all dependencies are downloaded: `mvn clean install`

#### Market Data Not Loading
- Check internet connection
- Verify API keys are configured (if using premium providers)
- Try refreshing data manually
- Check application logs for error messages

#### Trade Execution Fails
- Verify sufficient portfolio balance
- Check if stock symbol is valid
- Ensure market is open (weekdays 9:30 AM - 4:00 PM EST)
- Review error message for specific details

#### Database Connection Issues
- Application uses embedded H2 database
- Data persists between sessions
- Clear database by deleting `.h2.db` files in project directory

### Performance Tips

- **Refresh Rate**: Don't refresh market data too frequently
- **Portfolio Size**: Large portfolios may take longer to analyze
- **Network**: Stable internet connection improves data loading
- **Memory**: Close other applications for better performance

### Getting Help

- Check this user guide for detailed instructions
- Review README.md for technical setup
- Check ARCHITECTURE.md for system understanding
- Review REAL_TIME_STOCK_API.md for data provider details

## Best Practices

### Portfolio Management
- Diversify across different sectors
- Regularly rebalance portfolio
- Monitor risk levels and volatility
- Set realistic investment goals

### Trading
- Research stocks before trading
- Use limit orders for better price control
- Keep detailed trading records
- Learn from both wins and losses

### Risk Management
- Don't invest more than you can afford to lose
- Use stop-loss orders to limit losses
- Diversify to reduce overall portfolio risk
- Regularly review and adjust risk tolerance

### Performance Tracking
- Set clear investment objectives
- Track progress against benchmarks
- Review performance regularly
- Learn from market trends and patterns

## Keyboard Shortcuts (Future Feature)

- `Ctrl+R`: Refresh data
- `Ctrl+T`: Open trading screen
- `Ctrl+P`: Open portfolio screen
- `Ctrl+A`: Open analytics screen
- `F1`: Open help/user guide

## Glossary

- **P&L**: Profit and Loss
- **Sharpe Ratio**: Risk-adjusted return measure
- **Volatility**: Price fluctuation measurement
- **Diversification**: Spreading investments across assets
- **Rebalancing**: Adjusting portfolio to maintain target allocations
- **Benchmark**: Standard for performance comparison (e.g., S&P 500)

---

Thank you for using Stock Portfolio Manager! We hope this guide helps you make informed investment decisions and effectively manage your portfolio. For additional support or feature requests, please refer to the project documentation or contact the development team.
