package com.yourorg.usermgmt.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataStoreTest {

    private DataStore dataStore;

    @BeforeEach
    void setUp() {
        dataStore = new DataStore();
        dataStore.init();
    }

    @Test
    void shouldFindTenantById() {
        var tenant = dataStore.findTenantById("acme");
        assertTrue(tenant.isPresent());
        assertEquals("Acme Corporation", tenant.get().name());
    }

    @Test
    void shouldReturnEmptyForUnknownTenant() {
        assertTrue(dataStore.findTenantById("unknown").isEmpty());
    }

    @Test
    void shouldFindUserById() {
        var user = dataStore.findUserById("550e8400-e29b-41d4-a716-446655440001");
        assertTrue(user.isPresent());
        assertEquals("John Admin", user.get().userName());
    }

    @Test
    void shouldFindUserByEmail() {
        var user = dataStore.findUserByEmail("admin@acme.com");
        assertTrue(user.isPresent());
    }

    @Test
    void shouldReturnEmptyForUnknownEmail() {
        assertTrue(dataStore.findUserByEmail("unknown@test.com").isEmpty());
    }

    @Test
    void shouldFindUsersByTenant() {
        var users = dataStore.findUsersByTenant("acme");
        assertEquals(2, users.size());
    }

    @Test
    void shouldReturnEmptyListForUnknownTenant() {
        assertTrue(dataStore.findUsersByTenant("unknown").isEmpty());
    }

    @Test
    void shouldCheckTenantExists() {
        assertTrue(dataStore.tenantExists("acme"));
        assertFalse(dataStore.tenantExists("unknown"));
    }

    @Test
    void shouldReturnCredentials() {
        var creds = dataStore.getCredentials();
        assertEquals("admin123", creds.get("admin@acme.com"));
        assertEquals("jane123", creds.get("jane@acme.com"));
        assertEquals("hans123", creds.get("hans@globex.com"));
    }
}
