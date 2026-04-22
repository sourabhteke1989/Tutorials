package com.yourorg.usermgmt.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;

public record UserDto(
        @JsonProperty("user_id") String userId,
        @JsonProperty("tenant_id") String tenantId,
        @JsonProperty("user_name") String userName,
        @JsonProperty("email") String email,
        @JsonProperty("phone_number") String phoneNumber,
        @JsonProperty("status") String status,
        @JsonProperty("authorities") Set<String> authorities
) {
}
