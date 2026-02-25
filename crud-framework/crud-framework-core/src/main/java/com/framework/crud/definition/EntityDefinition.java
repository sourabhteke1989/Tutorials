package com.framework.crud.definition;

import com.framework.crud.model.CrudOperation;
import com.framework.crud.model.FieldDefinition;
import com.framework.crud.model.IdType;
import com.framework.crud.model.ManyToManyRelation;
import com.framework.crud.model.PermissionConfig;
import com.framework.crud.model.UniqueConstraint;
import com.framework.crud.model.ValidationResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Core interface that a developer implements <b>once per entity</b>.
 * <p>
 * This is the only class a developer needs to write. The framework uses the
 * metadata returned by its methods to:
 * <ul>
 *   <li>Map the logical {@code entityType} to a database table</li>
 *   <li>Determine which columns exist, which are required/optional</li>
 *   <li>Resolve named projection types to column lists</li>
 *   <li>Run built-in structural validation + custom validation logic</li>
 *   <li>Check required permissions per operation</li>
 * </ul>
 *
 * @param <T> The entity POJO class (e.g. {@code Product}, {@code Customer}).
 *            The framework converts the incoming payload map into this class
 *            before calling {@link #validate(CrudOperation, Object)}.
 */
public interface EntityDefinition<T> {

    // =========================================================================
    // Required methods
    // =========================================================================

    /**
     * Logical entity type string used in {@code CrudRequest.entityType}.
     * Must be unique across the application.
     * Example: {@code "product"}, {@code "customer"}, {@code "order"}
     */
    String getEntityType();

    /**
     * Physical database table name this entity maps to.
     * Example: {@code "products"}, {@code "customers"}
     */
    String getTableName();

    /**
     * The POJO class representing this entity.
     * The framework uses Jackson ObjectMapper to convert the payload map into this class.
     */
    Class<T> getEntityClass();

    /**
     * Full list of field definitions for this entity.
     * Each field maps a JSON payload key to a database column with metadata
     * (required, max-length, pattern, insertable, updatable, etc.).
     */
    List<FieldDefinition> getFieldDefinitions();

    // =========================================================================
    // Optional methods with defaults
    // =========================================================================

    /**
     * Name of the primary key column. Default: {@code "id"}.
     */
    default String getIdColumn() {
        return "id";
    }

    /**
     * Name of the primary key field in the JSON payload. Default: {@code "id"}.
     */
    default String getIdField() {
        return "id";
    }

    /**
     * Primary key generation strategy. Default: {@link IdType#AUTO_INCREMENT}.
     * <p>
     * Override to use UUID-based IDs:
     * <pre>
     * &#64;Override
     * public IdType getIdType() {
     *     return IdType.UUID;
     * }
     * </pre>
     * When using {@link IdType#UUID}, the ID column should be {@code VARCHAR(36)}
     * or a native UUID type in the database. The framework generates a random UUID (v4)
     * before INSERT and includes it in the payload automatically.
     *
     * @return the ID generation strategy for this entity.
     */
    default IdType getIdType() {
        return IdType.AUTO_INCREMENT;
    }

    /**
     * Whether this entity supports soft delete.
     * If true, DELETE will set a flag column instead of physically removing the row.
     * The column name is determined by {@link #getSoftDeleteColumn()}.
     */
    default boolean isSoftDeleteEnabled() {
        return false;
    }

    /**
     * Column name used for soft delete flag. Default: {@code "deleted"}.
     * Only used when {@link #isSoftDeleteEnabled()} returns true.
     */
    default String getSoftDeleteColumn() {
        return "deleted";
    }

    /**
     * Named projection types and their associated column lists.
     * <p>
     * Example:
     * <pre>
     * Map.of(
     *   "summary", List.of("id", "name", "status"),
     *   "detail",  List.of("id", "name", "description", "price", "status", "created_at")
     * )
     * </pre>
     * If the projection type in the request is {@code null} or not found here,
     * all columns from field definitions are returned.
     */
    default Map<String, List<String>> getProjectionTypes() {
        return Collections.emptyMap();
    }

    /**
     * Comprehensive permission configuration for this entity.
     * <p>
     * Defines named permissions for each operation (LIST, GET, CREATE, UPDATE, DELETE)
     * plus optional filter-based permissions for fine-grained access control on LIST
     * operations.
     * <p>
     * A GET operation without an ID is treated as a <b>LIST</b> operation and requires
     * the {@code listPermission}. A GET with an ID requires the {@code getPermission}.
     * <p>
     * Example:
     * <pre>
     * return PermissionConfig.builder()
     *     .listPermission("ListProduct")
     *     .getPermission("GetProduct")
     *     .createPermission("CreateProduct")
     *     .updatePermission("UpdateProduct")
     *     .deletePermission("DeleteProduct")
     *     .build();
     * </pre>
     *
     * @see PermissionConfig
     * @see com.framework.crud.model.FilterPermission
     */
    default PermissionConfig getPermissionConfig() {
        return PermissionConfig.empty();
    }

    /**
     * Set of operations allowed on this entity. Default: all operations.
     * <p>
     * Override to restrict. For example, a read-only entity:
     * <pre>
     * return Set.of(CrudOperation.GET);
     * </pre>
     */
    default Set<CrudOperation> getAllowedOperations() {
        return Set.of(CrudOperation.values());
    }

    /**
     * Uniqueness constraints checked <b>before</b> CREATE operations.
     * <p>
     * Each {@link UniqueConstraint} specifies one or more field names whose
     * combined values must be unique across existing records. The framework
     * queries the database and rejects the CREATE if a duplicate is found.
     * <p>
     * Examples:
     * <pre>
     * // Single-field uniqueness (email must be unique)
     * return List.of(UniqueConstraint.of("email"));
     *
     * // Composite uniqueness (name + category together must be unique)
     * return List.of(
     *     UniqueConstraint.of("name", "category")
     *         .withMessage("A product with this name already exists in this category")
     * );
     *
     * // Multiple constraints
     * return List.of(
     *     UniqueConstraint.of("email"),
     *     UniqueConstraint.of("firstName", "lastName", "phone")
     * );
     * </pre>
     *
     * @return list of uniqueness constraints; empty list means no uniqueness checks.
     */
    default List<UniqueConstraint> getUniqueConstraints() {
        return Collections.emptyList();
    }

    /**
     * Many-to-many relationships this entity participates in as the <b>source</b> side.
     * <p>
     * Each {@link ManyToManyRelation} defines a junction table and the target entity,
     * enabling the {@code POST /api/crud/relation} endpoint to query related entities
     * via a SQL JOIN.
     * <p>
     * Example — Products that have Tags via a product_tags junction table:
     * <pre>
     * return List.of(
     *     ManyToManyRelation.builder()
     *         .relationName("tags")
     *         .targetEntityType("tag")
     *         .junctionTable("product_tags")
     *         .sourceJoinColumn("product_id")
     *         .targetJoinColumn("tag_id")
     *         .build()
     * );
     * </pre>
     *
     * @return list of many-to-many relations; empty list means none.
     */
    default List<ManyToManyRelation> getManyToManyRelations() {
        return Collections.emptyList();
    }

    /**
     * Custom validation logic executed <b>after</b> the built-in structural validation.
     * <p>
     * The framework converts the request payload to the entity class {@code T}
     * and passes it here so the developer can run business-rule checks.
     *
     * @param operation The current operation (CREATE or UPDATE).
     * @param entity    The payload converted to the entity POJO.
     * @return A {@link ValidationResult}. Return {@code ValidationResult.success()} if valid.
     */
    default ValidationResult validate(CrudOperation operation, T entity) {
        return ValidationResult.success();
    }

    /**
     * Optional hook called <b>before</b> persisting a CREATE or UPDATE.
     * Allows the developer to enrich or transform the payload map
     * (e.g., set audit fields, compute defaults).
     *
     * @param operation The current operation.
     * @param payload   Mutable payload map that will be persisted.
     */
    default void beforeSave(CrudOperation operation, Map<String, Object> payload) {
        // no-op by default
    }

    /**
     * Optional hook called <b>after</b> a successful CREATE, UPDATE, or DELETE.
     *
     * @param operation The operation that was performed.
     * @param id        The entity's primary key.
     * @param payload   The payload map (null for DELETE).
     */
    default void afterSave(CrudOperation operation, Object id, Map<String, Object> payload) {
        // no-op by default
    }
}
