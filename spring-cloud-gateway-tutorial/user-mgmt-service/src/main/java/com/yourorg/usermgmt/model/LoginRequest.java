package com.yourorg.usermgmt.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginRequest(
        @JsonProperty("username") String username,
        @JsonProperty("password") String password,
        @JsonProperty("tenant_id") String tenantId
) {
}
