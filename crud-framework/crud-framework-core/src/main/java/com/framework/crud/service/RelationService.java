package com.framework.crud.service;

import com.framework.crud.definition.EntityDefinition;
import com.framework.crud.model.CrudOperation;
import com.framework.crud.model.CrudResponse;
import com.framework.crud.model.ManyToManyRelation;
import com.framework.crud.model.RelationRequest;
import com.framework.crud.registry.EntityRegistry;
import com.framework.crud.repository.DynamicCrudRepository;
import com.framework.crud.security.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Service responsible for managing many-to-many relationships
 * through junction tables.
 * <p>
 * Endpoint: {@code POST /api/crud/relation}
 * <p>
 * Supported operations:
 * <ul>
 *   <li><b>GET</b> (default) — query related entities via JOIN</li>
 *   <li><b>ADD</b> — insert association rows into the junction table</li>
 *   <li><b>REMOVE</b> — delete association rows from the junction table</li>
 * </ul>
 * <p>
 * The service:
 * <ol>
 *   <li>Resolves the source {@link EntityDefinition} from the request.</li>
 *   <li>Finds the matching {@link ManyToManyRelation} by name.</li>
 *   <li>Resolves the target {@link EntityDefinition}.</li>
 *   <li>Checks appropriate permissions on both source and target entities.</li>
 *   <li>Delegates to {@link DynamicCrudRepository} for the operation.</li>
 * </ol>
 */
@Service
public class RelationService {

    private static final Logger log = LoggerFactory.getLogger(RelationService.class);

    private final EntityRegistry entityRegistry;
    private final PermissionService permissionService;
    private final DynamicCrudRepository repository;
    private final AuditService auditService;

    public RelationService(EntityRegistry entityRegistry,
                           PermissionService permissionService,
                           DynamicCrudRepository repository,
                           AuditService auditService) {
        this.entityRegistry = entityRegistry;
        this.permissionService = permissionService;
        this.repository = repository;
        this.auditService = auditService;
    }

    /**
     * Process a many-to-many relation request (GET, ADD, or REMOVE).
     */
    @Transactional
    public CrudResponse processRelation(RelationRequest request) {
        // 1. Validate required fields
        if (request.getEntityType() == null || request.getEntityType().isBlank()) {
            throw new IllegalArgumentException("entityType is required");
        }
        if (request.getRelationName() == null || request.getRelationName().isBlank()) {
            throw new IllegalArgumentException("relationName is required");
        }
        if (request.getId() == null) {
            throw new IllegalArgumentException("id is required (source entity primary key)");
        }

        // 2. Resolve source entity
        EntityDefinition<?> sourceDef = entityRegistry.getDefinition(request.getEntityType());

        // 3. Find matching relation
        ManyToManyRelation relation = sourceDef.getManyToManyRelations().stream()
                .filter(r -> r.getRelationName().equalsIgnoreCase(request.getRelationName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Relation '%s' is not defined on entity '%s'. Available relations: %s",
                                request.getRelationName(),
                                sourceDef.getEntityType(),
                                sourceDef.getManyToManyRelations().stream()
                                        .map(ManyToManyRelation::getRelationName)
                                        .toList())));

        // 4. Resolve target entity
        EntityDefinition<?> targetDef = entityRegistry.getDefinition(relation.getTargetEntityType());

        // 5. Route by operation
        String op = (request.getOperation() == null || request.getOperation().isBlank())
                ? "GET" : request.getOperation().toUpperCase();

