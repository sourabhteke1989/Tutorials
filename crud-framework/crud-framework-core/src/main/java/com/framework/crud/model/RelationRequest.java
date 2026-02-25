package com.framework.crud.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.List;
import java.util.Map;


/**
 * Request payload for the relation endpoint ({@code POST /api/crud/relation}).
 * <p>
 * Supports three operations on many-to-many relations:
 * <ul>
 *   <li><b>GET</b> (default) — query related entities through a junction table</li>
 *   <li><b>ADD</b> — create new associations by inserting rows into the junction table</li>
 *   <li><b>REMOVE</b> — remove associations by deleting rows from the junction table</li>
 * </ul>
 *
 * <b>GET example:</b>
 * <pre>
 * {
 *   "entityType": "product",
 *   "relationName": "tags",
 *   "id": 1,
 *   "filters": { "color": "#3498DB" },
 *   "projectionType": "summary",
 *   "page": 0,
 *   "size": 20,
 *   "sortBy": "name",
 *   "sortDirection": "ASC"
 * }
 * </pre>
 *
 * <b>ADD / REMOVE example:</b>
 * <pre>
 * {
 *   "entityType": "product",
 *   "relationName": "tags",
 *   "id": 1,
 *   "operation": "ADD",
 *   "relatedIds": [3, 5, 7]
 * }
 * </pre>
 */
public class RelationRequest {

    /** The source entity type (e.g. "product"). Must be a registered entity. */
    private String entityType;

    /** The relation name as defined in EntityDefinition.getManyToManyRelations() (e.g. "tags"). */
    private String relationName;

    /** The source entity's primary key value. */
    private Object id;

    /**
     * Operation to perform: GET (default), ADD, or REMOVE.
     * <ul>
     *   <li>GET — query related entities (supports filters, projections, pagination, sorting)</li>
     *   <li>ADD — insert association(s) into the junction table</li>
     *   <li>REMOVE — delete association(s) from the junction table</li>
     * </ul>
     */
    private RelationOperation operation;

    /**
     * List of target entity primary keys to add or remove associations for.
     * Required when operation is ADD or REMOVE.
     */
    private List<Object> relatedIds;

    /** Page number (0-based) for paginated results. */
    private Integer page;

    /** Page size for paginated results. */
    private Integer size;

    /** Column/field name to sort by (from the target entity). */
    private String sortBy;

    /** Sort direction: ASC or DESC. Default ASC. */
    private String sortDirection;

    /**
     * Optional filters applied to the target entity columns.
     * Works exactly like the {@code filters} map in a regular GET request —
     * only filterable columns defined in the target EntityDefinition are accepted.
     */
    private Map<String, Object> filters;

    /**
     * Optional projection type to limit which columns of the target entity are returned.
     * Must match a key in the target entity's {@code getProjectionTypes()} map.
     * When {@code null}, all columns are returned.
     */
    private String projectionType;

    // ---- Constructors ----

    public RelationRequest() {
    }

    // ---- Getters & Setters ----

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getRelationName() {
        return relationName;
    }

    public void setRelationName(String relationName) {
        this.relationName = relationName;
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public RelationOperation getOperation() {
        return operation;
    }

    @JsonIgnore
    public void setOperation(RelationOperation operation) {
        this.operation = operation;
    }

    /**
     * Accept a string value for the operation field (used during JSON deserialization).
     * Parses the string to {@link RelationOperation} via {@link RelationOperation#fromString(String)}.
     * Returns {@link RelationOperation#GET} when null or blank.
     */
    @JsonSetter("operation")
    public void setOperation(String operation) {
        this.operation = RelationOperation.fromString(operation);
    }

    public List<Object> getRelatedIds() {
        return relatedIds;
    }

    public void setRelatedIds(List<Object> relatedIds) {
        this.relatedIds = relatedIds;
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

    public Map<String, Object> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }

    public String getProjectionType() {
        return projectionType;
    }

    public void setProjectionType(String projectionType) {
        this.projectionType = projectionType;
    }
}
