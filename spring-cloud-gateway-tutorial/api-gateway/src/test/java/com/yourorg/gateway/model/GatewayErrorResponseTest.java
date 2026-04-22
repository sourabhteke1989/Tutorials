package com.yourorg.gateway.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GatewayErrorResponseTest {

    @Test
    void shouldCreateErrorResponse() {
        GatewayErrorResponse response = GatewayErrorResponse.of(401, "Unauthorized", "Auth required", "corr-123");
        assertEquals(401, response.status());
        assertEquals("Unauthorized", response.error());
        assertEquals("Auth required", response.message());
        assertEquals("corr-123", response.correlationId());
        assertNotNull(response.timestamp());
    }

    @Test
    void shouldCreateWith500Status() {
        GatewayErrorResponse response = GatewayErrorResponse.of(500, "Internal Server Error", "Something failed", "corr-456");
        assertEquals(500, response.status());
    }
}
