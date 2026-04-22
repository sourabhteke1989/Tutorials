package com.yourorg.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a validated JWT token's extracted claims.
 */
public record TokenClaims(
        @JsonProperty("user_id") String userId,
        @JsonProperty("user_name") String userName,
        @JsonProperty("phone_number") String phoneNumber,
        @JsonProperty("tenant_id") String tenantId
) {
    public TokenClaims {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId (sub) claim is required");
        }
        userName = userName != null ? userName : "";
        phoneNumber = phoneNumber != null ? phoneNumber : "";
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId claim is required");
        }
    }
}
