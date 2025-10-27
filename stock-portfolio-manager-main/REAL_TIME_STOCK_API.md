# RealTimeStockAPI Integration Documentation

## Overview

The RealTimeStockAPI is a robust, multi-provider stock data integration system that provides real-time market data to the Stock Portfolio Manager application. It supports multiple API providers with automatic failover, rate limiting, and asynchronous operations to ensure reliable and responsive stock data delivery.

## Architecture

### Provider Hierarchy
The API implements a cascading provider system with the following priority:

1. **Alpha Vantage** (Primary) - Comprehensive data with high reliability
2. **Yahoo Finance** (Secondary) - Global coverage and alternative data source
3. **IEX Cloud** (Tertiary) - Real-time and historical data specialist
4. **CoinGecko** (Cryptocurrency) - Dedicated cryptocurrency data provider

### Key Features

#### Multi-Provider Support
- Automatic failover between providers if one fails
- Provider-specific data formatting and parsing
- Unified data model across all providers

#### Rate Limiting
- 60 requests per minute global limit
- Provider-specific rate limits respected
- Automatic request queuing and throttling

#### Asynchronous Operations
- Non-blocking UI operations using CompletableFuture
- Background data fetching and caching
- Responsive user interface during API calls

#### Error Handling
- Graceful degradation with fallback data
- Comprehensive error logging and reporting
- Network timeout and retry mechanisms

## API Methods

### Core Methods

#### `getStockQuote(String symbol)`
Retrieves real-time quote for a given stock symbol.

**Parameters:**
- `symbol`: Stock ticker symbol (e.g., "AAPL", "GOOGL")

**Returns:**
- `CompletableFuture<StockQuote>`: Asynchronous quote data

**Example:**
```java
CompletableFuture<StockQuote> quote = api.getStockQuote("AAPL");
quote.thenAccept(q -> {
    System.out.println("Price: " + q.getPrice());
    System.out.println("Change: " + q.getChange());
});
```

#### `getStockQuotes(List<String> symbols)`
Retrieves quotes for multiple symbols in batch.

**Parameters:**
- `symbols`: List of stock ticker symbols

**Returns:**
- `CompletableFuture<List<StockQuote>>`: Asynchronous list of quotes

#### `getPopularStocks()`
Returns a curated list of popular stocks for dashboard display.

**Returns:**
- `List<String>`: List of popular stock symbols

## Data Models

### StockQuote
```java
public class StockQuote {
    private String symbol;
    private double price;
    private double previousClose;
    private double change;
    private String changePercent;
    private long volume;
    private LocalDateTime lastUpdated;

    // Getters and setters...
}
```

### Provider Configuration

#### Alpha Vantage
- API Key: Required for authentication
- Rate Limit: 5 calls/minute free tier, 75/minute premium
- Data: Real-time and historical quotes

#### Yahoo Finance
- No API key required
- Rate Limit: 2000 calls/hour
- Data: Global stock coverage

#### IEX Cloud
- API Key: Required
- Rate Limit: 50,000 calls/month free tier
- Data: Real-time and historical data

#### CoinGecko
- No API key required
- Rate Limit: 10-30 calls/minute
- Data: Cryptocurrency market data

## Configuration

### API Keys Setup
API keys should be configured in the `RealTimeStockAPI.java` constructor:

```java
private static final String ALPHA_VANTAGE_API_KEY = "your_alpha_vantage_key";
private static final String IEX_CLOUD_API_KEY = "your_iex_cloud_key";
```

### Provider Priority
The provider priority can be customized by modifying the `getStockQuote` method's provider selection logic.

## Error Handling

### Common Error Scenarios

1. **Network Timeout**: Automatic retry with exponential backoff
2. **API Rate Limit**: Request queuing and delayed retry
3. **Provider Failure**: Automatic failover to next provider
4. **Invalid Symbol**: Returns null or default quote

### Error Recovery
- Failed requests are logged with detailed error information
- Fallback to cached data when available
- Graceful degradation to default values

## Performance Optimization

### Caching Strategy
- In-memory cache for frequently requested symbols
- Cache expiration based on provider update frequency
- Background cache refresh for popular stocks

### Request Batching
- Multiple symbol requests batched into single API calls
- Reduced network overhead and improved performance
- Provider-specific batch size limits respected

## Testing

### Unit Tests
Comprehensive unit tests are available in `RealTimeStockAPITest.java`:

- Mocked API responses for all providers
- Error scenario testing
- Rate limiting verification
- Asynchronous operation testing

### Integration Testing
Full system integration tests verify end-to-end functionality with real API calls (requires valid API keys).

## Usage Examples

### Basic Quote Retrieval
```java
RealTimeStockAPI api = new RealTimeStockAPI();

// Get single quote
api.getStockQuote("AAPL").thenAccept(quote -> {
    if (quote != null) {
        System.out.println(quote.getSymbol() + ": $" + quote.getPrice());
    }
});

// Get multiple quotes
List<String> symbols = Arrays.asList("AAPL", "GOOGL", "MSFT");
api.getStockQuotes(symbols).thenAccept(quotes -> {
    quotes.forEach(quote -> {
        System.out.println(quote.getSymbol() + ": $" + quote.getPrice());
    });
});
```

### Dashboard Integration
```java
// Update dashboard with popular stocks
List<String> popularStocks = api.getPopularStocks();
for (String symbol : popularStocks) {
    api.getStockQuote(symbol).thenAccept(quote -> {
        // Update UI with quote data
        updateStockTable(quote);
    });
}
```

## Monitoring and Maintenance

### Health Checks
- Provider availability monitoring
- Response time tracking
- Error rate monitoring

### Maintenance Tasks
- API key rotation
- Provider endpoint updates
- Rate limit adjustments

## Troubleshooting

### Common Issues

1. **API Key Invalid**: Verify API keys are correctly configured
2. **Rate Limit Exceeded**: Implement request throttling or upgrade API tier
3. **Network Issues**: Check internet connectivity and firewall settings
4. **Provider Down**: Automatic failover should handle this; monitor logs

### Debug Mode
Enable debug logging to trace API calls and responses:

```java
System.setProperty("java.util.logging.level", "FINE");
```

## Future Enhancements

- WebSocket support for real-time streaming data
- Historical data caching and analysis
- Machine learning-based price prediction
- Advanced technical indicators
- Multi-exchange support
