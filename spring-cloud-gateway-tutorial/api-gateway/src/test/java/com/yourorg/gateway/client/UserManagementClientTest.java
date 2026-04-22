package com.yourorg.gateway.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.yourorg.gateway.config.ConfigLoader;
import com.yourorg.gateway.config.GatewayProperties;
import com.yourorg.gateway.config.RouterConfig;
import com.yourorg.gateway.model.TenantBasicDetail;
import com.yourorg.gateway.model.UserDetail;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UserManagementClientTest {

    private MockWebServer mockWebServer;
    private UserManagementClient client;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/").toString();
        // Remove trailing slash
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        // Create a ConfigLoader with user-mgmt pointing to mock server
        // We need a minimal approach - use reflection or a custom setup
        // Simplest: use the config files but override user-mgmt URL
        // Actually, since ConfigLoader reads from files, we can create a custom test instance
        // Let's use the constructor directly via a mock-like approach
        client = createClientWithBaseUrl(baseUrl);

        objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    private UserManagementClient createClientWithBaseUrl(String baseUrl) {
        // Create a ConfigLoader with the mock URL for user-mgmt
        GatewayProperties props = new GatewayProperties(
                "router-config-monolith.json",
                "security-config.json",
                true, false, "production", null, null);
        ConfigLoader realLoader = new ConfigLoader(props);

        // Replace user-mgmt URL by creating a custom RouterConfig
        Map<String, String> services = new java.util.HashMap<>(realLoader.getRouterConfig().services());
        services.put("user-mgmt", baseUrl);

        // Create a wrapper ConfigLoader-like setup using a custom approach
        // Since ConfigLoader constructor reads from files, we'll use a test-friendly approach
        // by creating an anonymous ConfigLoader subclass is not ideal...
        // Instead, let's just create the client directly using reflection or a test config

        // Simplest: create a custom config file in test resources
        // But actually, the UserManagementClient constructor takes ConfigLoader
        // Let's create a mock ConfigLoader
        ConfigLoader mockLoader = org.mockito.Mockito.mock(ConfigLoader.class);
        RouterConfig routerConfig = new RouterConfig(
                List.of("localhost"),
                "http://localhost:3000",
                "http://localhost:3000",
                Map.of("user-mgmt", baseUrl),
                Map.of()
        );
        org.mockito.Mockito.when(mockLoader.getRouterConfig()).thenReturn(routerConfig);

        return new UserManagementClient(mockLoader);
    }

    @Test
    void getTenantDetails_success() throws Exception {
        String responseJson = """
                {
                    "tenant_id": "tenant-1",
                    "name": "Acme Corp",
                    "region": "us-east",
                    "country": "US",
                    "timezone": "America/New_York",
                    "auth_mode": "internal",
                    "status": "active",
                    "auth_url": null
                }
                """;
        mockWebServer.enqueue(new MockResponse()
                .setBody(responseJson)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(client.getTenantDetails("tenant-1"))
                .assertNext(tenant -> {
                    assertEquals("tenant-1", tenant.tenantId());
                    assertEquals("Acme Corp", tenant.name());
                    assertEquals("us-east", tenant.region());
                    assertEquals("US", tenant.country());
                })
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("/organization/details?tenant_id=tenant-1", request.getPath());
        assertEquals("true", request.getHeader("X-Internal-Request"));
    }

    @Test
    void getUserByUsername_success() throws Exception {
        String responseJson = """
                {
                    "user_id": "user-1",
                    "username": "john",
                    "display_name": "John Doe",
                    "email": "john@acme.com",
                    "phone_number": "+1234567890",
                    "tenant_id": "tenant-1",
                    "status": "active",
                    "roles": ["admin"]
                }
                """;
        mockWebServer.enqueue(new MockResponse()
                .setBody(responseJson)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(client.getUserByUsername("john", "tenant-1"))
                .assertNext(user -> assertNotNull(user))
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        assertTrue(request.getPath().contains("/user/by-username"));
        assertTrue(request.getPath().contains("username=john"));
    }

    @Test
    void getUserByToken_success() throws Exception {
        String responseJson = """
                {
                    "user_id": "user-1",
                    "username": "john",
                    "display_name": "John Doe",
                    "email": "john@acme.com",
                    "phone_number": "+1234567890",
                    "tenant_id": "tenant-1",
                    "status": "active",
                    "roles": ["admin"]
                }
                """;
        mockWebServer.enqueue(new MockResponse()
                .setBody(responseJson)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(client.getUserByToken("test-access-token"))
                .assertNext(user -> assertNotNull(user))
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("/user/me", request.getPath());
        assertEquals("Bearer test-access-token", request.getHeader("Authorization"));
        assertEquals("true", request.getHeader("X-Internal-Request"));
    }

    @Test
    void getUserAccessResourceIds_success() throws Exception {
        String responseJson = """
                ["resource-1", "resource-2", "resource-3"]
                """;
        mockWebServer.enqueue(new MockResponse()
                .setBody(responseJson)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(client.getUserAccessResourceIds("user-1", "app-1", "document", "read"))
                .assertNext(ids -> assertFalse(ids.isEmpty()))
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        assertTrue(request.getPath().contains("/user-access-control/resource-ids"));
        assertTrue(request.getPath().contains("user_id=user-1"));
        assertTrue(request.getPath().contains("app_id=app-1"));
    }

    @Test
    void hasUserAccessToResource_returnsTrue() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody("true")
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(client.hasUserAccessToResource("user-1", "resource-1", "write"))
                .assertNext(result -> assertTrue(result))
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        assertTrue(request.getPath().contains("/user-access-control/has-access"));
        assertTrue(request.getPath().contains("user_id=user-1"));
        assertTrue(request.getPath().contains("resource_id=resource-1"));
    }

    @Test
    void hasUserAccessToResource_returnsFalse() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody("false")
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(client.hasUserAccessToResource("user-1", "resource-1", "write"))
                .assertNext(result -> assertFalse(result))
                .verifyComplete();
    }

    @Test
    void getTenantDetails_serverError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        StepVerifier.create(client.getTenantDetails("tenant-1"))
                .expectError()
                .verify();
    }

    @Test
    void getUserByUsername_serverError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        StepVerifier.create(client.getUserByUsername("john", "tenant-1"))
                .expectError()
                .verify();
    }

    @Test
    void getUserByToken_serverError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        StepVerifier.create(client.getUserByToken("bad-token"))
                .expectError()
                .verify();
    }

    @Test
    void constructorThrowsWhenUserMgmtNotConfigured() {
        ConfigLoader mockLoader = org.mockito.Mockito.mock(ConfigLoader.class);
        RouterConfig routerConfig = new RouterConfig(
                List.of("localhost"), null, null,
                Map.of("other-service", "http://localhost:8080"),
                Map.of()
        );
        org.mockito.Mockito.when(mockLoader.getRouterConfig()).thenReturn(routerConfig);

        assertThrows(IllegalStateException.class, () -> new UserManagementClient(mockLoader));
    }
}
