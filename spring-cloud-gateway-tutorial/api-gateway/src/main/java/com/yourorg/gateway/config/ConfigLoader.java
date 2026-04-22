package com.yourorg.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads router and security configuration from external JSON files (REQ-RT-001, REQ-SC-001).
 * Fails fast if files are missing or malformed (REQ-RS-042).
 */
@Component
public class ConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);

    private final RouterConfig routerConfig;
    private final SecurityConfig securityConfig;

    public ConfigLoader(GatewayProperties gatewayProperties) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        this.routerConfig = loadConfig(mapper, gatewayProperties.routerConfigFilePath(), RouterConfig.class, "Router");
        this.securityConfig = loadConfig(mapper, gatewayProperties.securityConfigFilePath(), SecurityConfig.class, "Security");

        validate();
        log.info("Configuration loaded successfully. Allowed hosts: {}, Services: {}, UI Apps: {}",
                routerConfig.allowedHosts(),
                routerConfig.services().keySet(),
                routerConfig.uiApplications().keySet());
    }

    private <T> T loadConfig(ObjectMapper mapper, String filePath, Class<T> type, String label) {
        if (filePath == null || filePath.isBlank()) {
            throw new ConfigurationException(label + " configuration file path is not configured");
        }

        try {
            Path path = Path.of(filePath);
            if (Files.exists(path)) {
                log.info("Loading {} configuration from file: {}", label, path.toAbsolutePath());
                try (InputStream is = Files.newInputStream(path)) {
                    return mapper.readValue(is, type);
                }
            }

            // Try classpath
            InputStream cpStream = getClass().getClassLoader().getResourceAsStream(filePath);
            if (cpStream != null) {
                log.info("Loading {} configuration from classpath: {}", label, filePath);
                try (cpStream) {
                    return mapper.readValue(cpStream, type);
                }
            }

            throw new ConfigurationException(label + " configuration file not found: " + filePath);
        } catch (IOException e) {
            throw new ConfigurationException("Failed to parse " + label + " configuration: " + e.getMessage(), e);
        }
    }

    private void validate() {
        if (routerConfig.allowedHosts() == null || routerConfig.allowedHosts().isEmpty()) {
            throw new ConfigurationException("Router config must define at least one allowed host");
        }
        if (routerConfig.services() == null || routerConfig.services().isEmpty()) {
            throw new ConfigurationException("Router config must define at least one service");
        }
        if (securityConfig.services() == null || securityConfig.services().isEmpty()) {
            throw new ConfigurationException("Security config must define at least one service");
        }
    }

    public RouterConfig getRouterConfig() {
        return routerConfig;
    }

    public SecurityConfig getSecurityConfig() {
        return securityConfig;
    }

    public static class ConfigurationException extends RuntimeException {
        public ConfigurationException(String message) {
            super(message);
        }

        public ConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
