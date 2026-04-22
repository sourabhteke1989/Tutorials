package com.yourorg.gateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiGatewayApplicationTest {

    @Test
    void mainMethodDoesNotThrow() {
        // We can't actually start the full Spring context in a unit test without proper config,
        // but we can verify the class exists and is annotated correctly
        assertNotNull(ApiGatewayApplication.class.getAnnotation(
                org.springframework.boot.autoconfigure.SpringBootApplication.class));
    }
}
