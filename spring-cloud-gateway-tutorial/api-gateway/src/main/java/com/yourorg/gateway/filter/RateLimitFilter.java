package com.yourorg.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.gateway.config.GatewayProperties;
import com.yourorg.gateway.model.GatewayErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Token bucket rate limiter (REQ-RL-001 through REQ-RL-022).
 * In-memory implementation with per-tenant, per-IP, and per-user limiting.
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final GatewayProperties.RateLimitProperties rateLimitConfig;
    private final ObjectMapper objectMapper;

    private final Map<String, TokenBucket> tenantBuckets = new ConcurrentHashMap<>();
    private final Map<String, TokenBucket> ipBuckets = new ConcurrentHashMap<>();
    private final Map<String, TokenBucket> userBuckets = new ConcurrentHashMap<>();

    public RateLimitFilter(GatewayProperties properties, ObjectMapper objectMapper) {
        this.rateLimitConfig = properties.rateLimit();
        this.objectMapper = objectMapper;
        log.info("Rate limiting initialized: tenant={}/{}s, ip={}/{}s, user={}/{}s",
                rateLimitConfig.perTenantLimit(), rateLimitConfig.windowSeconds(),
                rateLimitConfig.perIpLimit(), rateLimitConfig.windowSeconds(),
                rateLimitConfig.perUserLimit(), rateLimitConfig.windowSeconds());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Exempt health/metrics endpoints (REQ-RL-013)
        if (path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }

        String clientIp = resolveClientIp(request);
        String tenantId = exchange.getAttribute("tenantId");
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");

        // Check IP rate limit
        TokenBucket ipBucket = ipBuckets.computeIfAbsent(clientIp,
                k -> new TokenBucket(rateLimitConfig.perIpLimit(), rateLimitConfig.windowSeconds()));
        if (!ipBucket.tryConsume()) {
            return rejectRateLimited(exchange, "IP rate limit exceeded", ipBucket);
        }

        // Check tenant rate limit if tenant is identified
        if (tenantId != null && !tenantId.isBlank()) {
            TokenBucket tenantBucket = tenantBuckets.computeIfAbsent(tenantId,
                    k -> new TokenBucket(rateLimitConfig.perTenantLimit(), rateLimitConfig.windowSeconds()));
            if (!tenantBucket.tryConsume()) {
                return rejectRateLimited(exchange, "Tenant rate limit exceeded", tenantBucket);
            }
        }

        // Check user rate limit if user is identified
        if (userId != null && !userId.isBlank()) {
            TokenBucket userBucket = userBuckets.computeIfAbsent(userId,
                    k -> new TokenBucket(rateLimitConfig.perUserLimit(), rateLimitConfig.windowSeconds()));
            if (!userBucket.tryConsume()) {
                return rejectRateLimited(exchange, "User rate limit exceeded", userBucket);
            }
        }

        // Add rate limit headers to response (REQ-RL-011)
        ServerHttpResponse response = exchange.getResponse();
        TokenBucket primaryBucket = ipBucket;
        response.getHeaders().add("X-RateLimit-Limit", String.valueOf(primaryBucket.getLimit()));
        response.getHeaders().add("X-RateLimit-Remaining", String.valueOf(primaryBucket.getRemaining()));
        response.getHeaders().add("X-RateLimit-Reset", String.valueOf(primaryBucket.getResetEpochSeconds()));

        return chain.filter(exchange);
    }

    private Mono<Void> rejectRateLimited(ServerWebExchange exchange, String reason, TokenBucket bucket) {
        log.warn("Rate limit exceeded: {} for request {}", reason, exchange.getRequest().getPath());

        String correlationId = exchange.getAttributeOrDefault(
                CorrelationIdFilter.CORRELATION_ID_ATTR, "unknown");

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().add("X-RateLimit-Limit", String.valueOf(bucket.getLimit()));
        response.getHeaders().add("X-RateLimit-Remaining", "0");
        response.getHeaders().add("X-RateLimit-Reset", String.valueOf(bucket.getResetEpochSeconds()));
        response.getHeaders().add("Retry-After", String.valueOf(bucket.getRetryAfterSeconds()));

        GatewayErrorResponse errorResponse = GatewayErrorResponse.of(
                429, "Too Many Requests", reason, correlationId);

        try {
            byte[] body = objectMapper.writeValueAsBytes(errorResponse);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
        } catch (Exception e) {
            return response.setComplete();
        }
    }

    private String resolveClientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 3;
    }

    /**
     * Token bucket rate limiting algorithm (REQ-RL-012).
     */
    static class TokenBucket {
        private final int maxTokens;
        private final long windowMs;
        private final AtomicLong tokens;
        private volatile long lastRefillTime;

        TokenBucket(int maxTokens, int windowSeconds) {
            this.maxTokens = maxTokens;
            this.windowMs = windowSeconds * 1000L;
            this.tokens = new AtomicLong(maxTokens);
            this.lastRefillTime = System.currentTimeMillis();
        }

        synchronized boolean tryConsume() {
            refill();
            long current = tokens.get();
            if (current > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTime;
            if (elapsed >= windowMs) {
                tokens.set(maxTokens);
                lastRefillTime = now;
            } else {
                long tokensToAdd = (elapsed * maxTokens) / windowMs;
                if (tokensToAdd > 0) {
                    long newTokens = Math.min(maxTokens, tokens.get() + tokensToAdd);
                    tokens.set(newTokens);
                    lastRefillTime = now;
                }
            }
        }

        int getLimit() {
            return maxTokens;
        }

        long getRemaining() {
            return tokens.get();
        }

        long getResetEpochSeconds() {
            return (lastRefillTime + windowMs) / 1000;
        }

        long getRetryAfterSeconds() {
            long remaining = (lastRefillTime + windowMs) - System.currentTimeMillis();
            return Math.max(1, remaining / 1000);
        }
    }
}
