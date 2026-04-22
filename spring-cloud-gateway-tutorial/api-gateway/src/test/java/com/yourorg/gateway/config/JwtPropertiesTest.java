package com.yourorg.gateway.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtPropertiesTest {

    @Test
    void shouldApplyDefaults() {
        JwtProperties props = new JwtProperties(null, "secret", 0, null, null);
        assertEquals("jjwt", props.generator());
        assertEquals(900000L, props.expiration());
        assertEquals("yourorg.com", props.issuer());
        assertEquals("yourorg.com", props.audience());
    }

    @Test
    void shouldAcceptExplicitValues() {
        JwtProperties props = new JwtProperties("nimbus", "key", 60000, "iss", "aud");
        assertEquals("nimbus", props.generator());
        assertEquals(60000, props.expiration());
        assertEquals("iss", props.issuer());
        assertEquals("aud", props.audience());
    }

    @Test
    void shouldDefaultBlankGenerator() {
        JwtProperties props = new JwtProperties("  ", "key", 1000, "iss", "aud");
        assertEquals("jjwt", props.generator());
    }
}
