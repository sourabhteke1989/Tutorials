package com.yourorg.gateway.filter;

import com.yourorg.gateway.config.GatewayProperties;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HttpsRedirectFilterTest {

    @Test
    void shouldRedirectHttpToHttpsWhenEnabled() {
        GatewayProperties props = new GatewayProperties("r.json", "s.json", false, true, "prod", null, null);
        HttpsRedirectFilter filter = new HttpsRedirectFilter(props);

        MockServerHttpRequest request = MockServerHttpRequest.get("http://example.com/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertEquals(HttpStatus.MOVED_PERMANENTLY, exchange.getResponse().getStatusCode());
        assertTrue(exchange.getResponse().getHeaders().getLocation().toString().startsWith("https://"));
        verifyNoInteractions(chain);
    }

    @Test
    void shouldPassThroughHttpsWhenEnabled() {
        GatewayProperties props = new GatewayProperties("r.json", "s.json", false, true, "prod", null, null);
        HttpsRedirectFilter filter = new HttpsRedirectFilter(props);

        MockServerHttpRequest request = MockServerHttpRequest.get("https://example.com/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void shouldNotRedirectWhenDisabled() {
        GatewayProperties props = new GatewayProperties("r.json", "s.json", false, false, "prod", null, null);
        HttpsRedirectFilter filter = new HttpsRedirectFilter(props);

        MockServerHttpRequest request = MockServerHttpRequest.get("http://example.com/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void shouldHaveCorrectOrder() {
        GatewayProperties props = new GatewayProperties("r.json", "s.json", false, false, "prod", null, null);
        HttpsRedirectFilter filter = new HttpsRedirectFilter(props);
        assertEquals(Integer.MIN_VALUE + 1, filter.getOrder());
    }
}
