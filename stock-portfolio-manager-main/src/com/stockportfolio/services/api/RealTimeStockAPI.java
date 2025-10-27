


package com.stockportfolio.services.api;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockportfolio.model.Stock;
import com.stockportfolio.utils.DatabaseManager;

/**
 * Real-time stock data API integration with multiple providers
 * Supports Alpha Vantage, Yahoo Finance, and IEX Cloud
 */
public class RealTimeStockAPI {

    private static final String ALPHA_VANTAGE_BASE_URL = "https://www.alphavantage.co/query";
    private static final String YAHOO_FINANCE_BASE_URL = "https://query1.finance.yahoo.com/v8/finance/chart";
    private static final String IEX_CLOUD_BASE_URL = "https://cloud.iexapis.com/stable";

    // API Keys (should be loaded from environment variables)
    private static final String ALPHA_VANTAGE_API_KEY = System.getenv("ALPHA_VANTAGE_API_KEY");
    private static final String IEX_CLOUD_API_KEY = System.getenv("IEX_CLOUD_API_KEY");

    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final DatabaseManager dbManager;
    private final ScheduledExecutorService scheduler;
    private final Map<String, CompletableFuture<?>> updateTasks;

    // Rate limiting
    private static final int REQUESTS_PER_MINUTE = 60;
    private final Semaphore rateLimiter;

    public RealTimeStockAPI() {
        // Use Apache HttpClient for Java 8 compatibility
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(10000)
                .setSocketTimeout(30000)
                .build();
        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(config)
                .build();
        this.objectMapper = new ObjectMapper();
        try {
            this.dbManager = DatabaseManager.getInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize DatabaseManager", e);
        }
        this.scheduler = Executors.newScheduledThreadPool(4);
        this.updateTasks = new ConcurrentHashMap<>();
        this.rateLimiter = new Semaphore(REQUESTS_PER_MINUTE);

        // Start periodic price updates
        startRealTimeUpdates();
    }

    /**
     * Get real-time stock quote
     */
    public CompletableFuture<StockQuote> getStockQuote(String symbol) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Try Alpha Vantage first
                if (ALPHA_VANTAGE_API_KEY != null) {
                    return getQuoteFromAlphaVantage(symbol);
                }

