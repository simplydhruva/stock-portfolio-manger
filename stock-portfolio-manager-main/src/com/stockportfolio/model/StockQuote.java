package com.stockportfolio.model;

public class StockQuote {
    private String symbol;
    private double price;
    private double change;
    private double changePercent;
    private long volume;

    public StockQuote(String symbol, double price, double change, double changePercent, long volume) {
        this.symbol = symbol;
        this.price = price;
        this.change = change;
        this.changePercent = changePercent;
        this.volume = volume;
    }

    // Getters
    public String getSymbol() { return symbol; }
    public double getPrice() { return price; }
    public double getChange() { return change; }
    public double getChangePercent() { return changePercent; }
    public long getVolume() { return volume; }
}
