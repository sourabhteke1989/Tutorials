package com.framework.crud.service;

import com.framework.crud.definition.EntityDefinition;
import com.framework.crud.event.CrudEventPublisher;
import com.framework.crud.exception.DuplicateEntityException;
import com.framework.crud.model.CrudOperation;
import com.framework.crud.model.CrudRequest;
import com.framework.crud.model.CrudResponse;
import com.framework.crud.model.UniqueConstraint;
import com.framework.crud.registry.EntityRegistry;
import com.framework.crud.repository.DynamicCrudRepository;
import com.framework.crud.security.PermissionService;
import com.framework.crud.validation.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central orchestrator for all CRUD operations.
 * <p>
 * Processing pipeline for every request:
 * <ol>
 *   <li>Resolve the {@link EntityDefinition} from the entity type.</li>
 *   <li>Parse and validate the requested operation.</li>
 *   <li>Check RBAC permissions via {@link PermissionService}.</li>
 *   <li>For CREATE/UPDATE: validate payload via {@link ValidationService}.</li>
 *   <li>Publish PRE event.</li>
 *   <li>Call {@code EntityDefinition.beforeSave()} hook (CREATE/UPDATE).</li>
 *   <li>Execute the operation via {@link DynamicCrudRepository}.</li>
 *   <li>Call {@code EntityDefinition.afterSave()} hook.</li>
 *   <li>Publish POST event.</li>
 *   <li>Write audit log.</li>
 *   <li>Return {@link CrudResponse}.</li>
 * </ol>
 */
@Service
public class CrudService {

    private static final Logger log = LoggerFactory.getLogger(CrudService.class);

    private final EntityRegistry entityRegistry;
    private final PermissionService permissionService;
    private final ValidationService validationService;
    private final DynamicCrudRepository repository;
    private final AuditService auditService;
    private final CrudEventPublisher eventPublisher;

