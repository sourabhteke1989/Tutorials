package com.yourorg.gateway.model;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class TokenClaimsTest {

    @Test
    void shouldCreateValidTokenClaims() {
        TokenClaims claims = new TokenClaims("user-1", "John", "+1234567890", "tenant-1");
        assertEquals("user-1", claims.userId());
        assertEquals("John", claims.userName());
        assertEquals("+1234567890", claims.phoneNumber());
        assertEquals("tenant-1", claims.tenantId());
    }

    @Test
    void shouldDefaultNullUserNameToEmpty() {
        TokenClaims claims = new TokenClaims("user-1", null, null, "tenant-1");
        assertEquals("", claims.userName());
        assertEquals("", claims.phoneNumber());
    }

    @Test
    void shouldThrowWhenUserIdIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new TokenClaims(null, "John", "123", "tenant-1"));
    }

    @Test
    void shouldThrowWhenUserIdIsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> new TokenClaims("  ", "John", "123", "tenant-1"));
    }

    @Test
    void shouldThrowWhenTenantIdIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new TokenClaims("user-1", "John", "123", null));
    }

    @Test
    void shouldThrowWhenTenantIdIsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> new TokenClaims("user-1", "John", "123", ""));
    }
}
