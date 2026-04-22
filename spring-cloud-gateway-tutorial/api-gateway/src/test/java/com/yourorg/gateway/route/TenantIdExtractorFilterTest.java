package com.yourorg.gateway.route;

import com.yourorg.gateway.config.RouterConfig;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantIdExtractorFilterTest {

    private final RouterConfig config = new RouterConfig(
            List.of("yourorg.cloud", "yourorg.com"),
            "http://website.com",
            "http://landing.com",
            Map.of("user-mgmt", "http://localhost:8081"),
            Map.of("app", "http://localhost:3000")
    );

    private final TenantIdExtractorFilter filter = new TenantIdExtractorFilter(config);

    @Test
    void shouldExtractTenantFromSubdomain() {
        assertEquals("acme", filter.extractTenantId("acme.yourorg.cloud"));
    }

    @Test
    void shouldExtractTenantFromSecondDomain() {
        assertEquals("globex", filter.extractTenantId("globex.yourorg.com"));
    }

    @Test
    void shouldReturnNullForBareDomain() {
        assertNull(filter.extractTenantId("yourorg.cloud"));
    }

    @Test
    void shouldReturnNullForUnknownHost() {
        assertNull(filter.extractTenantId("acme.unknown.com"));
    }

    @Test
    void shouldStripPort() {
        assertEquals("acme", filter.extractTenantId("acme.yourorg.cloud:8080"));
    }

    @Test
    void shouldHandleMultiLevelSubdomain() {
        assertEquals("dev.acme", filter.extractTenantId("dev.acme.yourorg.cloud"));
    }

    @Test
    void filterShouldAddTenantHeaderWhenSubdomainPresent() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .header("Host", "acme.yourorg.cloud")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        assertEquals("acme", exchange.getAttributes().get("tenantId"));
        verify(chain).filter(any());
    }

    @Test
    void filterShouldPassThroughWhenNullHost() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        assertNull(exchange.getAttributes().get("tenantId"));
    }

    @Test
    void filterShouldPassThroughWhenBareDomain() {
        MockServerHttpRequest request = MockServerHttpRequest.get("http://yourorg.cloud/api/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        assertNull(exchange.getAttributes().get("tenantId"));
    }
}
