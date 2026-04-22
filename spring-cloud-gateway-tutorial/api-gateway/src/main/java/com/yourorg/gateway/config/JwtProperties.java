package com.yourorg.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT configuration properties (REQ-JW-001, REQ-CF-002).
 */
@ConfigurationProperties(prefix = "gateway.jwt")
public record JwtProperties(
        String generator,
        String secretKey,
        long expiration,
        String issuer,
        String audience
) {
    public JwtProperties {
        if (generator == null || generator.isBlank()) {
            generator = "jjwt";
        }
        if (expiration <= 0) {
            expiration = 900000L;
        }
        if (issuer == null || issuer.isBlank()) {
            issuer = "yourorg.com";
        }
        if (audience == null || audience.isBlank()) {
            audience = "yourorg.com";
        }
    }
}
