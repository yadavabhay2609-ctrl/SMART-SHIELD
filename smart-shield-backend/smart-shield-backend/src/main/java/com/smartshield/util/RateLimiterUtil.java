package com.smartshield.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Thread-safe sliding-window rate limiter per client IP or key.
 * Can be injected or used as a utility across controllers, interceptors, and services.
 */
@Component
public class RateLimiterUtil {

    private final Map<String, Deque<Long>> requestLogs = new ConcurrentHashMap<>();

    // Default: 60 requests per minute
    public static final int DEFAULT_MAX_REQUESTS = 60;
    public static final long DEFAULT_WINDOW_MILLIS = 60_000L;

    /**
     * Checks if a request from the given key is allowed under default rate limits (60 req / 60 sec).
     *
     * @param key client identifier (e.g. client IP or IP:endpoint)
     * @return true if request is allowed, false if rate limited
     */
    public boolean tryAcquire(String key) {
        return tryAcquire(key, DEFAULT_MAX_REQUESTS, DEFAULT_WINDOW_MILLIS);
    }

    /**
     * Checks if a request from the given key is allowed under custom sliding-window limits.
     *
     * @param key client identifier
     * @param maxRequests maximum requests permitted within window
     * @param windowMillis sliding window duration in milliseconds
     * @return true if request is permitted, false if rate limited
     */
    public boolean tryAcquire(String key, int maxRequests, long windowMillis) {
        if (key == null || key.isBlank() || maxRequests <= 0 || windowMillis <= 0) {
            return true;
        }

        long now = System.currentTimeMillis();
        long windowStart = now - windowMillis;

        Deque<Long> timestamps = requestLogs.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            // Evict timestamps outside current sliding window
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() < maxRequests) {
                timestamps.addLast(now);
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Calculates the estimated wait time in milliseconds before a rate-limited key can make its next request.
     */
    public long getWaitTimeMillis(String key, int maxRequests, long windowMillis) {
        Deque<Long> timestamps = requestLogs.get(key);
        if (timestamps == null) {
            return 0L;
        }

        synchronized (timestamps) {
            if (timestamps.size() < maxRequests) {
                return 0L;
            }
            Long oldestTimestamp = timestamps.peekFirst();
            if (oldestTimestamp == null) {
                return 0L;
            }
            long timeUntilExpiry = (oldestTimestamp + windowMillis) - System.currentTimeMillis();
            return Math.max(0L, timeUntilExpiry);
        }
    }

    /**
     * Clears tracked history for a given key.
     */
    public void reset(String key) {
        if (key != null) {
            requestLogs.remove(key);
        }
    }

    /**
     * Clears all tracked history (e.g. for testing).
     */
    public void resetAll() {
        requestLogs.clear();
    }
}