    public CrudService(EntityRegistry entityRegistry,
                       PermissionService permissionService,
                       ValidationService validationService,
                       DynamicCrudRepository repository,
                       AuditService auditService,
                       CrudEventPublisher eventPublisher) {
        this.entityRegistry = entityRegistry;
        this.permissionService = permissionService;
        this.validationService = validationService;
        this.repository = repository;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Process a generic CRUD request.
     */
    @Transactional
    public CrudResponse process(CrudRequest request) {
        // 1. Basic request validation
        validateRequest(request);

        // 2. Resolve entity definition
        EntityDefinition<?> definition = entityRegistry.getDefinition(request.getEntityType());
        CrudOperation operation = request.resolveOperation();

        log.info("Processing {} on entity '{}' (table: {})",
                operation, definition.getEntityType(), definition.getTableName());

        // 3. Permission check (LIST vs GET distinction + filter-based permissions)
        permissionService.checkAccess(definition, operation, request.getId(), request.getFilters());

        // 4. Dispatch to operation handler
        return switch (operation) {
            case GET -> handleGet(definition, request);
            case CREATE -> handleCreate(definition, request);
            case UPDATE -> handleUpdate(definition, request);
            case DELETE -> handleDelete(definition, request);
        };
    }

    // =========================================================================
    // GET
    // =========================================================================

    private CrudResponse handleGet(EntityDefinition<?> definition, CrudRequest request) {
        List<String> projectionColumns = resolveProjectionColumns(definition, request.getProjectionType());

        if (request.getId() != null) {
            // Single entity by ID
            Map<String, Object> data = repository.findById(definition, request.getId(), projectionColumns);

            eventPublisher.publishPost(definition.getEntityType(), CrudOperation.GET,
                    request.getId(), null);
            auditService.auditQuery(definition.getEntityType(), request.getId(),
                    null, request.getProjectionType(),
                    null, null, null, null);

            return CrudResponse.success("Entity retrieved successfully", data);
        } else {
            // List with optional filters, pagination, sorting
            List<Map<String, Object>> dataList = repository.findAll(
                    definition,
                    projectionColumns,
                    request.getFilters(),
                    request.getSortBy(),
                    request.getSortDirection(),
                    request.getPage(),
                    request.getSize());

            Long totalCount = null;
            if (request.getSize() != null && request.getSize() > 0) {
                totalCount = repository.count(definition, request.getFilters());
            }

            auditService.auditQuery(definition.getEntityType(), null,
                    request.getFilters(), request.getProjectionType(),
                    request.getSortBy(), request.getSortDirection(),
                    request.getPage(), request.getSize());

            return CrudResponse.successList(
                    "Entities retrieved successfully",
                    dataList,
                    totalCount,
                    request.getPage(),
                    request.getSize());
        }
    }

    // =========================================================================
    // CREATE
    // =========================================================================

    @SuppressWarnings({"unchecked", "rawtypes"})
    private CrudResponse handleCreate(EntityDefinition<?> definition, CrudRequest request) {
        Map<String, Object> payload = request.getPayload();

        // Validate payload
        ((ValidationService) validationService).validate(
                (EntityDefinition) definition, CrudOperation.CREATE, payload);

        // Uniqueness check — reject if a duplicate exists for any defined constraint
        checkUniqueness(definition, payload);

        // Publish PRE event
        eventPublisher.publishPre(definition.getEntityType(), CrudOperation.CREATE, null, payload);

        // beforeSave hook
        definition.beforeSave(CrudOperation.CREATE, payload);

        // Execute INSERT
        Map<String, Object> created = repository.insert(definition, payload);

        // afterSave hook
        Object createdId = created.get(definition.getIdField());
        if (createdId == null) {
            createdId = created.get(definition.getIdColumn());
        }
        definition.afterSave(CrudOperation.CREATE, createdId, payload);

        // Publish POST event
        eventPublisher.publishPost(definition.getEntityType(), CrudOperation.CREATE, createdId, payload);

        // Audit
        auditService.audit(definition.getEntityType(), CrudOperation.CREATE, createdId, payload);

        return CrudResponse.success("Entity created successfully", created);
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    @SuppressWarnings({"unchecked", "rawtypes"})
    private CrudResponse handleUpdate(EntityDefinition<?> definition, CrudRequest request) {
        requireId(request, "UPDATE");
        Map<String, Object> payload = request.getPayload();

        // Validate payload
        ((ValidationService) validationService).validate(
                (EntityDefinition) definition, CrudOperation.UPDATE, payload);

        // Publish PRE event
        eventPublisher.publishPre(definition.getEntityType(), CrudOperation.UPDATE,
                request.getId(), payload);

        // beforeSave hook
        definition.beforeSave(CrudOperation.UPDATE, payload);

        // Execute UPDATE
        Map<String, Object> updated = repository.update(definition, request.getId(), payload);

        // afterSave hook
        definition.afterSave(CrudOperation.UPDATE, request.getId(), payload);

        // Publish POST event
        eventPublisher.publishPost(definition.getEntityType(), CrudOperation.UPDATE,
                request.getId(), payload);

        // Audit
        auditService.audit(definition.getEntityType(), CrudOperation.UPDATE, request.getId(), payload);

        return CrudResponse.success("Entity updated successfully", updated);
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    private CrudResponse handleDelete(EntityDefinition<?> definition, CrudRequest request) {
        requireId(request, "DELETE");

        // Publish PRE event
        eventPublisher.publishPre(definition.getEntityType(), CrudOperation.DELETE,
                request.getId(), null);

        // Execute DELETE
        repository.delete(definition, request.getId());

        // afterSave hook
        definition.afterSave(CrudOperation.DELETE, request.getId(), null);

        // Publish POST event
        eventPublisher.publishPost(definition.getEntityType(), CrudOperation.DELETE,
                request.getId(), null);

        // Audit
        auditService.audit(definition.getEntityType(), CrudOperation.DELETE, request.getId(), null);

        return CrudResponse.success("Entity deleted successfully");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void validateRequest(CrudRequest request) {
        if (request.getEntityType() == null || request.getEntityType().isBlank()) {
            throw new IllegalArgumentException("entityType is required");
        }
        if (request.getOperation() == null) {
            throw new IllegalArgumentException("operation is required");
        }
    }

    private void requireId(CrudRequest request, String operationName) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("id is required for " + operationName + " operation");
        }
    }

    /**
     * Iterate over the entity's unique constraints and verify that no existing
     * record matches the constraint field values in the payload.
     */
    private void checkUniqueness(EntityDefinition<?> definition, Map<String, Object> payload) {
        List<UniqueConstraint> constraints = definition.getUniqueConstraints();
        if (constraints == null || constraints.isEmpty()) {
            return;
        }

        for (UniqueConstraint constraint : constraints) {
            Map<String, Object> fieldValues = new LinkedHashMap<>();
            boolean allPresent = true;

            for (String fieldName : constraint.getFieldNames()) {
                Object value = payload.get(fieldName);
                if (value == null) {
                    // If a constraint field is not in the payload, skip this constraint
                    allPresent = false;
                    break;
                }
                fieldValues.put(fieldName, value);
            }

            if (!allPresent) {
                continue;
            }

            if (repository.existsByColumns(definition, fieldValues)) {
                log.warn("Duplicate detected for entity '{}' on constraint fields {}: {}",
                        definition.getEntityType(), constraint.getFieldNames(), fieldValues);
                throw new DuplicateEntityException(
                        definition.getEntityType(),
                        constraint.getFieldNames(),
                        fieldValues,
                        constraint.getEffectiveMessage());
            }
        }
    }

    /**
     * Resolve projection type to a list of column names.
     * Returns null to indicate "all columns".
     */
    private List<String> resolveProjectionColumns(EntityDefinition<?> definition,
                                                   String projectionType) {
        if (projectionType == null || projectionType.isBlank()) {
            return null; // all columns
        }

        Map<String, List<String>> projections = definition.getProjectionTypes();
        if (projections.containsKey(projectionType)) {
            return projections.get(projectionType);
        }

        log.warn("Unknown projectionType '{}' for entity '{}', falling back to all columns",
                projectionType, definition.getEntityType());
        return null;
    }
}
