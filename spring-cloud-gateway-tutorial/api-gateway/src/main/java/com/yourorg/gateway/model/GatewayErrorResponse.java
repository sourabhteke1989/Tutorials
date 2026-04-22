package com.yourorg.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record GatewayErrorResponse(
        @JsonProperty("status") int status,
        @JsonProperty("error") String error,
        @JsonProperty("message") String message,
        @JsonProperty("correlation_id") String correlationId,
        @JsonProperty("timestamp") String timestamp
) {
    public static GatewayErrorResponse of(int status, String error, String message, String correlationId) {
        return new GatewayErrorResponse(status, error, message, correlationId, Instant.now().toString());
    }
}
