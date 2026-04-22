package com.yourorg.gateway.route;

import com.yourorg.gateway.config.RouterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Extracts tenant ID from request hostname subdomain and injects X-Tenant-ID header
 * (REQ-MT-001, REQ-MT-004).
 */
public class TenantIdExtractorFilter implements GatewayFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantIdExtractorFilter.class);

    private final RouterConfig routerConfig;

    public TenantIdExtractorFilter(RouterConfig routerConfig) {
        this.routerConfig = routerConfig;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String host = request.getHeaders().getHost() != null
                ? request.getHeaders().getHost().getHostString()
                : null;

        if (host == null) {
            return chain.filter(exchange);
        }

        String tenantId = extractTenantId(host);

        if (tenantId != null && !tenantId.isBlank()) {
            log.debug("Extracted tenant ID: {} from host: {}", tenantId, host);
            exchange.getAttributes().put("tenantId", tenantId);

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Tenant-ID", tenantId)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        return chain.filter(exchange);
    }

    /**
     * Extracts the tenant ID from the hostname.
     * Pattern: {tenantId}.{allowedHost}
     */
    String extractTenantId(String host) {
        // Remove port if present
        String hostname = host.contains(":") ? host.substring(0, host.indexOf(':')) : host;

        for (String allowedHost : routerConfig.allowedHosts()) {
            if (hostname.endsWith("." + allowedHost)) {
                String prefix = hostname.substring(0, hostname.length() - allowedHost.length() - 1);
                if (!prefix.isBlank()) {
                    return prefix;
                }
            }
        }
        return null;
    }
}
