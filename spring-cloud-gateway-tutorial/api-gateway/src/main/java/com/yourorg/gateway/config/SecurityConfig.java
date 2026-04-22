package com.yourorg.gateway.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Security configuration loaded from external JSON file (REQ-SC-001, REQ-SC-002).
 */
public record SecurityConfig(
        @JsonProperty("services") Map<String, ServiceSecurityConfig> services
) {

    public record ServiceSecurityConfig(
            @JsonProperty("public_endpoints") List<EndpointConfig> publicEndpoints,
            @JsonProperty("private_endpoints") List<EndpointConfig> privateEndpoints
    ) {
    }

    public record EndpointConfig(
            @JsonProperty("url") String url,
            @JsonProperty("method") String method
    ) {
    }
}
