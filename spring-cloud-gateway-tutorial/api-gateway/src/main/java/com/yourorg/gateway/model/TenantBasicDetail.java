package com.yourorg.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TenantBasicDetail(
        @JsonProperty("tenant_id") String tenantId,
        @JsonProperty("name") String name,
        @JsonProperty("region") String region,
        @JsonProperty("country") String country,
        @JsonProperty("timezone") String timezone,
        @JsonProperty("auth_mode") String authMode,
        @JsonProperty("status") String status,
        @JsonProperty("auth_url") String authUrl
) {
}
