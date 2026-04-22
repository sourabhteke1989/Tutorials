package com.yourorg.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HeaderStrippingFilterTest {

    private final HeaderStrippingFilter filter = new HeaderStrippingFilter();

    @Test
    void shouldStripTrustedHeaders() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header("X-User-ID", "spoofed")
                .header("X-Tenant-ID", "spoofed")
                .header("X-User-Name", "spoofed")
                .header("X-User-Phone", "spoofed")
                .header("X-Internal-Request", "true")
                .header("X-Service-ID", "spoofed")
                .header("X-Application-ID", "spoofed")
                .header("X-Environment", "spoofed")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenAnswer(inv -> {
            ServerWebExchange ex = inv.getArgument(0);
            assertNull(ex.getRequest().getHeaders().getFirst("X-User-ID"));
            assertNull(ex.getRequest().getHeaders().getFirst("X-Tenant-ID"));
            assertNull(ex.getRequest().getHeaders().getFirst("X-Internal-Request"));
            return Mono.empty();
        });

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(any());
    }

    @Test
    void shouldPassThroughNonTrustedHeaders() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header("Authorization", "Bearer token")
                .header("Content-Type", "application/json")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenAnswer(inv -> {
            ServerWebExchange ex = inv.getArgument(0);
            assertNotNull(ex.getRequest().getHeaders().getFirst("Authorization"));
            return Mono.empty();
        });

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
    }

    @Test
    void shouldHaveCorrectOrder() {
        assertEquals(Integer.MIN_VALUE + 2, filter.getOrder());
    }
}
