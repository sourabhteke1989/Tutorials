package com.yourorg.usermgmt.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "ThisIsAVerySecureSecretKeyForHMACSHA256Algorithm!",
            900000, "yourorg.com", "yourorg.com");

    @Test
    void shouldGenerateToken() {
        String token = jwtService.generateToken("user-1", "John", "+123", "tenant-1");
        assertNotNull(token);
        assertFalse(token.isBlank());
        // JWT has 3 parts separated by dots
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void shouldGenerateDifferentTokens() {
        String token1 = jwtService.generateToken("user-1", "John", "+123", "tenant-1");
        String token2 = jwtService.generateToken("user-2", "Jane", "+456", "tenant-2");
        assertNotEquals(token1, token2);
    }
}
