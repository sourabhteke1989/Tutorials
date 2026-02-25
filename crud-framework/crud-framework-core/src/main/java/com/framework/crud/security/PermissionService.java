package com.framework.crud.security;

import com.framework.crud.definition.EntityDefinition;
import com.framework.crud.exception.CrudAccessDeniedException;
import com.framework.crud.model.CrudOperation;
import com.framework.crud.model.FilterPermission;
import com.framework.crud.model.ManyToManyRelation;
import com.framework.crud.model.PermissionConfig;
import com.framework.crud.model.RelationOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;

/**
 * Checks whether the current user is allowed to perform an operation on an entity.
 * <p>
 * The service supports comprehensive permission checking:
 * <ul>
 *   <li><b>CRUD operations</b> — Checks named permissions from {@link PermissionConfig}
 *       (LIST, GET, CREATE, UPDATE, DELETE).</li>
 *   <li><b>LIST vs GET distinction</b> — A GET operation without an ID is treated as LIST
 *       and requires {@code listPermission}. A GET with an ID requires {@code getPermission}.</li>
 *   <li><b>Filter-based permissions</b> — When a LIST request includes filters matching
 *       a defined {@link FilterPermission}, the filter-specific permission is required
 *       instead of the generic {@code listPermission}.</li>
 *   <li><b>Relation permissions</b> — Many-to-many relation operations check relation-specific
 *       permissions defined on the {@link ManyToManyRelation}.</li>
 * </ul>
 *
 * @see PermissionConfig
 * @see FilterPermission
 */
