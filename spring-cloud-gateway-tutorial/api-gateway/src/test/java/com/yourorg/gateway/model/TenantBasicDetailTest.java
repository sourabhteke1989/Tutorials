package com.yourorg.gateway.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TenantBasicDetailTest {

    @Test
    void shouldCreateTenantBasicDetail() {
        TenantBasicDetail tenant = new TenantBasicDetail("t1", "Acme", "US-EAST", "US", "UTC", "internal", "active", null);
        assertEquals("t1", tenant.tenantId());
        assertEquals("Acme", tenant.name());
        assertEquals("US-EAST", tenant.region());
        assertEquals("US", tenant.country());
        assertEquals("UTC", tenant.timezone());
        assertEquals("internal", tenant.authMode());
        assertEquals("active", tenant.status());
        assertNull(tenant.authUrl());
    }
}
