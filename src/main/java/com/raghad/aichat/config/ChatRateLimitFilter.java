package com.raghad.aichat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raghad.aichat.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ChatRateLimitFilter extends OncePerRequestFilter {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final int requestsPerMinute;

    public ChatRateLimitFilter(
            ObjectMapper objectMapper,
            @Value("${chat.rate-limit-per-minute:20}") int requestsPerMinute
    ) {
        this.objectMapper = objectMapper;
        this.requestsPerMinute = Math.max(1, requestsPerMinute);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && "/api/chat".equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Instant now = Instant.now();
        AtomicBoolean allowed = new AtomicBoolean();

        windows.compute(request.getRemoteAddr(), (address, current) -> {
            if (current == null || Duration.between(current.startedAt(), now).compareTo(WINDOW) >= 0) {
                allowed.set(true);
                return new Window(now, 1);
            }
            if (current.count() < requestsPerMinute) {
                allowed.set(true);
                return new Window(current.startedAt(), current.count() + 1);
            }
            return current;
        });

        if (!allowed.get()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            objectMapper.writeValue(
                    response.getWriter(),
                    ErrorResponse.of("RATE_LIMITED", "Too many chat requests. Please try again in a minute.")
            );
            return;
        }

        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(entry ->
                    Duration.between(entry.getValue().startedAt(), now).compareTo(WINDOW.multipliedBy(2)) >= 0
            );
        }

        filterChain.doFilter(request, response);
    }

    private record Window(Instant startedAt, int count) {
    }
}
