package com.framework.crud.model;

import java.util.Map;

/**
 * Generic CRUD request payload.
 * <p>
 * This is the single request object used for all CRUD operations.
 * The framework routes, validates, and processes based on the fields provided.
 * </p>
 *
 * <pre>
 * {
 *   "entityType": "product",
 *   "operation": "GET",
 *   "id": 1,
 *   "projectionType": "summary",
 *   "payload": { "name": "Widget", "price": 9.99 },
 *   "filters": { "status": "active" },
 *   "page": 0,
 *   "size": 20,
 *   "sortBy": "name",
 *   "sortDirection": "ASC"
 * }
 * </pre>
 */
public class CrudRequest {

    /** The logical entity type (e.g. "product", "customer"). Maps to a table via EntityDefinition. */
    private String entityType;

    /** The CRUD operation: GET, CREATE, UPDATE, DELETE. */
    private String operation;

    /** Primary key value. Required for GET (single), UPDATE, DELETE. */
    private Object id;

    /** Projection type determining which columns to fetch. If null, all columns are returned. */
    private String projectionType;

    /** Payload map used for CREATE and UPDATE — column values to insert/update. */
    private Map<String, Object> payload;

    /** Simple equality filters for GET (list) operations. Column name → value. */
    private Map<String, Object> filters;

    /** Page number for paginated GET (0-based). */
    private Integer page;

    /** Page size for paginated GET. */
    private Integer size;

    /** Column name to sort by. */
    private String sortBy;

    /** Sort direction: ASC or DESC. Default ASC. */
    private String sortDirection;

    // ---- Constructors ----

    public CrudRequest() {
    }

    // ---- Getters & Setters ----

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public String getProjectionType() {
        return projectionType;
    }

    public void setProjectionType(String projectionType) {
        this.projectionType = projectionType;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public Map<String, Object> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }

    /**
     * Resolve the CrudOperation enum from the string operation field.
     */
    public CrudOperation resolveOperation() {
        return CrudOperation.fromString(this.operation);
    }
}
