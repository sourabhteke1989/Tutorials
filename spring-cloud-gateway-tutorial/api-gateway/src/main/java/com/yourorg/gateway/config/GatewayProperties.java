package com.yourorg.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Gateway application properties (REQ-CF-002).
 */
@ConfigurationProperties(prefix = "gateway")
public record GatewayProperties(
        String routerConfigFilePath,
        String securityConfigFilePath,
        boolean developmentMode,
        boolean httpsRedirectEnabled,
        String defaultEnvironment,
        RateLimitProperties rateLimit,
        ResilienceProperties resilience
) {
    public GatewayProperties {
        if (defaultEnvironment == null || defaultEnvironment.isBlank()) {
            defaultEnvironment = "production";
        }
        if (rateLimit == null) {
            rateLimit = new RateLimitProperties(1000, 100, 500, 60);
        }
        if (resilience == null) {
            resilience = new ResilienceProperties(
                    50, 80, 5000, 10, 20, 30000, 5,
                    2, 500, 2.0, 5000,
                    5000, 30000,
                    50, 500
            );
        }
    }

    public record RateLimitProperties(
            int perTenantLimit,
            int perIpLimit,
            int perUserLimit,
            int windowSeconds
    ) {
        public RateLimitProperties {
            if (perTenantLimit <= 0) perTenantLimit = 1000;
            if (perIpLimit <= 0) perIpLimit = 100;
            if (perUserLimit <= 0) perUserLimit = 500;
            if (windowSeconds <= 0) windowSeconds = 60;
        }
    }

    public record ResilienceProperties(
            int failureRateThreshold,
            int slowCallRateThreshold,
            int slowCallDurationMs,
            int minimumNumberOfCalls,
            int slidingWindowSize,
            int waitDurationInOpenStateMs,
            int permittedCallsInHalfOpen,
            int maxRetryAttempts,
            int retryBackoffMs,
            double retryBackoffMultiplier,
            int maxBackoffMs,
            int connectionTimeoutMs,
            int responseTimeoutMs,
            int maxConcurrentCalls,
            int maxWaitDurationMs
    ) {
    }
}
