package com.yourorg.gateway.resilience;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.gateway.config.ConfigLoader;
import com.yourorg.gateway.config.GatewayProperties;
import com.yourorg.gateway.filter.CorrelationIdFilter;
import com.yourorg.gateway.model.GatewayErrorResponse;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resilience configuration: circuit breaker, retry, bulkhead (REQ-RS-001 through REQ-RS-042).
 */
@Configuration
public class ResilienceConfig {

    private static final Logger log = LoggerFactory.getLogger(ResilienceConfig.class);

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(GatewayProperties properties,
                                                          ConfigLoader configLoader,
                                                          MeterRegistry meterRegistry) {
        var resilience = properties.resilience();

        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(resilience.failureRateThreshold())
                .slowCallRateThreshold(resilience.slowCallRateThreshold())
                .slowCallDurationThreshold(Duration.ofMillis(resilience.slowCallDurationMs()))
                .minimumNumberOfCalls(resilience.minimumNumberOfCalls())
                .slidingWindowSize(resilience.slidingWindowSize())
                .waitDurationInOpenState(Duration.ofMillis(resilience.waitDurationInOpenStateMs()))
                .permittedNumberOfCallsInHalfOpenState(resilience.permittedCallsInHalfOpen())
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(defaultConfig);

        // Create circuit breaker per configured service
        for (String serviceKey : configLoader.getRouterConfig().services().keySet()) {
            CircuitBreaker cb = registry.circuitBreaker(serviceKey);
            cb.getEventPublisher()
                    .onStateTransition(event -> {
                        log.warn("Circuit breaker [{}] state transition: {} -> {}",
                                serviceKey, event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState());
                        meterRegistry.counter("gateway.circuit_breaker.state_transition",
                                "service", serviceKey,
                                "from", event.getStateTransition().getFromState().name(),
                                "to", event.getStateTransition().getToState().name()).increment();
                    });
            log.info("Circuit breaker initialized for service: {}", serviceKey);
        }

        return registry;
    }

    @Bean
    public RetryRegistry retryRegistry(GatewayProperties properties) {
        var resilience = properties.resilience();

        RetryConfig defaultConfig = RetryConfig.custom()
                .maxAttempts(resilience.maxRetryAttempts() + 1) // includes initial attempt
                .waitDuration(Duration.ofMillis(resilience.retryBackoffMs()))
                .retryOnResult(response -> false) // handled at HTTP level
                .build();

        return RetryRegistry.of(defaultConfig);
    }

    @Bean
    public BulkheadRegistry bulkheadRegistry(GatewayProperties properties) {
        var resilience = properties.resilience();

        BulkheadConfig defaultConfig = BulkheadConfig.custom()
                .maxConcurrentCalls(resilience.maxConcurrentCalls())
                .maxWaitDuration(Duration.ofMillis(resilience.maxWaitDurationMs()))
                .build();

        return BulkheadRegistry.of(defaultConfig);
    }

    @Bean
    public ResilienceHelper resilienceHelper(CircuitBreakerRegistry circuitBreakerRegistry,
                                              ObjectMapper objectMapper) {
        return new ResilienceHelper(circuitBreakerRegistry, objectMapper);
    }

    public static class ResilienceHelper {
        private final CircuitBreakerRegistry circuitBreakerRegistry;
        private final ObjectMapper objectMapper;
        private final Map<String, CircuitBreaker.State> lastKnownStates = new ConcurrentHashMap<>();

        public ResilienceHelper(CircuitBreakerRegistry circuitBreakerRegistry, ObjectMapper objectMapper) {
            this.circuitBreakerRegistry = circuitBreakerRegistry;
            this.objectMapper = objectMapper;
        }

        public boolean isCircuitOpen(String serviceKey) {
            try {
                CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(serviceKey);
                return cb.getState() == CircuitBreaker.State.OPEN;
            } catch (Exception e) {
                return false;
            }
        }

        public Mono<Void> circuitOpenResponse(ServerWebExchange exchange, String serviceKey) {
            String correlationId = exchange.getAttributeOrDefault(
                    CorrelationIdFilter.CORRELATION_ID_ATTR, "unknown");

            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            response.getHeaders().add("Retry-After", "30");

            GatewayErrorResponse errorBody = GatewayErrorResponse.of(
                    503, "Service Unavailable",
                    "Service " + serviceKey + " is temporarily unavailable. Please retry later.",
                    correlationId);

            try {
                byte[] body = objectMapper.writeValueAsBytes(errorBody);
                return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
            } catch (Exception e) {
                return response.setComplete();
            }
        }

        public Map<String, String> getCircuitBreakerStates() {
            Map<String, String> states = new ConcurrentHashMap<>();
            circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb ->
                    states.put(cb.getName(), cb.getState().name()));
            return states;
        }
    }
}
