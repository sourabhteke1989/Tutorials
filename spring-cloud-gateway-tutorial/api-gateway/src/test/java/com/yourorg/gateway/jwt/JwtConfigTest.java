package com.yourorg.gateway.jwt;

import com.yourorg.gateway.config.JwtProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtConfigTest {

    private static final String SECRET = "ThisIsAVerySecureSecretKeyForHMACSHA256Algorithm!";

    @Test
    void shouldCreateJjwtValidatorByDefault() {
        JwtProperties props = new JwtProperties("jjwt", SECRET, 900000, "yourorg.com", "yourorg.com");
        JwtConfig config = new JwtConfig();
        JwtTokenValidator validator = config.jwtTokenValidator(props);
        assertInstanceOf(JjwtTokenValidator.class, validator);
    }

    @Test
    void shouldCreateNimbusValidator() {
        JwtProperties props = new JwtProperties("nimbus", SECRET, 900000, "yourorg.com", "yourorg.com");
        JwtConfig config = new JwtConfig();
        JwtTokenValidator validator = config.jwtTokenValidator(props);
        assertInstanceOf(NimbusJwtTokenValidator.class, validator);
    }

    @Test
    void shouldDefaultToJjwtForUnknownGenerator() {
        JwtProperties props = new JwtProperties("unknown", SECRET, 900000, "yourorg.com", "yourorg.com");
        JwtConfig config = new JwtConfig();
        JwtTokenValidator validator = config.jwtTokenValidator(props);
        assertInstanceOf(JjwtTokenValidator.class, validator);
    }

    @Test
    void shouldThrowWhenSecretKeyIsNull() {
        JwtProperties props = new JwtProperties("jjwt", null, 900000, "yourorg.com", "yourorg.com");
        JwtConfig config = new JwtConfig();
        assertThrows(IllegalStateException.class, () -> config.jwtTokenValidator(props));
    }

    @Test
    void shouldThrowWhenSecretKeyIsBlank() {
        JwtProperties props = new JwtProperties("jjwt", "  ", 900000, "yourorg.com", "yourorg.com");
        JwtConfig config = new JwtConfig();
        assertThrows(IllegalStateException.class, () -> config.jwtTokenValidator(props));
    }

    @Test
    void shouldThrowWhenSecretKeyTooShort() {
        JwtProperties props = new JwtProperties("jjwt", "short", 900000, "yourorg.com", "yourorg.com");
        JwtConfig config = new JwtConfig();
        assertThrows(IllegalStateException.class, () -> config.jwtTokenValidator(props));
    }
}
