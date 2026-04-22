package com.yourorg.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void shouldGenerateCorrelationIdWhenNonePresent() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        String corrId = exchange.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTR);
        assertNotNull(corrId);
        assertFalse(corrId.isBlank());
    }

    @Test
    void shouldUseTraceparentHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header("traceparent", "00-trace-id-123").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertEquals("00-trace-id-123", exchange.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTR));
    }

    @Test
    void shouldUseCorrelationIdHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header("X-Correlation-ID", "corr-abc").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertEquals("corr-abc", exchange.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTR));
    }

    @Test
    void shouldUseRequestIdHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header("X-Request-ID", "req-xyz").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertEquals("req-xyz", exchange.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTR));
    }

    @Test
    void shouldPreferTraceparentOverOtherHeaders() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header("traceparent", "trace-1")
                .header("X-Correlation-ID", "corr-2")
                .header("X-Request-ID", "req-3").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertEquals("trace-1", exchange.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTR));
    }

    @Test
    void shouldAddCorrelationIdToResponse() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header("X-Correlation-ID", "my-corr").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertTrue(exchange.getResponse().getHeaders().containsKey("X-Correlation-ID"));
    }

    @Test
    void shouldHaveHighestPrecedence() {
        assertEquals(Integer.MIN_VALUE, filter.getOrder());
    }
}
