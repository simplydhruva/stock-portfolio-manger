package com.stockportfolio.utils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimiter {
    private static RateLimiter instance;
    private ConcurrentHashMap<String, RequestInfo> requestCounts;
    private int maxRequestsPerMinute;
    private int maxRequestsPerHour;

    private static class RequestInfo {
        AtomicInteger minuteCount = new AtomicInteger(0);
        AtomicInteger hourCount = new AtomicInteger(0);
        LocalDateTime minuteWindow = LocalDateTime.now();
        LocalDateTime hourWindow = LocalDateTime.now();
    }

    private RateLimiter() {
        this.requestCounts = new ConcurrentHashMap<>();
        this.maxRequestsPerMinute = 60; // Default: 60 requests per minute
        this.maxRequestsPerHour = 1000; // Default: 1000 requests per hour
    }

    public static synchronized RateLimiter getInstance() {
        if (instance == null) {
            instance = new RateLimiter();
        }
        return instance;
    }

    public void setLimits(int maxPerMinute, int maxPerHour) {
        this.maxRequestsPerMinute = maxPerMinute;
        this.maxRequestsPerHour = maxPerHour;
    }

    public boolean allowRequest(String key) {
        RequestInfo info = requestCounts.computeIfAbsent(key, k -> new RequestInfo());
        LocalDateTime now = LocalDateTime.now();

        // Reset counters if time windows have passed
        resetIfNeeded(info, now);

        // Check limits
        if (info.minuteCount.get() >= maxRequestsPerMinute ||
            info.hourCount.get() >= maxRequestsPerHour) {
            return false;
        }

        // Increment counters
        info.minuteCount.incrementAndGet();
        info.hourCount.incrementAndGet();

        return true;
    }

    private void resetIfNeeded(RequestInfo info, LocalDateTime now) {
        // Reset minute counter if more than a minute has passed
        if (ChronoUnit.MINUTES.between(info.minuteWindow, now) >= 1) {
            info.minuteCount.set(0);
            info.minuteWindow = now;
        }

        // Reset hour counter if more than an hour has passed
        if (ChronoUnit.HOURS.between(info.hourWindow, now) >= 1) {
            info.hourCount.set(0);
            info.hourWindow = now;
        }
    }

    public long getRemainingRequests(String key, ChronoUnit unit) {
        RequestInfo info = requestCounts.get(key);
        if (info == null) {
            return unit == ChronoUnit.MINUTES ? maxRequestsPerMinute : maxRequestsPerHour;
        }

        resetIfNeeded(info, LocalDateTime.now());

        if (unit == ChronoUnit.MINUTES) {
            return Math.max(0, maxRequestsPerMinute - info.minuteCount.get());
        } else if (unit == ChronoUnit.HOURS) {
            return Math.max(0, maxRequestsPerHour - info.hourCount.get());
        }

        return 0;
    }

    public void waitForAvailability(String key) throws InterruptedException {
        while (!allowRequest(key)) {
            Thread.sleep(1000); // Wait 1 second before retrying
        }
    }

    public void reset(String key) {
        requestCounts.remove(key);
    }

    public void resetAll() {
        requestCounts.clear();
    }
}
