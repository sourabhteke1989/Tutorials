package com.framework.crud.security;

import java.util.Set;

/**
 * Abstraction over the current user's security context.
 * <p>
 * The framework calls this to determine the authenticated user's identity
 * and permissions before executing a CRUD operation.
 * <p>
 * A default implementation bridges to Spring Security's SecurityContextHolder.
 * Applications can provide their own bean to customise behaviour.
 */
public interface CrudSecurityContext {

    /**
     * Username / principal of the currently authenticated user.
     */
    String getCurrentUsername();

    /**
     * All roles assigned to the current user (e.g. "ROLE_ADMIN", "ROLE_USER").
     */
    Set<String> getCurrentRoles();

    /**
     * All fine-grained permissions assigned to the current user
     * (e.g. "product:read", "customer:write").
     * <p>
     * This merges both role-based and permission-based authorities.
     */
    Set<String> getCurrentPermissions();

    /**
     * Check whether the current user holds a specific permission string.
     */
    default boolean hasPermission(String permission) {
        if (permission == null || permission.isBlank()) {
            return true; // no permission required
        }
        return getCurrentPermissions().contains(permission);
    }

    /**
     * Check whether the current user holds any of the given roles.
     */
    default boolean hasAnyRole(String... roles) {
        Set<String> currentRoles = getCurrentRoles();
        for (String role : roles) {
            if (currentRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }
}
