package com.roadwatch.server.service;

import com.roadwatch.server.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Very small in-memory limiter: at most N citizen submissions per IP per hour. */
@Component
public class SubmissionRateLimiter {

    private static final long WINDOW_MS = 3_600_000L;

    private final int maxPerHour;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public SubmissionRateLimiter(@Value("${roadwatch.rate-limit.per-hour}") int maxPerHour) {
        this.maxPerHour = maxPerHour;
    }

    public void check(String clientKey) {
        long now = System.currentTimeMillis();
        Deque<Long> times = hits.computeIfAbsent(clientKey, k -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && now - times.peekFirst() > WINDOW_MS) {
                times.pollFirst();
            }
            if (times.size() >= maxPerHour) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                        "Too many reports from this connection. Please try again later.");
            }
            times.addLast(now);
        }
        if (hits.size() > 20_000) {
            hits.entrySet().removeIf(e -> e.getValue().isEmpty());
        }
    }
}