@Service
public class PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionService.class);

    private final CrudSecurityContext securityContext;

    public PermissionService(CrudSecurityContext securityContext) {
        this.securityContext = securityContext;
    }

    /**
     * Check access for a CRUD operation, with LIST vs GET distinction and filter-based permissions.
     * <p>
     * For GET operations:
     * <ul>
     *   <li>If {@code id} is provided → requires {@code getPermission}.</li>
     *   <li>If {@code id} is null (LIST) and filters match a {@link FilterPermission} →
     *       requires that filter's permission.</li>
     *   <li>If {@code id} is null (LIST) with filters but the entity has permissions configured
     *       and no matching FilterPermission → <b>access denied</b> (only explicitly
     *       allowed filter combinations are permitted; if zero FilterPermissions are defined,
     *       no filtering is allowed at all).</li>
     *   <li>If {@code id} is null (LIST) with no filters → requires {@code listPermission}.</li>
     * </ul>
     *
     * @param definition the entity definition
     * @param operation  the CRUD operation
     * @param id         the entity ID (null for LIST operations)
     * @param filters    the request filter map (may be null or empty)
     * @throws CrudAccessDeniedException if access is denied
     */
    public void checkAccess(EntityDefinition<?> definition, CrudOperation operation,
                            Object id, Map<String, Object> filters) {
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

        // 2. Resolve the required permission from PermissionConfig
        PermissionConfig config = definition.getPermissionConfig();
        String requiredPermission = resolvePermission(config, operation, id, filters);

        if (requiredPermission == null || requiredPermission.isBlank()) {
            log.debug("No permission required for {} on '{}' — allowing (user: {})",
                    operation, entityType, username);
            return;
        }

        // 3. Check user's permissions
        if (!securityContext.hasPermission(requiredPermission)) {
            String operationLabel = (operation == CrudOperation.GET && id == null) ? "LIST" : operation.name();
            log.warn("Access denied: user '{}' lacks permission '{}' for {} on '{}'",
                    username, requiredPermission, operationLabel, entityType);
            throw new CrudAccessDeniedException(entityType, operationLabel, requiredPermission);
        }

        String operationLabel = (operation == CrudOperation.GET && id == null) ? "LIST" : operation.name();
        log.debug("Access granted: user '{}' has permission '{}' for {} on '{}'",
                username, requiredPermission, operationLabel, entityType);
    }

    /**
     * Check access for a many-to-many relation operation.
     * <p>
     * Uses relation-specific permissions defined on {@link ManyToManyRelation}:
     * <ul>
     *   <li>GET → {@code relation.getGetPermission()}</li>
     *   <li>ADD → {@code relation.getAddPermission()}</li>
     *   <li>REMOVE → {@code relation.getRemovePermission()}</li>
     * </ul>
     * If no relation-specific permission is defined, the check is skipped (open access).
     *
     * @param relation  the many-to-many relation definition
     * @param operation the relation operation (GET, ADD, REMOVE)
     * @param sourceEntityType the source entity type (for error messages)
     * @throws CrudAccessDeniedException if access is denied
     */
    public void checkRelationAccess(ManyToManyRelation relation, RelationOperation operation,
                                    String sourceEntityType) {
        String username = securityContext.getCurrentUsername();
        String relationName = relation.getRelationName();

        // Resolve the permission for this relation operation
        String requiredPermission = switch (operation) {
            case GET -> relation.getGetPermission();
            case ADD -> relation.getAddPermission();
            case REMOVE -> relation.getRemovePermission();
        };

        if (requiredPermission == null || requiredPermission.isBlank()) {
            log.debug("No relation permission required for {} on '{}.{}' — allowing (user: {})",
                    operation, sourceEntityType, relationName, username);
            return;
        }

        if (!securityContext.hasPermission(requiredPermission)) {
            log.warn("Access denied: user '{}' lacks permission '{}' for {} on relation '{}.{}'",
                    username, requiredPermission, operation, sourceEntityType, relationName);
            throw new CrudAccessDeniedException(
                    String.format("Access denied: permission '%s' required for %s on relation '%s.%s'",
                            requiredPermission, operation, sourceEntityType, relationName));
        }

        log.debug("Access granted: user '{}' has permission '{}' for {} on relation '{}.{}'",
                username, requiredPermission, operation, sourceEntityType, relationName);
    }

    /**
     * Resolve the required permission string from the PermissionConfig based on the
     * operation type, whether an ID is present, and the filter keys.
     */
    private String resolvePermission(PermissionConfig config, CrudOperation operation,
                                     Object id, Map<String, Object> filters) {
        return switch (operation) {
            case GET -> {
                if (id != null) {
                    // Single-record GET — use getPermission
                    yield config.getGetPermission();
                }
                // LIST operation — check for filter-based permission first
                yield resolveListPermission(config, filters);
            }
            case CREATE -> config.getCreatePermission();
            case UPDATE -> config.getUpdatePermission();
            case DELETE -> config.getDeletePermission();
        };
    }

    /**
     * For LIST operations, find the most specific matching FilterPermission.
     * <p>
     * When the entity has a {@link PermissionConfig} with permissions defined and the
     * request contains filters, the filter keys must match a configured
     * {@link FilterPermission} — otherwise the request is <b>denied</b>.
     * This applies whether the entity defines FilterPermissions or not: if none are
     * configured, no filtering is allowed at all.
     * <p>
     * Entities with no PermissionConfig (i.e. {@code PermissionConfig.empty()}) are
     * unaffected — filters fall back to the generic {@code listPermission}.
     */
    private String resolveListPermission(PermissionConfig config, Map<String, Object> filters) {
        if (filters != null && !filters.isEmpty()) {
            if (config.hasPermissions()) {
                // Entity has opted into the permission system —
                // only explicitly configured filter combinations are allowed
                if (config.getFilterPermissions().isEmpty()) {
                    log.warn("Filtering is not allowed on this entity — no FilterPermissions configured. " +
                            "Requested filter keys: {}", filters.keySet());
                    throw new CrudAccessDeniedException(
                            String.format("Filtering by %s is not allowed on this entity. "
                                    + "No filter combinations are configured.", filters.keySet()));
                }

                Set<String> filterKeys = filters.keySet();

                // Find the most specific matching FilterPermission (most matching fields)
                return config.getFilterPermissions().stream()
                        .filter(fp -> fp.matches(filterKeys))
                        .max(Comparator.comparingInt(FilterPermission::specificity))
                        .map(fp -> {
                            log.debug("Filter permission matched: {} for filter keys {}", fp, filterKeys);
                            return fp.getPermission();
                        })
                        .orElseThrow(() -> {
                            log.warn("No filter permission configured for filter keys: {}", filterKeys);
                            return new CrudAccessDeniedException(
                                    String.format("Filtering by %s is not allowed on this entity. "
                                            + "Only configured filter combinations are permitted.", filterKeys));
                        });
            }
            // PermissionConfig.empty() — no permission system, allow any filter
        }
        return config.getListPermission();
    }
}
