package com.framework.crud.security;

import com.framework.crud.definition.EntityDefinition;
import com.framework.crud.exception.CrudAccessDeniedException;
import com.framework.crud.model.CrudOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Checks whether the current user is allowed to perform an operation on an entity.
 * <p>
 * Resolution order:
 * <ol>
 *   <li>Check if the operation is in the entity's {@code allowedOperations} set.</li>
 *   <li>Look up the required permission from {@code EntityDefinition.getRequiredPermissions()}.</li>
 *   <li>Verify the current user holds that permission via {@link CrudSecurityContext}.</li>
 * </ol>
 */
@Service
public class PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionService.class);

    private final CrudSecurityContext securityContext;

    public PermissionService(CrudSecurityContext securityContext) {
        this.securityContext = securityContext;
    }

    /**
     * Check access. Throws {@link CrudAccessDeniedException} if denied.
     */
    public void checkAccess(EntityDefinition<?> definition, CrudOperation operation) {
        String entityType = definition.getEntityType();
        String username = securityContext.getCurrentUsername();

        // 1. Check if the operation is allowed on this entity at all
        if (!definition.getAllowedOperations().contains(operation)) {
            log.warn("Operation {} is not allowed on entity '{}' (user: {})",
                    operation, entityType, username);
            throw new CrudAccessDeniedException(
                    String.format("Operation '%s' is not supported for entity '%s'",
                            operation, entityType));
        }

        // 2. Look up required permission
        String requiredPermission = definition.getRequiredPermissions().get(operation);
        if (requiredPermission == null || requiredPermission.isBlank()) {
            // No specific permission required — allow
            log.debug("No permission required for {} on '{}' — allowing (user: {})",
                    operation, entityType, username);
            return;
        }

        // 3. Check user's permissions
        if (!securityContext.hasPermission(requiredPermission)) {
            log.warn("Access denied: user '{}' lacks permission '{}' for {} on '{}'",
                    username, requiredPermission, operation, entityType);
            throw new CrudAccessDeniedException(entityType, operation.name(), requiredPermission);
        }

        log.debug("Access granted: user '{}' has permission '{}' for {} on '{}'",
                username, requiredPermission, operation, entityType);
    }
}
