package com.yourorg.usermgmt.service;

import com.yourorg.usermgmt.model.TenantDto;
import com.yourorg.usermgmt.model.UserDto;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * In-memory data store with sample data for testing the gateway.
 */
@Service
public class DataStore {

    private final Map<String, TenantDto> tenants = new LinkedHashMap<>();
    private final Map<String, UserDto> usersById = new LinkedHashMap<>();
    private final Map<String, UserDto> usersByEmail = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        // Sample tenants
        addTenant(new TenantDto("acme", "Acme Corporation", "US-WEST", "US",
                "America/Los_Angeles", "internal", "active", null));
        addTenant(new TenantDto("globex", "Globex Corporation", "EU-WEST", "DE",
                "Europe/Berlin", "internal", "active", null));
        addTenant(new TenantDto("initech", "Initech", "US-EAST", "US",
                "America/New_York", "internal", "active", null));
        addTenant(new TenantDto("maxlogic", "MaxLogic Solutions", "US-EAST", "US",
                "America/New_York", "internal", "active", null));

        // Sample users for "acme" tenant
        addUser(new UserDto("550e8400-e29b-41d4-a716-446655440001", "acme",
                "John Admin", "admin@acme.com", "+1234567890", "active",
                Set.of("ROLE_ADMIN", "ROLE_USER")));
        addUser(new UserDto("550e8400-e29b-41d4-a716-446655440002", "acme",
                "Jane User", "jane@acme.com", "+1234567891", "active",
                Set.of("ROLE_USER")));

        // Sample users for "globex" tenant
        addUser(new UserDto("550e8400-e29b-41d4-a716-446655440003", "globex",
                "Hans Manager", "hans@globex.com", "+4912345678", "active",
                Set.of("ROLE_MANAGER", "ROLE_USER")));

        // Sample users for "maxlogic" tenant
        addUser(new UserDto("550e8400-e29b-41d4-a716-446655440010", "maxlogic",
                "Max Admin", "admin@maxlogic.com", "+1555000100", "active",
                Set.of("ROLE_ADMIN", "ROLE_USER")));
        addUser(new UserDto("550e8400-e29b-41d4-a716-446655440011", "maxlogic",
                "Lisa Engineer", "lisa@maxlogic.com", "+1555000101", "active",
                Set.of("ROLE_USER")));
    }

    private void addTenant(TenantDto tenant) {
        tenants.put(tenant.tenantId(), tenant);
    }

    private void addUser(UserDto user) {
        usersById.put(user.userId(), user);
        usersByEmail.put(user.email(), user);
    }

    public Optional<TenantDto> findTenantById(String tenantId) {
        return Optional.ofNullable(tenants.get(tenantId));
    }

    public Optional<UserDto> findUserById(String userId) {
        return Optional.ofNullable(usersById.get(userId));
    }

    public Optional<UserDto> findUserByEmail(String email) {
        return Optional.ofNullable(usersByEmail.get(email));
    }

    public List<UserDto> findUsersByTenant(String tenantId) {
        return usersById.values().stream()
                .filter(u -> tenantId.equals(u.tenantId()))
                .toList();
    }

    public boolean tenantExists(String tenantId) {
        return tenants.containsKey(tenantId);
    }

    public Map<String, String> getCredentials() {
        // Simple credential store for demo: email -> password
        return Map.of(
                "admin@acme.com", "admin123",
                "jane@acme.com", "jane123",
                "hans@globex.com", "hans123",
                "admin@maxlogic.com", "max123",
                "lisa@maxlogic.com", "lisa123"
        );
    }
}
