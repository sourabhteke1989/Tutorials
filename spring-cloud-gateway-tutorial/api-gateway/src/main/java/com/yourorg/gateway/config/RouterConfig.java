package com.yourorg.gateway.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Router configuration loaded from external JSON file (REQ-RT-001, REQ-RT-003).
 */
public record RouterConfig(
        @JsonProperty("allowed_hosts") List<String> allowedHosts,
        @JsonProperty("website_url") String websiteUrl,
        @JsonProperty("landing_app_url") String landingAppUrl,
        @JsonProperty("services") Map<String, String> services,
        @JsonProperty("ui_applications") Map<String, String> uiApplications
) {
}
