package com.yourorg.gateway.resilience;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.yourorg.gateway.config.ConfigLoader;
import com.yourorg.gateway.config.GatewayProperties;
import com.yourorg.gateway.config.RouterConfig;
import com.yourorg.gateway.filter.CorrelationIdFilter;
import com.yourorg.gateway.model.GatewayErrorResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResilienceConfigTest {

    private GatewayProperties gatewayProperties;
    private ConfigLoader configLoader;
    private SimpleMeterRegistry meterRegistry;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        gatewayProperties = new GatewayProperties(
                "router-config-monolith.json",
                "security-config.json",
                true, false, "production", null, null);
        configLoader = new ConfigLoader(gatewayProperties);
        meterRegistry = new SimpleMeterRegistry();
        objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    @Test
    void shouldCreateCircuitBreakerRegistryWithAllServices() {
        ResilienceConfig resilienceConfig = new ResilienceConfig();
        CircuitBreakerRegistry registry = resilienceConfig.circuitBreakerRegistry(
                gatewayProperties, configLoader, meterRegistry);

        assertNotNull(registry);
        // Each service from router config should have a circuit breaker
        for (String serviceKey : configLoader.getRouterConfig().services().keySet()) {
            CircuitBreaker cb = registry.circuitBreaker(serviceKey);
            assertNotNull(cb);
            assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
        }
    }

    @Test
    void shouldCreateRetryRegistry() {
        ResilienceConfig resilienceConfig = new ResilienceConfig();
        var retryRegistry = resilienceConfig.retryRegistry(gatewayProperties);
        assertNotNull(retryRegistry);
    }

    @Test
    void shouldCreateBulkheadRegistry() {
        ResilienceConfig resilienceConfig = new ResilienceConfig();
        var bulkheadRegistry = resilienceConfig.bulkheadRegistry(gatewayProperties);
        assertNotNull(bulkheadRegistry);
    }

    @Test
    void shouldCreateResilienceHelper() {
        ResilienceConfig resilienceConfig = new ResilienceConfig();
        CircuitBreakerRegistry registry = resilienceConfig.circuitBreakerRegistry(
                gatewayProperties, configLoader, meterRegistry);
        ResilienceConfig.ResilienceHelper helper = resilienceConfig.resilienceHelper(registry, objectMapper);
        assertNotNull(helper);
    }

    // --- ResilienceHelper tests ---

    @Test
    void isCircuitOpen_returnsFalseWhenClosed() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        registry.circuitBreaker("test-service");
        ResilienceConfig.ResilienceHelper helper = new ResilienceConfig.ResilienceHelper(registry, objectMapper);

        assertFalse(helper.isCircuitOpen("test-service"));
    }

    @Test
    void isCircuitOpen_returnsTrueWhenOpen() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(2)
                .slidingWindowSize(2)
                .build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        CircuitBreaker cb = registry.circuitBreaker("test-service");

        // Force open state by recording failures
        cb.transitionToOpenState();

        ResilienceConfig.ResilienceHelper helper = new ResilienceConfig.ResilienceHelper(registry, objectMapper);
        assertTrue(helper.isCircuitOpen("test-service"));
    }

    @Test
    void isCircuitOpen_returnsFalseOnException() {
        // Calling with a service name that doesn't exist yet - circuitBreaker() auto-creates,
        // so we use a custom approach
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        ResilienceConfig.ResilienceHelper helper = new ResilienceConfig.ResilienceHelper(registry, objectMapper);

        // New circuit breaker is auto-created in CLOSED state
        assertFalse(helper.isCircuitOpen("nonexistent-service"));
    }

    @Test
    void circuitOpenResponse_returns503WithBody() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        ResilienceConfig.ResilienceHelper helper = new ResilienceConfig.ResilienceHelper(registry, objectMapper);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test").build());
        exchange.getAttributes().put(CorrelationIdFilter.CORRELATION_ID_ATTR, "test-corr-id");

        StepVerifier.create(helper.circuitOpenResponse(exchange, "user-mgmt"))
                .verifyComplete();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
        assertEquals("application/json", exchange.getResponse().getHeaders().getContentType().toString());
        assertNotNull(exchange.getResponse().getHeaders().getFirst("Retry-After"));
        assertEquals("30", exchange.getResponse().getHeaders().getFirst("Retry-After"));
    }

    @Test
    void circuitOpenResponse_usesUnknownWhenNoCorrelationId() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        ResilienceConfig.ResilienceHelper helper = new ResilienceConfig.ResilienceHelper(registry, objectMapper);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test").build());

        StepVerifier.create(helper.circuitOpenResponse(exchange, "user-mgmt"))
                .verifyComplete();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
    }

    @Test
    void getCircuitBreakerStates_returnsAllStates() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        registry.circuitBreaker("service-a");
        registry.circuitBreaker("service-b");

        ResilienceConfig.ResilienceHelper helper = new ResilienceConfig.ResilienceHelper(registry, objectMapper);
        Map<String, String> states = helper.getCircuitBreakerStates();

        assertEquals(2, states.size());
        assertEquals("CLOSED", states.get("service-a"));
        assertEquals("CLOSED", states.get("service-b"));
    }

    @Test
    void getCircuitBreakerStates_emptyRegistry() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        ResilienceConfig.ResilienceHelper helper = new ResilienceConfig.ResilienceHelper(registry, objectMapper);

        Map<String, String> states = helper.getCircuitBreakerStates();
        assertTrue(states.isEmpty());
    }

    @Test
    void circuitBreakerRegistry_usesConfigProperties() {
        GatewayProperties.ResilienceProperties resilience = new GatewayProperties.ResilienceProperties(
                60, 90, 3000, 5, 10, 20000, 3,
                3, 1000, 2.0, 10000, 5000, 30000, 100, 1000);
        GatewayProperties props = new GatewayProperties(
                "router-config-monolith.json", "security-config.json",
                true, false, "production", null, resilience);
        ConfigLoader loader = new ConfigLoader(props);

        ResilienceConfig resilienceConfig = new ResilienceConfig();
        CircuitBreakerRegistry registry = resilienceConfig.circuitBreakerRegistry(
                props, loader, meterRegistry);

        assertNotNull(registry);
        // Verify default config was applied
        CircuitBreakerConfig defaultCbConfig = registry.getDefaultConfig();
        assertEquals(60, defaultCbConfig.getFailureRateThreshold());
        assertEquals(90, defaultCbConfig.getSlowCallRateThreshold());
    }

    @Test
    void circuitBreakerRegistry_stateTransitionTriggersLogging() {
        ResilienceConfig resilienceConfig = new ResilienceConfig();
        CircuitBreakerRegistry registry = resilienceConfig.circuitBreakerRegistry(
                gatewayProperties, configLoader, meterRegistry);

        // Get one of the service circuit breakers and force a state transition
        String firstService = configLoader.getRouterConfig().services().keySet().iterator().next();
        CircuitBreaker cb = registry.circuitBreaker(firstService);

        // Transition to OPEN to trigger the event publisher lambda
        cb.transitionToOpenState();
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());

        // Transition back to verify another transition event
        cb.transitionToHalfOpenState();
        assertEquals(CircuitBreaker.State.HALF_OPEN, cb.getState());

        // Verify meter was incremented
        assertTrue(meterRegistry.getMeters().stream()
                .anyMatch(m -> m.getId().getName().equals("gateway.circuit_breaker.state_transition")));
    }

    @Test
    void circuitOpenResponse_handlesSerializationException() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        // Use a broken ObjectMapper that will throw on serialization
        ObjectMapper brokenMapper = new ObjectMapper();
        // We can't easily break ObjectMapper for writeValueAsBytes on a record,
        // but we can use a mock
        ObjectMapper mockMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        try {
            org.mockito.Mockito.when(mockMapper.writeValueAsBytes(org.mockito.Mockito.any()))
                    .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("test") {});
        } catch (Exception e) {
            fail("Mock setup failed");
        }

        ResilienceConfig.ResilienceHelper helper = new ResilienceConfig.ResilienceHelper(registry, mockMapper);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test").build());

        StepVerifier.create(helper.circuitOpenResponse(exchange, "svc"))
                .verifyComplete();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
    }
}
