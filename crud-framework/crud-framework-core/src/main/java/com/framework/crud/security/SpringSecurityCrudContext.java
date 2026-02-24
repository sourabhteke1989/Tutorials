package com.framework.crud.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation that bridges to Spring Security's {@code SecurityContextHolder}.
 * <p>
 * Roles and permissions are derived from the user's {@link GrantedAuthority} list.
 * This bean is auto-configured but can be replaced by providing a custom
 * {@link CrudSecurityContext} bean.
 */
public class SpringSecurityCrudContext implements CrudSecurityContext {

    @Override
    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "anonymous";
        }
        return auth.getName();
    }

    @Override
    public Set<String> getCurrentRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Collections.emptySet();
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getCurrentPermissions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Collections.emptySet();
        }
        // Return ALL authorities (both roles and permissions)
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
