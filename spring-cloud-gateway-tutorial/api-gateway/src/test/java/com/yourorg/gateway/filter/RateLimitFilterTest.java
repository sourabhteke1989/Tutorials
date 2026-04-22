package com.yourorg.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.yourorg.gateway.config.GatewayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private RateLimitFilter filter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        GatewayProperties props = new GatewayProperties(
                "r.json", "s.json", true, false, "prod",
                new GatewayProperties.RateLimitProperties(5, 3, 5, 60),
                null);
        objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        filter = new RateLimitFilter(props, objectMapper);
    }

    @Test
    void shouldAllowRequestWithinLimit() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(any());
    }

    @Test
    void shouldRejectWhenIpLimitExceeded() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // Exhaust IP limit (3 requests)
        for (int i = 0; i < 3; i++) {
            MockServerHttpRequest req = MockServerHttpRequest.get("/test").build();
            MockServerWebExchange ex = MockServerWebExchange.from(req);
            StepVerifier.create(filter.filter(ex, chain)).verifyComplete();
        }

        // 4th request should be rejected
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertEquals(429, exchange.getResponse().getStatusCode().value());
    }

    @Test
    void shouldExemptActuatorEndpoints() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/actuator/health").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(any());
    }

    @Test
    void shouldAddRateLimitHeaders() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertNotNull(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit"));
        assertNotNull(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining"));
        assertNotNull(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Reset"));
    }

    @Test
    void shouldHaveCorrectOrder() {
        assertEquals(Integer.MIN_VALUE + 3, filter.getOrder());
    }

    @Test
    void shouldUseXForwardedForForClientIp() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header("X-Forwarded-For", "1.2.3.4, 5.6.7.8").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(any());
    }

    @Test
    void shouldApplyTenantRateLimiting() {
        GatewayProperties props = new GatewayProperties(
                "r.json", "s.json", true, false, "prod",
                new GatewayProperties.RateLimitProperties(2, 1000, 1000, 60),
                null);
        ObjectMapper om = new ObjectMapper();
        om.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        RateLimitFilter tenantFilter = new RateLimitFilter(props, om);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // First 2 requests with tenant should pass
        for (int i = 0; i < 2; i++) {
            MockServerHttpRequest req = MockServerHttpRequest.get("/test")
                    .header("X-Forwarded-For", "10.0.0." + i).build();
            MockServerWebExchange ex = MockServerWebExchange.from(req);
            ex.getAttributes().put("tenantId", "test-tenant");
            StepVerifier.create(tenantFilter.filter(ex, chain)).verifyComplete();
        }

        // 3rd request with same tenant should be rejected
        MockServerHttpRequest req = MockServerHttpRequest.get("/test")
                .header("X-Forwarded-For", "10.0.0.99").build();
        MockServerWebExchange ex = MockServerWebExchange.from(req);
        ex.getAttributes().put("tenantId", "test-tenant");
        StepVerifier.create(tenantFilter.filter(ex, chain)).verifyComplete();
        assertEquals(429, ex.getResponse().getStatusCode().value());
    }

    @Test
    void shouldApplyUserRateLimiting() {
        GatewayProperties props = new GatewayProperties(
                "r.json", "s.json", true, false, "prod",
                new GatewayProperties.RateLimitProperties(1000, 1000, 2, 60),
                null);
        ObjectMapper om = new ObjectMapper();
        om.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        RateLimitFilter userFilter = new RateLimitFilter(props, om);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // First 2 requests with user should pass
        for (int i = 0; i < 2; i++) {
            MockServerHttpRequest req = MockServerHttpRequest.get("/test")
                    .header("X-Forwarded-For", "20.0.0." + i)
                    .header("X-User-ID", "test-user").build();
            MockServerWebExchange ex = MockServerWebExchange.from(req);
            StepVerifier.create(userFilter.filter(ex, chain)).verifyComplete();
        }

        // 3rd request with same user should be rejected
        MockServerHttpRequest req = MockServerHttpRequest.get("/test")
                .header("X-Forwarded-For", "20.0.0.99")
                .header("X-User-ID", "test-user").build();
        MockServerWebExchange ex = MockServerWebExchange.from(req);
        StepVerifier.create(userFilter.filter(ex, chain)).verifyComplete();
        assertEquals(429, ex.getResponse().getStatusCode().value());
    }

    @Test
    void shouldNotApplyTenantLimitWhenTenantIdIsBlank() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header("X-Forwarded-For", "30.0.0.1").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getAttributes().put("tenantId", "  ");

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(any());
    }

    @Test
    void shouldNotApplyUserLimitWhenUserIdIsBlank() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header("X-Forwarded-For", "40.0.0.1")
                .header("X-User-ID", "  ").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(any());
    }
}
