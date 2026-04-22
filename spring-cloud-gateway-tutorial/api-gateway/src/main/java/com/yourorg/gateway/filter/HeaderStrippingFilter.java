package com.yourorg.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Strips trusted identity headers from incoming requests to prevent spoofing (REQ-NF-004, REQ-AU-010).
 */
@Component
public class HeaderStrippingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(HeaderStrippingFilter.class);

    private static final List<String> TRUSTED_HEADERS = List.of(
            "X-Tenant-ID",
            "X-User-ID",
            "X-User-Name",
            "X-User-Phone",
            "X-Internal-Request",
            "X-Service-ID",
            "X-Application-ID",
            "X-Environment"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();

        for (String header : TRUSTED_HEADERS) {
            if (exchange.getRequest().getHeaders().containsKey(header)) {
                requestBuilder.headers(h -> h.remove(header));
                log.debug("Stripped incoming trusted header: {}", header);
            }
        }

        return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
