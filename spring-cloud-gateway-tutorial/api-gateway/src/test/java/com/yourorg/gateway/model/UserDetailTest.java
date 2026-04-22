package com.yourorg.gateway.model;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class UserDetailTest {

    @Test
    void shouldCreateUserDetail() {
        UserDetail user = new UserDetail("u1", "t1", "John", "john@test.com", "+123", "active", Set.of("ADMIN"));
        assertEquals("u1", user.userId());
        assertEquals("t1", user.tenantId());
        assertEquals("John", user.userName());
        assertEquals("john@test.com", user.email());
        assertTrue(user.authorities().contains("ADMIN"));
    }

    @Test
    void shouldMatchTenant() {
        UserDetail user = new UserDetail("u1", "t1", "John", "john@test.com", "+123", "active", Set.of());
        assertTrue(user.belongsToTenant("t1"));
        assertFalse(user.belongsToTenant("t2"));
    }

    @Test
    void shouldHandleNullTenantId() {
        UserDetail user = new UserDetail("u1", null, "John", "john@test.com", "+123", "active", Set.of());
        assertFalse(user.belongsToTenant("t1"));
    }
}
