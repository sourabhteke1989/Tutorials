package com.yourorg.gateway.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EndpointMatcherTest {

    @Test
    void convertToRegexSimplePath() {
        assertEquals("^/user/me$", EndpointMatcher.convertToRegex("/user/me"));
    }

    @Test
    void convertToRegexWithPathParam() {
        assertEquals("^/user/[^/]+$", EndpointMatcher.convertToRegex("/user/{resourceId}"));
    }

    @Test
    void convertToRegexWithMultipleParams() {
        assertEquals("^/org/[^/]+/user/[^/]+$",
                EndpointMatcher.convertToRegex("/org/{orgId}/user/{userId}"));
    }

    @Test
    void shouldMatchPublicEndpoint() {
        EndpointMatcher matcher = createMatcher();
        assertTrue(matcher.isPublicEndpoint("user-mgmt", "/auth/internal-auth/login", "POST"));
        assertTrue(matcher.isPublicEndpoint("user-mgmt", "/auth/internal-auth/initiate", "GET"));
    }

    @Test
    void shouldNotMatchNonPublicEndpoint() {
        EndpointMatcher matcher = createMatcher();
        assertFalse(matcher.isPublicEndpoint("user-mgmt", "/user/me", "GET"));
    }

    @Test
    void shouldMatchPrivateEndpoint() {
        EndpointMatcher matcher = createMatcher();
        assertTrue(matcher.isPrivateEndpoint("user-mgmt", "/user/me", "GET"));
    }

    @Test
    void shouldNotMatchUnknownPath() {
        EndpointMatcher matcher = createMatcher();
        assertFalse(matcher.isPrivateEndpoint("user-mgmt", "/unknown/path", "GET"));
    }

    @Test
    void shouldRecognizeKnownService() {
        EndpointMatcher matcher = createMatcher();
        assertTrue(matcher.isServiceKnown("user-mgmt"));
    }

    @Test
    void shouldNotRecognizeUnknownService() {
        EndpointMatcher matcher = createMatcher();
        assertFalse(matcher.isServiceKnown("unknown-service"));
    }

    @Test
    void shouldMatchPathParamEndpoint() {
        EndpointMatcher matcher = createMatcher();
        assertTrue(matcher.isPrivateEndpoint("user-mgmt", "/user/abc-123", "GET"));
    }

    @Test
    void shouldRespectHttpMethod() {
        EndpointMatcher matcher = createMatcher();
        assertTrue(matcher.isPublicEndpoint("user-mgmt", "/auth/internal-auth/login", "POST"));
        assertFalse(matcher.isPublicEndpoint("user-mgmt", "/auth/internal-auth/login", "GET"));
    }

    @Test
    void shouldBeCaseInsensitiveOnMethod() {
        EndpointMatcher matcher = createMatcher();
        assertTrue(matcher.isPublicEndpoint("user-mgmt", "/auth/internal-auth/login", "post"));
    }

    @Test
    void shouldReturnFalseForUnknownServiceEndpoints() {
        EndpointMatcher matcher = createMatcher();
        assertFalse(matcher.isPublicEndpoint("no-such-svc", "/any", "GET"));
        assertFalse(matcher.isPrivateEndpoint("no-such-svc", "/any", "GET"));
    }

    @Test
    void shouldHandleNullPublicEndpoints() {
        ConfigLoader configLoader = Mockito.mock(ConfigLoader.class);
        when(configLoader.getSecurityConfig()).thenReturn(new SecurityConfig(
                Map.of("svc", new SecurityConfig.ServiceSecurityConfig(null, List.of()))
        ));
        EndpointMatcher matcher = new EndpointMatcher(configLoader);
        assertFalse(matcher.isPublicEndpoint("svc", "/any", "GET"));
    }

    private EndpointMatcher createMatcher() {
        SecurityConfig securityConfig = new SecurityConfig(
                Map.of("user-mgmt", new SecurityConfig.ServiceSecurityConfig(
                        List.of(
                                new SecurityConfig.EndpointConfig("/auth/internal-auth/login", "POST"),
                                new SecurityConfig.EndpointConfig("/auth/internal-auth/initiate", "GET"),
                                new SecurityConfig.EndpointConfig("/auth/internal-auth/identify", "POST")
                        ),
                        List.of(
                                new SecurityConfig.EndpointConfig("/user/me", "GET"),
                                new SecurityConfig.EndpointConfig("/user/{resourceId}", "GET"),
                                new SecurityConfig.EndpointConfig("/user/list", "GET"),
                                new SecurityConfig.EndpointConfig("/user/{resourceId}", "PUT")
                        )
                ))
        );

        ConfigLoader configLoader = Mockito.mock(ConfigLoader.class);
        when(configLoader.getSecurityConfig()).thenReturn(securityConfig);
        return new EndpointMatcher(configLoader);
    }
}
