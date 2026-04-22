package com.yourorg.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.yourorg.gateway.config.EndpointMatcher;
import com.yourorg.gateway.exception.InvalidAccessTokenException;
import com.yourorg.gateway.jwt.JwtTokenValidator;
import com.yourorg.gateway.model.TokenClaims;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthenticationFilterFactoryTest {

    private JwtTokenValidator jwtValidator;
    private EndpointMatcher endpointMatcher;
    private AuthenticationFilterFactory factory;
    private GatewayFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        jwtValidator = mock(JwtTokenValidator.class);
        endpointMatcher = mock(EndpointMatcher.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        factory = new AuthenticationFilterFactory(jwtValidator, endpointMatcher, mapper, new SimpleMeterRegistry());
        filter = factory.apply(new AuthenticationFilterFactory.Config());
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    private MockServerWebExchange createExchange(String path, String method) {
        MockServerHttpRequest.BaseBuilder<?> builder;
        switch (method) {
            case "POST" -> builder = MockServerHttpRequest.post(path);
            case "PUT" -> builder = MockServerHttpRequest.put(path);
            case "DELETE" -> builder = MockServerHttpRequest.delete(path);
            case "OPTIONS" -> builder = MockServerHttpRequest.options(path);
            default -> builder = MockServerHttpRequest.get(path);
        }
        return MockServerWebExchange.from(builder
                .header("X-Service-ID", "user-mgmt")
                .build());
    }

    @Test
    void shouldRejectUnknownService() {
        when(endpointMatcher.isServiceKnown("unknown")).thenReturn(false);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test")
                        .header("X-Service-ID", "unknown").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldRejectMissingServiceId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldPassOptionsThrough() {
        when(endpointMatcher.isServiceKnown("user-mgmt")).thenReturn(true);
        MockServerWebExchange exchange = createExchange("/test", "OPTIONS");

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(any());
    }

    @Test
    void shouldBypassPublicEndpoint() {
        when(endpointMatcher.isServiceKnown("user-mgmt")).thenReturn(true);
        when(endpointMatcher.isPublicEndpoint(eq("user-mgmt"), anyString(), eq("POST"))).thenReturn(true);
        MockServerWebExchange exchange = createExchange("/auth/login", "POST");

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(any());
    }

    @Test
    void shouldRejectMissingAuthMode() {
        when(endpointMatcher.isServiceKnown("user-mgmt")).thenReturn(true);
        when(endpointMatcher.isPublicEndpoint(anyString(), anyString(), anyString())).thenReturn(false);
        MockServerWebExchange exchange = createExchange("/user/me", "GET");

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldRejectExternalAuthMode() {
        when(endpointMatcher.isServiceKnown("user-mgmt")).thenReturn(true);
        when(endpointMatcher.isPublicEndpoint(anyString(), anyString(), anyString())).thenReturn(false);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/user/me")
                        .header("X-Service-ID", "user-mgmt")
                        .header("X-Auth-Mode", "external").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.NOT_IMPLEMENTED, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldRejectMissingBearerToken() {
        when(endpointMatcher.isServiceKnown("user-mgmt")).thenReturn(true);
        when(endpointMatcher.isPublicEndpoint(anyString(), anyString(), anyString())).thenReturn(false);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/user/me")
                        .header("X-Service-ID", "user-mgmt")
                        .header("X-Auth-Mode", "internal").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldRejectInvalidToken() {
        when(endpointMatcher.isServiceKnown("user-mgmt")).thenReturn(true);
        when(endpointMatcher.isPublicEndpoint(anyString(), anyString(), anyString())).thenReturn(false);
        when(jwtValidator.validateToken("bad-token"))
                .thenThrow(new InvalidAccessTokenException("invalid signature"));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/user/me")
                        .header("X-Service-ID", "user-mgmt")
                        .header("X-Auth-Mode", "internal")
                        .header("Authorization", "Bearer bad-token").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldRejectExpiredToken() {
        when(endpointMatcher.isServiceKnown("user-mgmt")).thenReturn(true);
        when(endpointMatcher.isPublicEndpoint(anyString(), anyString(), anyString())).thenReturn(false);
        when(jwtValidator.validateToken("expired-token"))
                .thenThrow(new InvalidAccessTokenException("Token has expired"));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/user/me")
                        .header("X-Service-ID", "user-mgmt")
                        .header("X-Auth-Mode", "internal")
                        .header("Authorization", "Bearer expired-token").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldRejectTenantMismatch() {
        when(endpointMatcher.isServiceKnown("user-mgmt")).thenReturn(true);
        when(endpointMatcher.isPublicEndpoint(anyString(), anyString(), anyString())).thenReturn(false);
        when(jwtValidator.validateToken("good-token"))
                .thenReturn(new TokenClaims("user-1", "John", "+123", "tenant-1"));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/user/me")
                        .header("X-Service-ID", "user-mgmt")
                        .header("X-Auth-Mode", "internal")
                        .header("Authorization", "Bearer good-token")
                        .header("X-Tenant-ID", "tenant-2").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldRejectNonWhitelistedPrivateEndpoint() {
        when(endpointMatcher.isServiceKnown("user-mgmt")).thenReturn(true);
        when(endpointMatcher.isPublicEndpoint(anyString(), anyString(), anyString())).thenReturn(false);
        when(endpointMatcher.isPrivateEndpoint(anyString(), anyString(), anyString())).thenReturn(false);
        when(jwtValidator.validateToken("good-token"))
                .thenReturn(new TokenClaims("user-1", "John", "+123", "tenant-1"));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/unknown/path")
                        .header("X-Service-ID", "user-mgmt")
                        .header("X-Auth-Mode", "internal")
                        .header("Authorization", "Bearer good-token").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldEnrichHeadersOnSuccess() {
        when(endpointMatcher.isServiceKnown("user-mgmt")).thenReturn(true);
        when(endpointMatcher.isPublicEndpoint(anyString(), anyString(), anyString())).thenReturn(false);
        when(endpointMatcher.isPrivateEndpoint(anyString(), anyString(), anyString())).thenReturn(true);
        when(jwtValidator.validateToken("good-token"))
                .thenReturn(new TokenClaims("user-1", "John Doe", "+1234567890", "tenant-1"));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/user/me")
                        .header("X-Service-ID", "user-mgmt")
                        .header("X-Auth-Mode", "internal")
                        .header("Authorization", "Bearer good-token").build());

        when(chain.filter(any())).thenAnswer(inv -> {
            ServerWebExchange ex = inv.getArgument(0);
            assertEquals("user-1", ex.getRequest().getHeaders().getFirst("X-User-ID"));
            assertEquals("John Doe", ex.getRequest().getHeaders().getFirst("X-User-Name"));
            assertEquals("+1234567890", ex.getRequest().getHeaders().getFirst("X-User-Phone"));
            assertEquals("tenant-1", ex.getRequest().getHeaders().getFirst("X-Tenant-ID"));
            assertEquals("false", ex.getRequest().getHeaders().getFirst("X-Internal-Request"));
            return Mono.empty();
        });

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(any());
    }

    @Test
    void shouldMatchTenantWhenHeaderMatchesToken() {
        when(endpointMatcher.isServiceKnown("user-mgmt")).thenReturn(true);
        when(endpointMatcher.isPublicEndpoint(anyString(), anyString(), anyString())).thenReturn(false);
        when(endpointMatcher.isPrivateEndpoint(anyString(), anyString(), anyString())).thenReturn(true);
        when(jwtValidator.validateToken("good-token"))
                .thenReturn(new TokenClaims("user-1", "John", "+123", "tenant-1"));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/user/me")
                        .header("X-Service-ID", "user-mgmt")
                        .header("X-Auth-Mode", "internal")
                        .header("Authorization", "Bearer good-token")
                        .header("X-Tenant-ID", "tenant-1").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(any());
    }

    @Test
    void shouldPassWhenTenantHeaderIsBlank() {
        when(endpointMatcher.isServiceKnown("user-mgmt")).thenReturn(true);
        when(endpointMatcher.isPublicEndpoint(anyString(), anyString(), anyString())).thenReturn(false);
        when(endpointMatcher.isPrivateEndpoint(anyString(), anyString(), anyString())).thenReturn(true);
        when(jwtValidator.validateToken("good-token"))
                .thenReturn(new TokenClaims("user-1", "John", "+123", "tenant-1"));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/user/me")
                        .header("X-Service-ID", "user-mgmt")
                        .header("X-Auth-Mode", "internal")
                        .header("Authorization", "Bearer good-token")
                        .header("X-Tenant-ID", "  ").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(any());
    }

    @Test
    void shouldPassWhenNoTenantHeader() {
        when(endpointMatcher.isServiceKnown("user-mgmt")).thenReturn(true);
        when(endpointMatcher.isPublicEndpoint(anyString(), anyString(), anyString())).thenReturn(false);
        when(endpointMatcher.isPrivateEndpoint(anyString(), anyString(), anyString())).thenReturn(true);
        when(jwtValidator.validateToken("good-token"))
                .thenReturn(new TokenClaims("user-1", "John", "+123", "tenant-1"));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/user/me")
                        .header("X-Service-ID", "user-mgmt")
                        .header("X-Auth-Mode", "internal")
                        .header("Authorization", "Bearer good-token").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(any());
    }

    @Test
    void shouldExtractServicePathWithApiPrefix() {
        when(endpointMatcher.isServiceKnown("user-mgmt")).thenReturn(true);
        when(endpointMatcher.isPublicEndpoint(eq("user-mgmt"), eq("/users/me"), eq("GET"))).thenReturn(true);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/user-mgmt/api/users/me")
                        .header("X-Service-ID", "user-mgmt").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(any());
    }

    @Test
    void shouldRejectInvalidAuthMode() {
        when(endpointMatcher.isServiceKnown("user-mgmt")).thenReturn(true);
        when(endpointMatcher.isPublicEndpoint(anyString(), anyString(), anyString())).thenReturn(false);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/user/me")
                        .header("X-Service-ID", "user-mgmt")
                        .header("X-Auth-Mode", "unknown-mode").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldRejectAuthorizationHeaderWithoutBearerPrefix() {
        when(endpointMatcher.isServiceKnown("user-mgmt")).thenReturn(true);
        when(endpointMatcher.isPublicEndpoint(anyString(), anyString(), anyString())).thenReturn(false);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/user/me")
                        .header("X-Service-ID", "user-mgmt")
                        .header("X-Auth-Mode", "internal")
                        .header("Authorization", "Basic abc123").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void shortcutFieldOrderReturnsEmptyList() {
        assertTrue(factory.shortcutFieldOrder().isEmpty());
    }

    @Test
    void configClassCanBeInstantiated() {
        AuthenticationFilterFactory.Config config = new AuthenticationFilterFactory.Config();
        assertNotNull(config);
    }
}
