package com.yourorg.gateway.route;

import com.yourorg.gateway.config.ConfigLoader;
import com.yourorg.gateway.config.GatewayProperties;
import com.yourorg.gateway.config.RouterConfig;
import com.yourorg.gateway.filter.AuthenticationFilterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds all gateway routes from external configuration (REQ-RT-001 through REQ-RT-031).
 */
@Configuration
public class RouteConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RouteConfiguration.class);

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder,
                                            ConfigLoader configLoader,
                                            GatewayProperties gatewayProperties,
                                            AuthenticationFilterFactory authFilterFactory) {
        RouterConfig config = configLoader.getRouterConfig();
        RouteLocatorBuilder.Builder routes = builder.routes();

        String[] tenantHostPattern = buildTenantHostPattern(config);
        String[] bareHostPattern = buildBareHostPattern(config);

        log.info("Building routes with tenant pattern: {}, bare pattern: {}",
                String.join(",", tenantHostPattern), String.join(",", bareHostPattern));

        // === Backend Service Routes on tenant subdomains (REQ-RT-010 through REQ-RT-013) ===
        for (var entry : config.services().entrySet()) {
            String serviceKey = entry.getKey();
            String serviceUrl = entry.getValue();

            routes = routes.route("tenant-api-" + serviceKey, r -> r
                    .host(tenantHostPattern)
                    .and()
                    .path("/" + serviceKey + "/api/**")
                    .filters(f -> f
                            .filter(new TenantIdExtractorFilter(config))
                            .addRequestHeader("X-Service-ID", serviceKey)
                            .rewritePath("/" + serviceKey + "/api/(?<segment>.*)", "/${segment}")
                            .filter(authFilterFactory.apply(new AuthenticationFilterFactory.Config()))
                    )
                    .uri(serviceUrl)
            );

            log.info("Route: tenant-api-{} -> {}", serviceKey, serviceUrl);
        }

        // === Backend Service Routes on bare domain (REQ-RT-014) ===
        for (var entry : config.services().entrySet()) {
            String serviceKey = entry.getKey();
            String serviceUrl = entry.getValue();

            routes = routes.route("website-api-" + serviceKey, r -> r
                    .host(bareHostPattern)
                    .and()
                    .path("/" + serviceKey + "/api/**")
                    .filters(f -> f
                            .addRequestHeader("X-Service-ID", serviceKey)
                            .rewritePath("/" + serviceKey + "/api/(?<segment>.*)", "/${segment}")
                            .filter(authFilterFactory.apply(new AuthenticationFilterFactory.Config()))
                    )
                    .uri(serviceUrl)
            );

            log.info("Route: website-api-{} -> {}", serviceKey, serviceUrl);
        }

        // === UI Application Routes on tenant subdomains (REQ-RT-020 through REQ-RT-022) ===
        for (var entry : config.uiApplications().entrySet()) {
            String appKey = entry.getKey();
            String appUrl = entry.getValue();

            routes = routes.route("tenant-ui-" + appKey, r -> r
                    .host(tenantHostPattern)
                    .and()
                    .path("/" + appKey + "/**")
                    .filters(f -> f
                            .filter(new TenantIdExtractorFilter(config))
                            .addRequestHeader("X-Application-ID", appKey)
                            .addRequestHeader("X-Environment", gatewayProperties.defaultEnvironment())
                    )
                    .uri(appUrl)
            );

            log.info("Route: tenant-ui-{} -> {}", appKey, appUrl);
        }

        // === Landing Application Fallback (REQ-RT-023) ===
        if (config.landingAppUrl() != null) {
            routes = routes.route("tenant-landing", r -> r
                    .host(tenantHostPattern)
                    .and()
                    .path("/**")
                    .filters(f -> f
                            .filter(new TenantIdExtractorFilter(config))
                            .addRequestHeader("X-Environment", gatewayProperties.defaultEnvironment())
                    )
                    .uri(config.landingAppUrl())
            );

            log.info("Route: tenant-landing -> {}", config.landingAppUrl());
        }

        // === Company Website catch-all (REQ-RT-030, REQ-RT-031) ===
        if (config.websiteUrl() != null) {
            routes = routes.route("website-catchall", r -> r
                    .host(bareHostPattern)
                    .and()
                    .path("/**")
                    .filters(f -> f
                            .rewritePath("/(?<segment>.*)", "/${segment}")
                    )
                    .uri(config.websiteUrl())
            );

            log.info("Route: website-catchall -> {}", config.websiteUrl());
        }

        return routes.build();
    }

    /**
     * Builds host pattern matching tenant subdomains.
     * E.g., "**.yourorg.cloud,**.yourorg.com"
     */
    private String[] buildTenantHostPattern(RouterConfig config) {
        return config.allowedHosts().stream()
                .map(host -> "**." + host)
                .toArray(String[]::new);
    }

    /**
     * Builds host pattern matching bare domains (no subdomain).
     */
    private String[] buildBareHostPattern(RouterConfig config) {
        return config.allowedHosts().toArray(String[]::new);
    }
}
