package com.framework.crud.service;

import com.framework.crud.model.CrudOperation;
import com.framework.crud.security.CrudSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Comprehensive audit logging service.
 * <p>
 * Captures every operation performed through the framework with full context:
 * <ul>
 *   <li><b>Entity CRUD</b> — CREATE / UPDATE / DELETE with payload details</li>
 *   <li><b>Entity queries</b> — GET with filters, projection, sort, pagination</li>
 *   <li><b>Relation queries</b> — M:N GET with relation context, filters, projection</li>
 *   <li><b>Relation mutations</b> — M:N ADD / REMOVE with affected IDs and counts</li>
 * </ul>
 * <p>
 * Replace this bean with your own implementation to write audit records
 * to a database table, message queue, or external audit system.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final CrudSecurityContext securityContext;

    public AuditService(CrudSecurityContext securityContext) {
        this.securityContext = securityContext;
    }

    // =========================================================================
    // Entity CRUD — write operations (CREATE / UPDATE / DELETE)
    // =========================================================================

    /**
     * Audit a write operation (CREATE, UPDATE, DELETE) on a single entity.
     *
     * @param entityType the entity type
     * @param operation  the CRUD operation performed
     * @param id         the entity primary key (null for CREATE before ID is assigned)
     * @param payload    the request payload (null for DELETE)
     */
    public void audit(String entityType, CrudOperation operation, Object id,
                      Map<String, Object> payload) {
        String username = securityContext.getCurrentUsername();
        log.info("AUDIT | user={} | entity={} | operation={} | id={} | payloadKeys={}",
                username,
                entityType,
                operation,
                id,
                payload != null ? payload.keySet() : "N/A");
    }

    // =========================================================================
    // Entity CRUD — read / query operations (GET)
    // =========================================================================

    /**
     * Audit a query (GET) operation with full query context including
     * filters, projection type, sorting, and pagination.
     *
     * @param entityType     the entity type being queried
     * @param id             the entity PK (non-null for single-entity fetch, null for list)
     * @param filters        applied filter criteria (may be null or empty)
     * @param projectionType the named projection requested (may be null for all columns)
     * @param sortBy         sort field (may be null)
     * @param sortDirection  ASC or DESC (may be null)
     * @param page           page number (may be null)
     * @param size           page size (may be null)
     */
    public void auditQuery(String entityType, Object id,
                           Map<String, Object> filters, String projectionType,
                           String sortBy, String sortDirection,
                           Integer page, Integer size) {
        String username = securityContext.getCurrentUsername();
        log.info("AUDIT | user={} | entity={} | operation=GET | id={} | filters={} | projection={} | sort={}:{} | page={} | size={}",
                username,
                entityType,
                id != null ? id : "ALL",
                filters != null && !filters.isEmpty() ? filters : "none",
                projectionType != null ? projectionType : "all",
                sortBy != null ? sortBy : "default",
                sortDirection != null ? sortDirection : "ASC",
                page != null ? page : "N/A",
                size != null ? size : "N/A");
    }

    // =========================================================================
    // Relation — read / query operations (M:N GET)
    // =========================================================================

    /**
     * Audit a many-to-many relation GET query with full context.
     *
     * @param sourceEntity   the source entity type
     * @param relationName   the logical relation name
     * @param targetEntity   the target (related) entity type
     * @param sourceId       the source entity PK
     * @param filters        applied filters on the target entity (may be null)
     * @param projectionType named projection on the target entity (may be null)
     * @param sortBy         sort field on target entity (may be null)
     * @param sortDirection  ASC or DESC (may be null)
     * @param page           page number (may be null)
     * @param size           page size (may be null)
     */
    public void auditRelationQuery(String sourceEntity, String relationName,
                                   String targetEntity, Object sourceId,
                                   Map<String, Object> filters, String projectionType,
                                   String sortBy, String sortDirection,
                                   Integer page, Integer size) {
        String username = securityContext.getCurrentUsername();
        log.info("AUDIT | user={} | operation=RELATION_GET | source={}[id={}] | relation={} | target={} | filters={} | projection={} | sort={}:{} | page={} | size={}",
                username,
                sourceEntity, sourceId,
                relationName,
                targetEntity,
                filters != null && !filters.isEmpty() ? filters : "none",
                projectionType != null ? projectionType : "all",
                sortBy != null ? sortBy : "default",
                sortDirection != null ? sortDirection : "ASC",
                page != null ? page : "N/A",
                size != null ? size : "N/A");
    }

    // =========================================================================
    // Relation — write operations (M:N ADD / REMOVE)
    // =========================================================================

    /**
     * Audit a many-to-many relation mutation (ADD or REMOVE).
     *
     * @param sourceEntity  the source entity type
     * @param relationName  the logical relation name
     * @param targetEntity  the target (related) entity type
     * @param sourceId      the source entity PK
     * @param relatedIds    the list of target entity PKs that were added/removed
     * @param mutationType  "ADD" or "REMOVE"
     * @param affectedCount the number of rows actually inserted or deleted
     */
    public void auditRelationMutation(String sourceEntity, String relationName,
                                      String targetEntity, Object sourceId,
                                      List<Object> relatedIds, String mutationType,
                                      int affectedCount) {
        String username = securityContext.getCurrentUsername();
        log.info("AUDIT | user={} | operation=RELATION_{} | source={}[id={}] | relation={} | target={} | relatedIds={} | affectedCount={}",
                username,
                mutationType,
                sourceEntity, sourceId,
                relationName,
                targetEntity,
                relatedIds,
                affectedCount);
    }
}
