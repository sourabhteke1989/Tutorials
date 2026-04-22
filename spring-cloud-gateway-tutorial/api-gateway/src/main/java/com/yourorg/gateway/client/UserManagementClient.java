package com.yourorg.gateway.client;

import com.yourorg.gateway.config.ConfigLoader;
import com.yourorg.gateway.model.TenantBasicDetail;
import com.yourorg.gateway.model.UserDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Reactive HTTP client for User Management service (REQ-CL-001, REQ-CL-002, REQ-CL-003).
 */
@Component
public class UserManagementClient {

    private static final Logger log = LoggerFactory.getLogger(UserManagementClient.class);

    private final WebClient webClient;

    public UserManagementClient(ConfigLoader configLoader) {
        String userMgmtUrl = configLoader.getRouterConfig().services().get("user-mgmt");
        if (userMgmtUrl == null) {
            throw new IllegalStateException("user-mgmt service URL not found in router configuration");
        }
        this.webClient = WebClient.builder()
                .baseUrl(userMgmtUrl)
                .build();
        log.info("UserManagementClient initialized with URL: {}", userMgmtUrl);
    }

    public Mono<TenantBasicDetail> getTenantDetails(String tenantId) {
        return webClient.get()
                .uri("/organization/details?tenant_id={tenantId}", tenantId)
                .header("X-Internal-Request", "true")
                .retrieve()
                .bodyToMono(TenantBasicDetail.class)
                .doOnError(e -> log.error("Failed to fetch tenant details for {}: {}", tenantId, e.getMessage()));
    }

    public Mono<UserDetail> getUserByUsername(String username, String tenantId) {
        return webClient.get()
                .uri("/user/by-username?username={username}&tenant_id={tenantId}", username, tenantId)
                .header("X-Internal-Request", "true")
                .retrieve()
                .bodyToMono(UserDetail.class)
                .doOnError(e -> log.error("Failed to fetch user by username {}: {}", username, e.getMessage()));
    }

    public Mono<UserDetail> getUserByToken(String accessToken) {
        return webClient.get()
                .uri("/user/me")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Internal-Request", "true")
                .retrieve()
                .bodyToMono(UserDetail.class)
                .doOnError(e -> log.error("Failed to fetch user by token: {}", e.getMessage()));
    }

    public Mono<List<String>> getUserAccessResourceIds(String userId, String appId,
                                                        String resourceType, String permission) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/user-access-control/resource-ids")
                        .queryParam("user_id", userId)
                        .queryParam("app_id", appId)
                        .queryParam("resource_type", resourceType)
                        .queryParam("permission", permission)
                        .build())
                .header("X-Internal-Request", "true")
                .retrieve()
                .bodyToFlux(String.class)
                .collectList()
                .doOnError(e -> log.error("Failed to fetch access resource IDs: {}", e.getMessage()));
    }

    public Mono<Boolean> hasUserAccessToResource(String userId, String resourceId, String permission) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/user-access-control/has-access")
                        .queryParam("user_id", userId)
                        .queryParam("resource_id", resourceId)
                        .queryParam("permission", permission)
                        .build())
                .header("X-Internal-Request", "true")
                .retrieve()
                .bodyToMono(Boolean.class)
                .doOnError(e -> log.error("Failed to check user access: {}", e.getMessage()));
    }
}
