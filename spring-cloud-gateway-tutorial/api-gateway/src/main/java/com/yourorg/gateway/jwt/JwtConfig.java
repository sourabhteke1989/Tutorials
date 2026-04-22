package com.yourorg.gateway.jwt;

import com.yourorg.gateway.config.JwtProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the JWT validator implementation based on gateway.jwt.generator property (REQ-JW-002).
 */
@Configuration
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    @Bean
    public JwtTokenValidator jwtTokenValidator(JwtProperties jwtProperties) {
        if (jwtProperties.secretKey() == null || jwtProperties.secretKey().isBlank()) {
            throw new IllegalStateException("JWT secret key must be configured (gateway.jwt.secret-key / JWT_SECRET_KEY)");
        }
        if (jwtProperties.secretKey().getBytes().length < 32) {
            throw new IllegalStateException("JWT secret key must be at least 256 bits (32 bytes) for HMAC-SHA256");
        }

        String generator = jwtProperties.generator();
        log.info("Initializing JWT validator with implementation: {}", generator);

        return switch (generator.toLowerCase()) {
            case "nimbus" -> new NimbusJwtTokenValidator(jwtProperties);
            default -> new JjwtTokenValidator(jwtProperties);
        };
    }
}
