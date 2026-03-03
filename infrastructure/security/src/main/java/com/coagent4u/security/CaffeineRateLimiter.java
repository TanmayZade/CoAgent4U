package com.coagent4u.security;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Per-user sliding-window rate limiter backed by Caffeine cache.
 * Each user gets a counter that expires after the window duration.
 */
public class CaffeineRateLimiter {

    private final Cache<String, AtomicInteger> counters;
    private final int maxRequests;

    public CaffeineRateLimiter(int maxRequestsPerMinute) {
        this.maxRequests = maxRequestsPerMinute;
        this.counters = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(1))
                .maximumSize(10_000)
                .build();
    }

    /**
     * Returns true if the request is allowed (under the limit).
     */
    public boolean tryAcquire(String userId) {
        AtomicInteger counter = counters.get(userId, k -> new AtomicInteger(0));
        return counter.incrementAndGet() <= maxRequests;
    }

    /**
     * Returns the remaining requests for the user in the current window.
     */
    public int remaining(String userId) {
        AtomicInteger counter = counters.getIfPresent(userId);
        if (counter == null)
            return maxRequests;
        return Math.max(0, maxRequests - counter.get());
    }
}
