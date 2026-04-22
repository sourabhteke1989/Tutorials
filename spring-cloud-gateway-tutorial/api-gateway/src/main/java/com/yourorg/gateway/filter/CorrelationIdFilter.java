package com.yourorg.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Generates or propagates a Correlation ID for every request (REQ-OB-004).
 * Supports W3C traceparent, X-Correlation-ID, and X-Request-ID headers.
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String TRACEPARENT_HEADER = "traceparent";
    public static final String CORRELATION_ID_ATTR = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String correlationId = resolveCorrelationId(request);

        exchange.getAttributes().put(CORRELATION_ID_ATTR, correlationId);

        ServerHttpRequest mutatedRequest = request.mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .build();

        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().add(CORRELATION_ID_HEADER, correlationId);

        MDC.put("correlationId", correlationId);
        MDC.put("requestPath", request.getPath().value());
        MDC.put("httpMethod", request.getMethod().name());

        long startTime = System.currentTimeMillis();

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .doFinally(signalType -> {
                    long duration = System.currentTimeMillis() - startTime;
                    MDC.put("durationMs", String.valueOf(duration));
                    MDC.put("httpStatus", String.valueOf(
                            response.getStatusCode() != null ? response.getStatusCode().value() : 0));
                    log.info("Request completed: {} {} -> {} ({}ms)",
                            request.getMethod(), request.getPath().value(),
                            response.getStatusCode(), duration);
                    MDC.clear();
                });
    }

    private String resolveCorrelationId(ServerHttpRequest request) {
        // 1. Check traceparent
        String traceparent = request.getHeaders().getFirst(TRACEPARENT_HEADER);
        if (traceparent != null && !traceparent.isBlank()) {
            return traceparent;
        }

        // 2. Check X-Correlation-ID
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId != null && !correlationId.isBlank()) {
            return correlationId;
        }

        // 3. Check X-Request-ID
        String requestId = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId != null && !requestId.isBlank()) {
            return requestId;
        }

        // 4. Generate new UUID
        return UUID.randomUUID().toString();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
