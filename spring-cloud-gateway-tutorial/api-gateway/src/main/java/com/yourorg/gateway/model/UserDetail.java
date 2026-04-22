package com.yourorg.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;

public record UserDetail(
        @JsonProperty("user_id") String userId,
        @JsonProperty("tenant_id") String tenantId,
        @JsonProperty("user_name") String userName,
        @JsonProperty("email") String email,
        @JsonProperty("phone_number") String phoneNumber,
        @JsonProperty("status") String status,
        @JsonProperty("authorities") Set<String> authorities
) {

    public boolean belongsToTenant(String targetTenantId) {
        return tenantId != null && tenantId.equals(targetTenantId);
    }
}
