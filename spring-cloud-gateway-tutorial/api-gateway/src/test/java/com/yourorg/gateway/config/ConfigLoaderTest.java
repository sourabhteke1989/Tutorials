package com.yourorg.gateway.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {

    @Test
    void shouldLoadConfigFromClasspath() {
        GatewayProperties props = new GatewayProperties(
                "router-config-monolith.json",
                "security-config.json",
                true, false, "production", null, null);
        ConfigLoader loader = new ConfigLoader(props);
        assertNotNull(loader.getRouterConfig());
        assertNotNull(loader.getSecurityConfig());
        assertFalse(loader.getRouterConfig().allowedHosts().isEmpty());
        assertFalse(loader.getRouterConfig().services().isEmpty());
        assertFalse(loader.getSecurityConfig().services().isEmpty());
    }

    @Test
    void shouldFailWithMissingRouterFile() {
        GatewayProperties props = new GatewayProperties(
                "nonexistent-router.json",
                "security-config.json",
                true, false, "production", null, null);
        assertThrows(ConfigLoader.ConfigurationException.class, () -> new ConfigLoader(props));
    }

    @Test
    void shouldFailWithMissingSecurityFile() {
        GatewayProperties props = new GatewayProperties(
                "router-config-monolith.json",
                "nonexistent-security.json",
                true, false, "production", null, null);
        assertThrows(ConfigLoader.ConfigurationException.class, () -> new ConfigLoader(props));
    }

    @Test
    void shouldFailWithNullFilePath() {
        GatewayProperties props = new GatewayProperties(
                null,
                "security-config.json",
                true, false, "production", null, null);
        assertThrows(ConfigLoader.ConfigurationException.class, () -> new ConfigLoader(props));
    }

    @Test
    void shouldFailWithBlankFilePath() {
        GatewayProperties props = new GatewayProperties(
                "  ",
                "security-config.json",
                true, false, "production", null, null);
        assertThrows(ConfigLoader.ConfigurationException.class, () -> new ConfigLoader(props));
    }

    @Test
    void configurationExceptionWithCause() {
        RuntimeException cause = new RuntimeException("root cause");
        ConfigLoader.ConfigurationException ex = new ConfigLoader.ConfigurationException("msg", cause);
        assertEquals("msg", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void configurationExceptionWithMessage() {
        ConfigLoader.ConfigurationException ex = new ConfigLoader.ConfigurationException("msg");
        assertEquals("msg", ex.getMessage());
    }

    @Test
    void shouldLoadConfigFromFileSystem(@TempDir Path tempDir) throws IOException {
        // Create a valid router config on the file system
        Path routerFile = tempDir.resolve("router.json");
        Files.writeString(routerFile, """
                {
                    "allowed_hosts": ["localhost"],
                    "website_url": "http://localhost:3000",
                    "landing_app_url": "http://localhost:3000",
                    "services": {"user-mgmt": "http://localhost:8081"},
                    "ui_applications": {"app": "http://localhost:3000"}
                }
                """);

        Path securityFile = tempDir.resolve("security.json");
        Files.writeString(securityFile, """
                {
                    "services": {
                        "user-mgmt": {
                            "public_endpoints": [{"url": "/auth/login", "method": "POST"}],
                            "private_endpoints": [{"url": "/user/me", "method": "GET"}]
                        }
                    }
                }
                """);

        GatewayProperties props = new GatewayProperties(
                routerFile.toString(),
                securityFile.toString(),
                true, false, "production", null, null);
        ConfigLoader loader = new ConfigLoader(props);

        assertNotNull(loader.getRouterConfig());
        assertEquals(1, loader.getRouterConfig().allowedHosts().size());
        assertEquals("localhost", loader.getRouterConfig().allowedHosts().get(0));
    }

    @Test
    void shouldFailWithMalformedJson(@TempDir Path tempDir) throws IOException {
        Path routerFile = tempDir.resolve("bad-router.json");
        Files.writeString(routerFile, "{ invalid json }}}");

        GatewayProperties props = new GatewayProperties(
                routerFile.toString(),
                "security-config.json",
                true, false, "production", null, null);
        assertThrows(ConfigLoader.ConfigurationException.class, () -> new ConfigLoader(props));
    }

    @Test
    void shouldFailWhenRouterConfigHasEmptyHosts(@TempDir Path tempDir) throws IOException {
        Path routerFile = tempDir.resolve("router.json");
        Files.writeString(routerFile, """
                {
                    "allowed_hosts": [],
                    "services": {"user-mgmt": "http://localhost:8081"},
                    "ui_applications": {}
                }
                """);

        GatewayProperties props = new GatewayProperties(
                routerFile.toString(),
                "security-config.json",
                true, false, "production", null, null);
        assertThrows(ConfigLoader.ConfigurationException.class, () -> new ConfigLoader(props));
    }

    @Test
    void shouldFailWhenRouterConfigHasEmptyServices(@TempDir Path tempDir) throws IOException {
        Path routerFile = tempDir.resolve("router.json");
        Files.writeString(routerFile, """
                {
                    "allowed_hosts": ["localhost"],
                    "services": {},
                    "ui_applications": {}
                }
                """);

        GatewayProperties props = new GatewayProperties(
                routerFile.toString(),
                "security-config.json",
                true, false, "production", null, null);
        assertThrows(ConfigLoader.ConfigurationException.class, () -> new ConfigLoader(props));
    }

    @Test
    void shouldFailWhenSecurityConfigHasEmptyServices(@TempDir Path tempDir) throws IOException {
        Path routerFile = tempDir.resolve("router.json");
        Files.writeString(routerFile, """
                {
                    "allowed_hosts": ["localhost"],
                    "services": {"user-mgmt": "http://localhost:8081"},
                    "ui_applications": {}
                }
                """);

        Path securityFile = tempDir.resolve("security.json");
        Files.writeString(securityFile, """
                {
                    "services": {}
                }
                """);

        GatewayProperties props = new GatewayProperties(
                routerFile.toString(),
                securityFile.toString(),
                true, false, "production", null, null);
        assertThrows(ConfigLoader.ConfigurationException.class, () -> new ConfigLoader(props));
    }
}
