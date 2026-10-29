package com.privychat.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private final ConcurrentHashMap<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

    private static class RequestCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long resetTime = System.currentTimeMillis() + 60000;

        public boolean tryAcquire() {
            long now = System.currentTimeMillis();
            if (now > resetTime) {
                synchronized (this) {
                    if (now > resetTime) {
                        count.set(0);
                        resetTime = now + 60000;
                    }
                }
            }
            return count.incrementAndGet() <= MAX_REQUESTS_PER_MINUTE;
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        
        // Apply strict rate limiting on auth endpoints
        if (path.startsWith("/api/v1/auth/")) {
            String clientIp = getClientIP(request);
            RequestCounter counter = requestCounts.computeIfAbsent(clientIp, k -> new RequestCounter());

            if (!counter.tryAcquire()) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please wait a moment before retrying.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || !xfHeader.contains(",")) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}

