package com.yourorg.gateway.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GatewayPropertiesTest {

    @Test
    void shouldApplyDefaults() {
        GatewayProperties props = new GatewayProperties(
                "router.json", "security.json", true, false, null, null, null);
        assertEquals("production", props.defaultEnvironment());
        assertNotNull(props.rateLimit());
        assertNotNull(props.resilience());
        assertEquals(1000, props.rateLimit().perTenantLimit());
        assertEquals(100, props.rateLimit().perIpLimit());
        assertEquals(500, props.rateLimit().perUserLimit());
        assertEquals(60, props.rateLimit().windowSeconds());
    }

    @Test
    void shouldOverrideDefaultEnvironment() {
        GatewayProperties props = new GatewayProperties(
                "r.json", "s.json", true, false, "staging", null, null);
        assertEquals("staging", props.defaultEnvironment());
    }

    @Test
    void rateLimitShouldDefaultInvalidValues() {
        GatewayProperties.RateLimitProperties rl = new GatewayProperties.RateLimitProperties(0, -1, 0, -5);
        assertEquals(1000, rl.perTenantLimit());
        assertEquals(100, rl.perIpLimit());
        assertEquals(500, rl.perUserLimit());
        assertEquals(60, rl.windowSeconds());
    }

    @Test
    void shouldUseBlankEnvironmentDefault() {
        GatewayProperties props = new GatewayProperties(
                "r.json", "s.json", true, false, "  ", null, null);
        assertEquals("production", props.defaultEnvironment());
    }
}