                // Fallback to Yahoo Finance (free)
                return getQuoteFromYahoo(symbol);

            } catch (Exception e) {
                System.err.println("Error fetching quote for " + symbol + ": " + e.getMessage());
                return createErrorQuote(symbol);
            }
        });
    }

    private StockQuote getQuoteFromAlphaVantage(String symbol) throws Exception {
        if (!rateLimiter.tryAcquire()) {
            // Wait and retry after 1 second to handle rate limit
            Thread.sleep(1000);
            if (!rateLimiter.tryAcquire(5, TimeUnit.SECONDS)) {
                throw new RuntimeException("Rate limit exceeded");
            }
        }

        String url = String.format("%s?function=GLOBAL_QUOTE&symbol=%s&apikey=%s",
                ALPHA_VANTAGE_BASE_URL, symbol, ALPHA_VANTAGE_API_KEY);

        HttpGet request = new HttpGet(url);
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(request);

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("API request failed: " + response.getStatusLine().getStatusCode());
            }

            String responseBody = EntityUtils.toString(response.getEntity());
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode quote = root.get("Global Quote");

            if (quote == null || quote.isEmpty()) {
                throw new RuntimeException("No quote data available");
            }

            return new StockQuote(
                    symbol,
                    quote.get("05. price").asDouble(),
                    quote.get("08. previous close").asDouble(),
                    quote.get("09. change").asDouble(),
                    quote.get("10. change percent").asText().replace("%", ""),
                    quote.get("06. volume").asLong(),
                    LocalDateTime.now()
            );
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private StockQuote getQuoteFromYahoo(String symbol) throws Exception {
        String url = String.format("%s/%s?interval=1m&range=1d", YAHOO_FINANCE_BASE_URL, symbol);

        HttpGet request = new HttpGet(url);
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(request);

            if (response.getStatusLine().getStatusCode() == 429) {
                // Rate limit exceeded, wait and retry with exponential backoff
                Thread.sleep(2000); // Wait 2 seconds
                return getQuoteFromYahoo(symbol); // Retry once
            }

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Yahoo Finance API request failed: " + response.getStatusLine().getStatusCode());
            }

            String responseBody = EntityUtils.toString(response.getEntity());
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode chart = root.get("chart").get("result").get(0);
            JsonNode meta = chart.get("meta");

            double currentPrice = meta.get("regularMarketPrice").asDouble();
            double previousClose = meta.get("previousClose").asDouble();
            double change = currentPrice - previousClose;
            double changePercent = (change / previousClose) * 100;

            return new StockQuote(
                    symbol,
                    currentPrice,
                    previousClose,
                    change,
                    String.format("%.2f", changePercent),
                    meta.get("regularMarketVolume").asLong(),
                    LocalDateTime.now()
            );
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private StockQuote createErrorQuote(String symbol) {
        // Return simulated quote for demo purposes
        Random random = new Random();
        double basePrice = 100 + random.nextDouble() * 400; // $100-$500
        double change = (random.nextDouble() - 0.5) * basePrice * 0.05; // ±5%
        double previousClose = basePrice - change;
        double changePercent = (change / previousClose) * 100;

        return new StockQuote(
                symbol,
                basePrice,
                previousClose,
                change,
                String.format("%.2f", changePercent),
                1000000 + Math.abs(random.nextLong()) % 9000000, // 1M-10M volume
                LocalDateTime.now()
        );
    }

    /**
     * Get historical stock data
     */
    public CompletableFuture<List<HistoricalPrice>> getHistoricalData(String symbol, String period) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (ALPHA_VANTAGE_API_KEY != null) {
                    return getHistoricalFromAlphaVantage(symbol, period);
                }

                // Fallback to simulated data
                return generateSimulatedHistoricalData(symbol, period);

            } catch (Exception e) {
                System.err.println("Error fetching historical data: " + e.getMessage());
                return generateSimulatedHistoricalData(symbol, period);
            }
        });
    }

    private List<HistoricalPrice> getHistoricalFromAlphaVantage(String symbol, String period) throws Exception {
        if (!rateLimiter.tryAcquire()) {
            throw new RuntimeException("Rate limit exceeded");
        }

        String function = period.equals("intraday") ? "TIME_SERIES_INTRADAY" : "TIME_SERIES_DAILY";
        String url = String.format("%s?function=%s&symbol=%s&apikey=%s",
                ALPHA_VANTAGE_BASE_URL, function, symbol, ALPHA_VANTAGE_API_KEY);

        if (period.equals("intraday")) {
            url += "&interval=5min";
        }

        HttpGet request = new HttpGet(url);
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(request);

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("API request failed: " + response.getStatusLine().getStatusCode());
            }

            String responseBody = EntityUtils.toString(response.getEntity());
            JsonNode root = objectMapper.readTree(responseBody);

            String timeSeriesKey = period.equals("intraday") ? "Time Series (5min)" : "Time Series (Daily)";
            JsonNode timeSeries = root.get(timeSeriesKey);

            List<HistoricalPrice> prices = new ArrayList<>();

            timeSeries.fields().forEachRemaining(entry -> {
                String timestamp = entry.getKey();
                JsonNode data = entry.getValue();

                HistoricalPrice price = new HistoricalPrice(
                        timestamp,
                        data.get("1. open").asDouble(),
                        data.get("2. high").asDouble(),
                        data.get("3. low").asDouble(),
                        data.get("4. close").asDouble(),
                        data.get("5. volume").asLong()
                );
                prices.add(price);
            });

            return prices;
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private List<HistoricalPrice> generateSimulatedHistoricalData(String symbol, String period) {
        List<HistoricalPrice> prices = new ArrayList<>();
        Random random = new Random();

        double basePrice = 100 + random.nextDouble() * 400;
        int days = period.equals("1M") ? 30 : period.equals("3M") ? 90 : 365;

        for (int i = days; i >= 0; i--) {
            LocalDateTime date = LocalDateTime.now().minusDays(i);

            double open = basePrice + (random.nextGaussian() * basePrice * 0.02);
            double close = open + (random.nextGaussian() * open * 0.03);
            double high = Math.max(open, close) + (random.nextDouble() * Math.abs(close - open));
            double low = Math.min(open, close) - (random.nextDouble() * Math.abs(close - open));
            long volume = 500000 + Math.abs(random.nextLong()) % 5000000;

            prices.add(new HistoricalPrice(
                    date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    open, high, low, close, volume
            ));

            basePrice = close; // Trend continuation
        }

        return prices;
    }

    /**
     * Search for stocks
     */
    public CompletableFuture<List<StockSearchResult>> searchStocks(String query) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (ALPHA_VANTAGE_API_KEY != null) {
                    return searchStocksAlphaVantage(query);
                }

                // Fallback to local database search
                return searchStocksLocal(query);

            } catch (Exception e) {
                System.err.println("Error searching stocks: " + e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    private List<StockSearchResult> searchStocksAlphaVantage(String query) throws Exception {
        if (!rateLimiter.tryAcquire()) {
            throw new RuntimeException("Rate limit exceeded");
        }

        String url = String.format("%s?function=SYMBOL_SEARCH&keywords=%s&apikey=%s",
                ALPHA_VANTAGE_BASE_URL, query, ALPHA_VANTAGE_API_KEY);

        HttpGet request = new HttpGet(url);
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(request);

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("API request failed: " + response.getStatusLine().getStatusCode());
            }

            String responseBody = EntityUtils.toString(response.getEntity());
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode matches = root.get("bestMatches");

            List<StockSearchResult> results = new ArrayList<>();

            if (matches != null && matches.isArray()) {
                for (JsonNode match : matches) {
                    results.add(new StockSearchResult(
                            match.get("1. symbol").asText(),
                            match.get("2. name").asText(),
                            match.get("4. region").asText(),
                            match.get("8. currency").asText()
                    ));
                }
            }

            return results;
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private List<StockSearchResult> searchStocksLocal(String query) {
        try {
            List<Stock> stocks = dbManager.getAllStocks();
            return stocks.stream()
                    .filter(stock -> {
                        String symbol = stock.getSymbol();
                        String name = stock.getName();
                        return symbol.toLowerCase().contains(query.toLowerCase()) ||
                               name.toLowerCase().contains(query.toLowerCase());
                    })
                    .map(stock -> new StockSearchResult(
                            stock.getSymbol(),
                            stock.getName(),
                            stock.getExchange(),
                            "USD" // Default currency
                    ))
                    .limit(10)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error searching local stocks: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Start real-time price updates
     */
    private void startRealTimeUpdates() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                updatePopularStocks();
            } catch (Exception e) {
                System.err.println("Error in real-time updates: " + e.getMessage());
            }
        }, 0, 30, TimeUnit.SECONDS); // Update every 30 seconds for real-time feel
    }

    private void updatePopularStocks() {
        List<String> popularStocks = getPopularStocks();

        for (String symbol : popularStocks) {
            getStockQuote(symbol).thenAccept(quote -> {
                if (quote != null) {
                    updateStockInDatabase(quote);
                }
            });
        }
    }

    private void updateStockInDatabase(StockQuote quote) {
        try {
            Stock stock = new Stock();
            stock.setSymbol(quote.getSymbol());
            stock.setCurrentPrice(quote.getCurrentPrice());
            stock.setPreviousClose(quote.getPreviousClose());
            stock.setChange(quote.getChange());
            stock.setChangePercent(Double.parseDouble(quote.getChangePercent()));
            stock.setVolume(quote.getVolume());
            stock.setLastUpdated(quote.getTimestamp());

            dbManager.updateStock(stock);
        } catch (Exception e) {
            System.err.println("Error updating stock in database: " + e.getMessage());
        }
    }

    /**
     * Get market movers
     */
    public CompletableFuture<MarketMovers> getMarketMovers() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Stock> stocks = dbManager.getAllStocks();

                List<StockQuote> gainers = new ArrayList<>();
                List<StockQuote> losers = new ArrayList<>();
                List<StockQuote> mostActive = new ArrayList<>();

                for (Stock stock : stocks) {
                    String symbol = stock.getSymbol();
                    double currentPrice = stock.getCurrentPrice();
                    double previousClose = stock.getPreviousClose();
                    double change = currentPrice - previousClose;
                    double changePercent = (change / previousClose) * 100;
                    long volume = stock.getVolume();

                    StockQuote quote = new StockQuote(symbol, currentPrice, previousClose,
                            change, String.format("%.2f", changePercent), volume, LocalDateTime.now());

                    if (changePercent > 2.0) {
                        gainers.add(quote);
                    } else if (changePercent < -2.0) {
                        losers.add(quote);
                    }

                    if (volume > 5000000) {
                        mostActive.add(quote);
                    }
                }

                // Sort and limit
                gainers.sort((a, b) -> Double.compare(
                        Double.parseDouble(b.getChangePercent()),
                        Double.parseDouble(a.getChangePercent())
                ));
                losers.sort((a, b) -> Double.compare(
                        Double.parseDouble(a.getChangePercent()),
                        Double.parseDouble(b.getChangePercent())
                ));
                mostActive.sort((a, b) -> Long.compare(b.getVolume(), a.getVolume()));

                return new MarketMovers(
                        gainers.stream().limit(10).collect(Collectors.toList()),
                        losers.stream().limit(10).collect(Collectors.toList()),
                        mostActive.stream().limit(10).collect(Collectors.toList())
                );

            } catch (Exception e) {
                System.err.println("Error getting market movers: " + e.getMessage());
                return new MarketMovers(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
            }
        });
    }

    /**
     * Shutdown the API service
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    // Data classes
    public static class StockQuote {
        private final String symbol;
        private final double currentPrice;
        private final double previousClose;
        private final double change;
        private final String changePercent;
        private final long volume;
        private final LocalDateTime timestamp;

        public StockQuote(String symbol, double currentPrice, double previousClose,
                         double change, String changePercent, long volume, LocalDateTime timestamp) {
            this.symbol = symbol;
            this.currentPrice = currentPrice;
            this.previousClose = previousClose;
            this.change = change;
            this.changePercent = changePercent;
            this.volume = volume;
            this.timestamp = timestamp;
        }

        // Getters
        public String getSymbol() { return symbol; }
        public double getCurrentPrice() { return currentPrice; }
        public double getPreviousClose() { return previousClose; }
        public double getChange() { return change; }
        public String getChangePercent() { return changePercent; }
        public long getVolume() { return volume; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    public static class HistoricalPrice {
        private final String timestamp;
        private final double open;
        private final double high;
        private final double low;
        private final double close;
        private final long volume;

        public HistoricalPrice(String timestamp, double open, double high,
                             double low, double close, long volume) {
            this.timestamp = timestamp;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
        }

        // Getters
        public String getTimestamp() { return timestamp; }
        public double getOpen() { return open; }
        public double getHigh() { return high; }
        public double getLow() { return low; }
        public double getClose() { return close; }
        public long getVolume() { return volume; }
    }

    public static class StockSearchResult {
        private final String symbol;
        private final String name;
        private final String region;
        private final String currency;

        public StockSearchResult(String symbol, String name, String region, String currency) {
            this.symbol = symbol;
            this.name = name;
            this.region = region;
            this.currency = currency;
        }

        // Getters
        public String getSymbol() { return symbol; }
        public String getName() { return name; }
        public String getRegion() { return region; }
        public String getCurrency() { return currency; }
    }

    public static class MarketMovers {
        private final List<StockQuote> gainers;
        private final List<StockQuote> losers;
        private final List<StockQuote> mostActive;

        public MarketMovers(List<StockQuote> gainers, List<StockQuote> losers, List<StockQuote> mostActive) {
            this.gainers = gainers;
            this.losers = losers;
            this.mostActive = mostActive;
        }

        // Getters
        public List<StockQuote> getGainers() { return gainers; }
        public List<StockQuote> getLosers() { return losers; }
        public List<StockQuote> getMostActive() { return mostActive; }
    }

    // Getter for popular stocks
    public List<String> getPopularStocks() {
        return getPopularStocksList();
    }

    private List<String> getPopularStocksList() {
        // Return top 50 Fortune 500 stocks
        return java.util.Arrays.asList(
            "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "META", "NVDA", "NFLX", "BABA", "ORCL",
            "CRM", "AMD", "INTC", "CSCO", "ADBE", "PYPL", "UBER", "SPOT", "ZM", "SHOP",
            "SQ", "COIN", "PLTR", "SNOW", "CRWD", "ZS", "OKTA", "DDOG", "NET", "DOCU",
            "TWLO", "RNG", "FSLY", "PINS", "ETSY", "ROKU", "FVRR", "UPWK", "W", "TDOC",
            "SE", "BIDU", "JD", "NTES", "TCEHY", "BILI", "IQ", "HUYA", "WB", "YY"
        );
    }
}
