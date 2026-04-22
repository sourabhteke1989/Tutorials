package com.yourorg.gateway.filter;

import com.yourorg.gateway.config.GatewayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Redirects HTTP to HTTPS (REQ-HS-001, REQ-HS-002, REQ-HS-003).
 * Only active when gateway.https-redirect-enabled is true.
 */
@Component
public class HttpsRedirectFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(HttpsRedirectFilter.class);
    private final boolean enabled;

    public HttpsRedirectFilter(GatewayProperties properties) {
        this.enabled = properties.httpsRedirectEnabled();
        log.info("HTTPS redirect filter: {}", enabled ? "ENABLED" : "DISABLED");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!enabled) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        URI uri = request.getURI();

        if ("http".equalsIgnoreCase(uri.getScheme())) {
            String httpsUrl = "https" + uri.toString().substring(4);
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
            response.getHeaders().setLocation(URI.create(httpsUrl));
            return response.setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
