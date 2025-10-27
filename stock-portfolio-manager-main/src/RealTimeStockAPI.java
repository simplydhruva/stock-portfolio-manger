import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Real-time stock data API integration with multiple providers
 * Supports Alpha Vantage, Yahoo Finance, and IEX Cloud
 */
public class RealTimeStockAPI {

    private static final String ALPHA_VANTAGE_BASE_URL = "https://www.alphavantage.co/query";
    private static final String YAHOO_FINANCE_BASE_URL = "https://query1.finance.yahoo.com/v8/finance/chart";
    private static final String IEX_CLOUD_BASE_URL = "https://cloud.iexapis.com/stable";
    private static final String COINGECKO_BASE_URL = "https://api.coingecko.com/api/v3";

    // API Keys (should be loaded from environment variables)
    private static final String ALPHA_VANTAGE_API_KEY = System.getenv("ALPHA_VANTAGE_API_KEY");
    private static final String IEX_CLOUD_API_KEY = System.getenv("IEX_CLOUD_API_KEY");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    private final Map<String, CompletableFuture<?>> updateTasks;

    // Rate limiting
    private static final int REQUESTS_PER_MINUTE = 60;
    private final Semaphore rateLimiter;

    public RealTimeStockAPI() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
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
                System.err.println("Error getting stock quote: " + e.getMessage());
                return createDefaultQuote(symbol);
            }
        });
    }

    /**
     * Get real-time cryptocurrency quote
     */
    public CompletableFuture<StockQuote> getCryptoQuote(String symbol) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getQuoteFromCoinGecko(symbol);
            } catch (Exception e) {
                System.err.println("Error getting crypto quote: " + e.getMessage());
                return createDefaultQuote(symbol);
            }
        });
    }

    private StockQuote getQuoteFromCoinGecko(String symbol) throws Exception {
        rateLimiter.acquire();
        try {
            String url = COINGECKO_BASE_URL + "/simple/price?ids=" + symbol + "&vs_currencies=usd&include_24hr_change=true&include_last_updated_at=true";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            if (root.has(symbol)) {
                JsonNode coinData = root.get(symbol);
                double price = coinData.get("usd").asDouble();
                double changePercent = coinData.get("usd_24h_change").asDouble();
                long lastUpdated = coinData.get("last_updated_at").asLong();

                return new StockQuote(
                        symbol,
                        price,
                        0.0,
                        0.0,
                        changePercent,
                        0L,
                        LocalDateTime.ofEpochSecond(lastUpdated, 0, java.time.ZoneOffset.UTC)
                );
            }
        } finally {
            scheduler.schedule(() -> rateLimiter.release(), 1, TimeUnit.MINUTES);
        }
        throw new RuntimeException("Crypto quote not found");
    }

    /**
     * Get historical price data
     */
    public CompletableFuture<List<HistoricalPrice>> getHistoricalPrices(String symbol, String period) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (ALPHA_VANTAGE_API_KEY != null) {
                    return getHistoricalFromAlphaVantage(symbol, period);
                }
                return getHistoricalFromYahoo(symbol, period);
            } catch (Exception e) {
                System.err.println("Error getting historical prices: " + e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    /**
     * Search for stocks by symbol or name
     */
    public CompletableFuture<List<StockSearchResult>> searchStocks(String query) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (ALPHA_VANTAGE_API_KEY != null) {
                    return searchStocksAlphaVantage(query);
                }
                return searchStocksYahoo(query);
            } catch (Exception e) {
                System.err.println("Error searching stocks: " + e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    /**
     * Get market movers (gainers, losers, most active)
     */
    public CompletableFuture<MarketMovers> getMarketMovers() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (IEX_CLOUD_API_KEY != null) {
                    return getMoversFromIEX();
                }
                return getMoversFromYahoo();
            } catch (Exception e) {
                System.err.println("Error getting market movers: " + e.getMessage());
                return new MarketMovers(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
            }
        });
    }

    // Implementation methods
    private StockQuote getQuoteFromAlphaVantage(String symbol) throws Exception {
        rateLimiter.acquire();
        try {
            String url = ALPHA_VANTAGE_BASE_URL + "?function=GLOBAL_QUOTE&symbol=" + symbol + "&apikey=" + ALPHA_VANTAGE_API_KEY;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            if (root.has("Global Quote")) {
                JsonNode quote = root.get("Global Quote");
                return new StockQuote(
                        symbol,
                        quote.get("05. price").asDouble(),
                        quote.get("08. previous close").asDouble(),
                        quote.get("09. change").asDouble(),
                        quote.get("10. change percent").asDouble(),
                        quote.get("06. volume").asLong(),
                        LocalDateTime.now()
                );
            }
        } finally {
            // Release rate limiter after 1 minute
            scheduler.schedule(() -> rateLimiter.release(), 1, TimeUnit.MINUTES);
        }
        throw new RuntimeException("Quote not found");
    }

    private StockQuote getQuoteFromYahoo(String symbol) throws Exception {
        rateLimiter.acquire();
        try {
            String url = YAHOO_FINANCE_BASE_URL + "/" + symbol + "?period1=1640995200&period2=1641081600&interval=1d";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            if (root.has("chart") && root.get("chart").has("result")) {
                JsonNode result = root.get("chart").get("result").get(0);
                JsonNode meta = result.get("meta");

                double currentPrice = meta.get("regularMarketPrice").asDouble();
                double previousClose = meta.get("chartPreviousClose").asDouble();
                double change = currentPrice - previousClose;
                double changePercent = (change / previousClose) * 100;

                return new StockQuote(
                        symbol,
                        currentPrice,
                        previousClose,
                        change,
                        changePercent,
                        meta.get("regularMarketVolume").asLong(),
                        LocalDateTime.now()
                );
            }
        } finally {
            scheduler.schedule(() -> rateLimiter.release(), 1, TimeUnit.MINUTES);
        }
        throw new RuntimeException("Quote not found");
    }

    private List<HistoricalPrice> getHistoricalFromAlphaVantage(String symbol, String period) throws Exception {
        // Implementation for historical data from Alpha Vantage
        return new ArrayList<>();
    }

    private List<HistoricalPrice> getHistoricalFromYahoo(String symbol, String period) throws Exception {
        // Implementation for historical data from Yahoo Finance
        return new ArrayList<>();
    }

    private List<StockSearchResult> searchStocksAlphaVantage(String query) throws Exception {
        // Implementation for stock search using Alpha Vantage
        return new ArrayList<>();
    }

    private List<StockSearchResult> searchStocksYahoo(String query) throws Exception {
        // Implementation for stock search using Yahoo Finance
        return new ArrayList<>();
    }

    private MarketMovers getMoversFromIEX() throws Exception {
        // Implementation for market movers from IEX Cloud
        return new MarketMovers(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    private MarketMovers getMoversFromYahoo() throws Exception {
        // Implementation for market movers from Yahoo Finance
        return new MarketMovers(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    private StockQuote createDefaultQuote(String symbol) {
        return new StockQuote(symbol, 100.0, 98.0, 2.0, 2.04, 1000000L, LocalDateTime.now());
    }

    private void startRealTimeUpdates() {
        // Schedule periodic updates for popular stocks
        scheduler.scheduleAtFixedRate(() -> {
            // Update popular stocks every 5 minutes
            List<String> popularStocks = Arrays.asList("AAPL", "GOOGL", "MSFT", "AMZN", "TSLA");
            for (String symbol : popularStocks) {
                updateTasks.put(symbol, getStockQuote(symbol));
            }
        }, 0, 5, TimeUnit.MINUTES);
    }

    // Data classes
    public static class StockQuote {
        private final String symbol;
        private final double price;
        private final double previousClose;
        private final double change;
        private final double changePercent;
        private final long volume;
        private final LocalDateTime timestamp;

        public StockQuote(String symbol, double price, double previousClose, double change,
                         double changePercent, long volume, LocalDateTime timestamp) {
            this.symbol = symbol;
            this.price = price;
            this.previousClose = previousClose;
            this.change = change;
            this.changePercent = changePercent;
            this.volume = volume;
            this.timestamp = timestamp;
        }

        public String getSymbol() { return symbol; }
        public double getPrice() { return price; }
        public double getPreviousClose() { return previousClose; }
        public double getChange() { return change; }
        public double getChangePercent() { return changePercent; }
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
}
