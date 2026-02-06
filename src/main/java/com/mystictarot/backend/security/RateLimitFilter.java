package com.mystictarot.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String RATE_LIMIT_PREFIX = "/api/v1/auth/";
    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> windowStart = new ConcurrentHashMap<>();
    private static final long WINDOW_MS = 60_000;

    @Value("${app.rate-limit.auth-max-per-minute:20}")
    private int authMaxPerMinute;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path != null && path.startsWith(RATE_LIMIT_PREFIX)) {
            String key = getClientKey(request);
            if (!allowRequest(key)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("{\"error\":\"Too many requests. Try again later.\"}");
                response.setContentType("application/json");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean allowRequest(String key) {
        long now = System.currentTimeMillis();
        windowStart.putIfAbsent(key, now);
        requestCounts.putIfAbsent(key, new AtomicInteger(0));
        long start = windowStart.get(key);
        if (now - start > WINDOW_MS) {
            windowStart.put(key, now);
            requestCounts.get(key).set(0);
        }
        return requestCounts.get(key).incrementAndGet() <= authMaxPerMinute;
    }

    private String getClientKey(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