        return switch (op) {
            case "GET" -> processGet(request, sourceDef, targetDef, relation);
            case "ADD" -> processAdd(request, sourceDef, targetDef, relation);
            case "REMOVE" -> processRemove(request, sourceDef, targetDef, relation);
            default -> throw new IllegalArgumentException(
                    "Invalid relation operation: '" + request.getOperation()
                            + "'. Allowed: GET, ADD, REMOVE");
        };
    }

    // =========================================================================
    // GET — query related entities
    // =========================================================================

    private CrudResponse processGet(RelationRequest request,
                                     EntityDefinition<?> sourceDef,
                                     EntityDefinition<?> targetDef,
                                     ManyToManyRelation relation) {
        // Permission: user must have GET on both source and target
        permissionService.checkAccess(sourceDef, CrudOperation.GET);
        permissionService.checkAccess(targetDef, CrudOperation.GET);

        log.info("Relation GET: {} [id={}] —({})—> {} via {}",
                sourceDef.getEntityType(), request.getId(),
                relation.getRelationName(), targetDef.getEntityType(),
                relation.getJunctionTable());

        List<String> projectionColumns = resolveProjectionColumns(targetDef, request.getProjectionType());

        List<Map<String, Object>> dataList = repository.findByRelation(
                targetDef, relation, request.getId(),
                request.getFilters(), projectionColumns,
                request.getSortBy(), request.getSortDirection(),
                request.getPage(), request.getSize());

        Long totalCount = null;
        if (request.getSize() != null && request.getSize() > 0) {
            totalCount = repository.countByRelation(targetDef, relation, request.getId(), request.getFilters());
        }

        auditService.audit(sourceDef.getEntityType(), CrudOperation.GET, request.getId(),
                Map.of("relation", relation.getRelationName(),
                       "targetEntity", targetDef.getEntityType()));

        return CrudResponse.successList(
                String.format("Related '%s' entities retrieved successfully via '%s'",
                        targetDef.getEntityType(), relation.getRelationName()),
                dataList, totalCount, request.getPage(), request.getSize());
    }

    // =========================================================================
    // ADD — insert junction-table rows (create associations)
    // =========================================================================

    private CrudResponse processAdd(RelationRequest request,
                                     EntityDefinition<?> sourceDef,
                                     EntityDefinition<?> targetDef,
                                     ManyToManyRelation relation) {
        validateRelatedIds(request);

        // Permission: user must have CREATE on the source entity
        permissionService.checkAccess(sourceDef, CrudOperation.CREATE);

        log.info("Relation ADD: {} [id={}] —({})—> {} relatedIds={} via {}",
                sourceDef.getEntityType(), request.getId(),
                relation.getRelationName(), targetDef.getEntityType(),
                request.getRelatedIds(), relation.getJunctionTable());

        int count = repository.addRelations(relation, request.getId(), request.getRelatedIds());

        auditService.audit(sourceDef.getEntityType(), CrudOperation.CREATE, request.getId(),
                Map.of("relation", relation.getRelationName(),
                       "targetEntity", targetDef.getEntityType(),
                       "relatedIds", request.getRelatedIds().toString(),
                       "insertedCount", String.valueOf(count)));

        return CrudResponse.success(
                String.format("%d association(s) added between '%s' [id=%s] and '%s' %s via '%s'",
                        count, sourceDef.getEntityType(), request.getId(),
                        targetDef.getEntityType(), request.getRelatedIds(),
                        relation.getRelationName()));
    }

    // =========================================================================
    // REMOVE — delete junction-table rows (remove associations)
    // =========================================================================

    private CrudResponse processRemove(RelationRequest request,
                                        EntityDefinition<?> sourceDef,
                                        EntityDefinition<?> targetDef,
                                        ManyToManyRelation relation) {
        validateRelatedIds(request);

        // Permission: user must have DELETE on the source entity
        permissionService.checkAccess(sourceDef, CrudOperation.DELETE);

        log.info("Relation REMOVE: {} [id={}] —({})—> {} relatedIds={} via {}",
                sourceDef.getEntityType(), request.getId(),
                relation.getRelationName(), targetDef.getEntityType(),
                request.getRelatedIds(), relation.getJunctionTable());

        int count = repository.removeRelations(relation, request.getId(), request.getRelatedIds());

        auditService.audit(sourceDef.getEntityType(), CrudOperation.DELETE, request.getId(),
                Map.of("relation", relation.getRelationName(),
                       "targetEntity", targetDef.getEntityType(),
                       "relatedIds", request.getRelatedIds().toString(),
                       "deletedCount", String.valueOf(count)));

        return CrudResponse.success(
                String.format("%d association(s) removed between '%s' [id=%s] and '%s' %s via '%s'",
                        count, sourceDef.getEntityType(), request.getId(),
                        targetDef.getEntityType(), request.getRelatedIds(),
                        relation.getRelationName()));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void validateRelatedIds(RelationRequest request) {
        if (request.getRelatedIds() == null || request.getRelatedIds().isEmpty()) {
            throw new IllegalArgumentException(
                    "relatedIds is required for " + request.getOperation().toUpperCase()
                            + " operation (list of target entity primary keys)");
        }
    }

    /**
     * Resolve projection type to a list of column names on the target entity.
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

        log.warn("Unknown projectionType '{}' for target entity '{}', falling back to all columns",
                projectionType, definition.getEntityType());
        return null;
    }
}
