package com.yourorg.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.gateway.config.EndpointMatcher;
import com.yourorg.gateway.exception.InvalidAccessTokenException;
import com.yourorg.gateway.jwt.JwtTokenValidator;
import com.yourorg.gateway.model.GatewayErrorResponse;
import com.yourorg.gateway.model.TokenClaims;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

/**
 * Authentication filter for backend service routes (REQ-AU-001 through REQ-AU-011).
 * Validates JWT tokens, checks endpoint whitelists, and enriches downstream requests.
 */
@Component
public class AuthenticationFilterFactory extends AbstractGatewayFilterFactory<AuthenticationFilterFactory.Config> {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilterFactory.class);

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Set<String> ACCEPTED_AUTH_MODES = Set.of("internal", "external");

    private final JwtTokenValidator jwtTokenValidator;
    private final EndpointMatcher endpointMatcher;
    private final ObjectMapper objectMapper;

    // Metrics (REQ-OB-011)
    private final Counter authSuccessCounter;
    private final Counter authFailureExpiredCounter;
    private final Counter authFailureInvalidCounter;
    private final Counter authFailureMissingCounter;
    private final Counter authFailureOrgMismatchCounter;
    private final Counter publicBypassCounter;
    private final Counter unknownServiceCounter;

    public AuthenticationFilterFactory(JwtTokenValidator jwtTokenValidator,
                                       EndpointMatcher endpointMatcher,
                                       ObjectMapper objectMapper,
                                       MeterRegistry meterRegistry) {
        super(Config.class);
        this.jwtTokenValidator = jwtTokenValidator;
        this.endpointMatcher = endpointMatcher;
        this.objectMapper = objectMapper;

        this.authSuccessCounter = Counter.builder("gateway.auth.success").register(meterRegistry);
        this.authFailureExpiredCounter = Counter.builder("gateway.auth.failure")
                .tag("reason", "expired_token").register(meterRegistry);
        this.authFailureInvalidCounter = Counter.builder("gateway.auth.failure")
                .tag("reason", "invalid_signature").register(meterRegistry);
        this.authFailureMissingCounter = Counter.builder("gateway.auth.failure")
                .tag("reason", "missing_token").register(meterRegistry);
        this.authFailureOrgMismatchCounter = Counter.builder("gateway.auth.failure")
                .tag("reason", "org_mismatch").register(meterRegistry);
        this.publicBypassCounter = Counter.builder("gateway.auth.public_bypass").register(meterRegistry);
        this.unknownServiceCounter = Counter.builder("gateway.auth.unknown_service").register(meterRegistry);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = extractServicePath(request.getPath().value());
            String method = request.getMethod().name();
            String serviceId = request.getHeaders().getFirst("X-Service-ID");
            String correlationId = exchange.getAttributeOrDefault(
                    CorrelationIdFilter.CORRELATION_ID_ATTR, "unknown");

            log.debug("Auth filter evaluating: {} {} for service {}", method, path, serviceId);

            // REQ-AU-002: Check if service is recognized
            if (serviceId == null || !endpointMatcher.isServiceKnown(serviceId)) {
                unknownServiceCounter.increment();
                log.error("Service ID not found in security configuration: {}", serviceId);
                return errorResponse(exchange, HttpStatus.FORBIDDEN, "Forbidden",
                        "Service not recognized: " + serviceId, correlationId);
            }

            // Allow CORS preflight through (REQ-SC-003 exception)
            if (HttpMethod.OPTIONS.matches(method)) {
                return chain.filter(exchange);
            }

            // REQ-AU-003: Public endpoint bypass
            if (endpointMatcher.isPublicEndpoint(serviceId, path, method)) {
                publicBypassCounter.increment();
                log.debug("Public endpoint match: {} {} for service {}", method, path, serviceId);
                return chain.filter(exchange);
            }

            // REQ-AU-005: Check X-Auth-Mode header
            String authMode = request.getHeaders().getFirst("X-Auth-Mode");
            if (authMode == null || !ACCEPTED_AUTH_MODES.contains(authMode.toLowerCase())) {
                log.warn("Missing or invalid X-Auth-Mode header: {}", authMode);
                return errorResponse(exchange, HttpStatus.BAD_REQUEST, "Bad Request",
                        "X-Auth-Mode header is required. Accepted values: " + ACCEPTED_AUTH_MODES,
                        correlationId);
            }

            // REQ-AU-007: External auth not yet implemented
            if ("external".equalsIgnoreCase(authMode)) {
                return errorResponse(exchange, HttpStatus.NOT_IMPLEMENTED, "Not Implemented",
                        "External authentication is not yet available", correlationId);
            }

            // REQ-AU-004: Extract Bearer token
            String authHeader = request.getHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                authFailureMissingCounter.increment();
                log.warn("Missing or invalid Authorization header");
                return errorResponse(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized",
                        "Authentication required", correlationId);
            }

            String token = authHeader.substring(BEARER_PREFIX.length());

            // REQ-AU-006: Validate JWT token
            TokenClaims claims;
            try {
                claims = jwtTokenValidator.validateToken(token);
            } catch (InvalidAccessTokenException e) {
                if (e.getMessage().contains("expired")) {
                    authFailureExpiredCounter.increment();
                } else {
                    authFailureInvalidCounter.increment();
                }
                log.warn("Token validation failed: {}", e.getMessage());
                // REQ-EH-004: Security-neutral error message
                return errorResponse(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized",
                        "Authentication required", correlationId);
            }

            // REQ-AU-008: Cross-validate tenant ID
            String subdomainTenantId = request.getHeaders().getFirst("X-Tenant-ID");
            if (subdomainTenantId != null && !subdomainTenantId.isBlank()
                    && !subdomainTenantId.equals(claims.tenantId())) {
                authFailureOrgMismatchCounter.increment();
                log.error("Tenant ID mismatch: token={} subdomain={}", claims.tenantId(), subdomainTenantId);
                return errorResponse(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized",
                        "Authentication required", correlationId);
            }

            // REQ-AU-009: Private endpoint whitelist check
            if (!endpointMatcher.isPrivateEndpoint(serviceId, path, method)) {
                log.warn("Endpoint not in private whitelist: {} {} for service {}", method, path, serviceId);
                return errorResponse(exchange, HttpStatus.FORBIDDEN, "Forbidden",
                        "Access denied to this endpoint", correlationId);
            }

            // REQ-AU-010: Enrich downstream request with verified identity headers
            authSuccessCounter.increment();
            log.debug("Authentication successful for user {} in tenant {}", claims.userId(), claims.tenantId());

            ServerHttpRequest enrichedRequest = request.mutate()
                    .header("X-User-ID", claims.userId())
                    .header("X-User-Name", claims.userName())
                    .header("X-User-Phone", claims.phoneNumber())
                    .header("X-Tenant-ID", claims.tenantId())
                    .header("X-Internal-Request", "false")
                    .build();

            return chain.filter(exchange.mutate().request(enrichedRequest).build());
        };
    }

    /**
     * Extracts the downstream service path by stripping the /{serviceKey}/api prefix.
     */
    private String extractServicePath(String fullPath) {
        // Path format: /{serviceKey}/api/{remainingPath}
        // The route already strips the prefix, but if called during filter,
        // the path may still contain the original prefix
        String[] segments = fullPath.split("/", 4);
        if (segments.length >= 4 && "api".equals(segments[2])) {
            return "/" + segments[3];
        }
        // If path was already rewritten or doesn't match the pattern
        return fullPath;
    }

    private Mono<Void> errorResponse(ServerWebExchange exchange, HttpStatus status,
                                      String error, String message, String correlationId) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().add(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId);

        GatewayErrorResponse errorBody = GatewayErrorResponse.of(
                status.value(), error, message, correlationId);

        try {
            byte[] body = objectMapper.writeValueAsBytes(errorBody);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
        } catch (Exception e) {
            log.error("Failed to serialize error response", e);
            return response.setComplete();
        }
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of();
    }

    public static class Config {
        // No configuration needed — uses injected beans
    }
}
