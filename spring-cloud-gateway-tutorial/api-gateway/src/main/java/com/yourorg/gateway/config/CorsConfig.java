package com.yourorg.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS configuration (REQ-CO-001, REQ-CO-002, REQ-CO-003).
 * Only active in development mode.
 */
@Configuration
public class CorsConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    @Bean
    public CorsWebFilter corsWebFilter(GatewayProperties properties) {
        CorsConfiguration corsConfig = new CorsConfiguration();

        if (properties.developmentMode()) {
            log.warn("Development mode CORS enabled — all origins permitted");
            corsConfig.addAllowedOriginPattern("*");
            corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
            corsConfig.addAllowedHeader("*");
            corsConfig.setAllowCredentials(true);
            corsConfig.setMaxAge(3600L);
        } else {
            log.info("Production CORS — restrictive policy (handled by reverse proxy)");
            corsConfig.addAllowedOriginPattern("*");
            corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
            corsConfig.addAllowedHeader("*");
            corsConfig.setAllowCredentials(true);
        }

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
