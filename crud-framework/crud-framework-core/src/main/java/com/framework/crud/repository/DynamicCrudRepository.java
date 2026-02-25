package com.framework.crud.repository;

import com.framework.crud.definition.EntityDefinition;
import com.framework.crud.exception.EntityNotFoundException;
import com.framework.crud.model.FieldDefinition;
import com.framework.crud.model.IdType;
import com.framework.crud.model.ManyToManyRelation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Dynamic SQL executor that builds and runs parameterized queries
 * based on {@link EntityDefinition} metadata.
 * <p>
 * All values are bound via named parameters — <b>never</b> concatenated
 * into SQL strings — to prevent SQL injection.
 */
@Repository
public class DynamicCrudRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamicCrudRepository.class);

    private final NamedParameterJdbcTemplate jdbc;

    public DynamicCrudRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // =========================================================================
    // GET by ID
    // =========================================================================

    public Map<String, Object> findById(EntityDefinition<?> def, Object id, List<String> columns) {
        String selectCols = buildSelectColumns(def, columns);
        String sql = String.format("SELECT %s FROM %s WHERE %s = :id",
                selectCols, sanitizeIdentifier(def.getTableName()), sanitizeIdentifier(def.getIdColumn()));

        if (def.isSoftDeleteEnabled()) {
            sql += String.format(" AND %s = false", sanitizeIdentifier(def.getSoftDeleteColumn()));
        }

        log.debug("findById SQL: {}", sql);

        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        List<Map<String, Object>> results = jdbc.queryForList(sql, params);

        if (results.isEmpty()) {
            throw new EntityNotFoundException(def.getEntityType(), id);
        }
        return results.get(0);
    }

    // =========================================================================
    // GET list (with filters, pagination, sorting)
    // =========================================================================

    public List<Map<String, Object>> findAll(EntityDefinition<?> def, List<String> columns,
                                              Map<String, Object> filters, String sortBy,
                                              String sortDirection, Integer page, Integer size) {
        String selectCols = buildSelectColumns(def, columns);
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(selectCols)
           .append(" FROM ").append(sanitizeIdentifier(def.getTableName()));

        MapSqlParameterSource params = new MapSqlParameterSource();

        // WHERE clause
        List<String> conditions = buildWhereConditions(def, filters, params);
        if (def.isSoftDeleteEnabled()) {
            conditions.add(sanitizeIdentifier(def.getSoftDeleteColumn()) + " = false");
        }
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        // ORDER BY
        if (sortBy != null && !sortBy.isBlank()) {
            String colName = resolveColumnName(def, sortBy);
            if (colName != null) {
                String dir = "DESC".equalsIgnoreCase(sortDirection) ? "DESC" : "ASC";
                sql.append(" ORDER BY ").append(sanitizeIdentifier(colName)).append(" ").append(dir);
            }
        }

        // PAGINATION (LIMIT / OFFSET)
        if (size != null && size > 0) {
            int pageNum = (page != null && page >= 0) ? page : 0;
            sql.append(" LIMIT :limit OFFSET :offset");
            params.addValue("limit", size);
            params.addValue("offset", pageNum * size);
        }

        log.debug("findAll SQL: {}", sql);
        return jdbc.queryForList(sql.toString(), params);
    }

    /**
     * Count total records matching the filters (for pagination metadata).
     */
    public long count(EntityDefinition<?> def, Map<String, Object> filters) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM ").append(sanitizeIdentifier(def.getTableName()));

        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> conditions = buildWhereConditions(def, filters, params);
        if (def.isSoftDeleteEnabled()) {
            conditions.add(sanitizeIdentifier(def.getSoftDeleteColumn()) + " = false");
        }
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        log.debug("count SQL: {}", sql);
        Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
        return count != null ? count : 0L;
    }

    // =========================================================================
    // CREATE
    // =========================================================================

    public Map<String, Object> insert(EntityDefinition<?> def, Map<String, Object> payload) {
        boolean isUuid = def.getIdType() == IdType.UUID;
        java.util.UUID generatedUuid = null;

        // For UUID strategy, generate the ID before INSERT
        if (isUuid) {
            generatedUuid = java.util.UUID.randomUUID();
            payload.put(def.getIdField(), generatedUuid);
        }

        // Filter to only insertable fields
        Map<String, String> fieldToColumn = def.getFieldDefinitions().stream()
                .filter(FieldDefinition::isInsertable)
                .filter(f -> payload.containsKey(f.getFieldName()))
                .collect(Collectors.toMap(FieldDefinition::getFieldName, FieldDefinition::getColumnName,
                        (a, b) -> a, LinkedHashMap::new));

        // For UUID, explicitly include the ID column in the INSERT
        if (isUuid) {
            fieldToColumn.put(def.getIdField(), def.getIdColumn());
        }

        if (fieldToColumn.isEmpty()) {
            throw new IllegalArgumentException("No insertable fields provided in payload");
        }

        List<String> columns = new ArrayList<>(fieldToColumn.values());
        List<String> paramNames = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();

        for (Map.Entry<String, String> entry : fieldToColumn.entrySet()) {
            String paramName = "p_" + entry.getKey();
            paramNames.add(":" + paramName);
            params.addValue(paramName, payload.get(entry.getKey()));
        }

        // Apply default values for fields not in payload
        for (FieldDefinition fd : def.getFieldDefinitions()) {
            if (fd.isInsertable() && fd.getDefaultValue() != null
                    && !payload.containsKey(fd.getFieldName())) {
                columns.add(fd.getColumnName());
                String paramName = "p_" + fd.getFieldName();
                paramNames.add(":" + paramName);
                params.addValue(paramName, fd.getDefaultValue());
            }
        }

        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)",
                sanitizeIdentifier(def.getTableName()),
                columns.stream().map(this::sanitizeIdentifier).collect(Collectors.joining(", ")),
                String.join(", ", paramNames));

        log.debug("insert SQL: {}", sql);

        if (isUuid) {
            // UUID strategy: ID is already in the payload, no need for KeyHolder
            jdbc.update(sql, params);
            try {
                return findById(def, generatedUuid, null);
            } catch (EntityNotFoundException e) {
                Map<String, Object> result = new LinkedHashMap<>(payload);
                result.put(def.getIdField(), generatedUuid);
                return result;
            }
        } else {
            // AUTO_INCREMENT strategy: use KeyHolder to retrieve generated ID
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.update(sql, params, keyHolder);

            Object generatedId = null;
            if (keyHolder.getKeys() != null && !keyHolder.getKeys().isEmpty()) {
                generatedId = keyHolder.getKeys().values().iterator().next();
            } else if (keyHolder.getKey() != null) {
                generatedId = keyHolder.getKey();
            }

            if (generatedId != null) {
                try {
                    return findById(def, generatedId, null);
                } catch (EntityNotFoundException e) {
                    Map<String, Object> result = new LinkedHashMap<>(payload);
                    result.put(def.getIdField(), generatedId);
                    return result;
                }
            }

            return new LinkedHashMap<>(payload);
        }
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    public Map<String, Object> update(EntityDefinition<?> def, Object id, Map<String, Object> payload) {
        // Filter to only updatable fields present in payload
        Map<String, String> fieldToColumn = def.getFieldDefinitions().stream()
                .filter(FieldDefinition::isUpdatable)
                .filter(f -> payload.containsKey(f.getFieldName()))
                .collect(Collectors.toMap(FieldDefinition::getFieldName, FieldDefinition::getColumnName,
                        (a, b) -> a, LinkedHashMap::new));

        if (fieldToColumn.isEmpty()) {
            throw new IllegalArgumentException("No updatable fields provided in payload");
        }

        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        List<String> setClauses = new ArrayList<>();

        for (Map.Entry<String, String> entry : fieldToColumn.entrySet()) {
            String paramName = "p_" + entry.getKey();
            setClauses.add(sanitizeIdentifier(entry.getValue()) + " = :" + paramName);
            params.addValue(paramName, payload.get(entry.getKey()));
        }

        String sql = String.format("UPDATE %s SET %s WHERE %s = :id",
                sanitizeIdentifier(def.getTableName()),
                String.join(", ", setClauses),
                sanitizeIdentifier(def.getIdColumn()));

        if (def.isSoftDeleteEnabled()) {
            sql += String.format(" AND %s = false", sanitizeIdentifier(def.getSoftDeleteColumn()));
        }

        log.debug("update SQL: {}", sql);

        int rowsAffected = jdbc.update(sql, params);
        if (rowsAffected == 0) {
            throw new EntityNotFoundException(def.getEntityType(), id);
        }

        // Return the updated record
        return findById(def, id, null);
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    public void delete(EntityDefinition<?> def, Object id) {
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        String sql;

        if (def.isSoftDeleteEnabled()) {
            sql = String.format("UPDATE %s SET %s = true WHERE %s = :id AND %s = false",
                    sanitizeIdentifier(def.getTableName()),
                    sanitizeIdentifier(def.getSoftDeleteColumn()),
                    sanitizeIdentifier(def.getIdColumn()),
                    sanitizeIdentifier(def.getSoftDeleteColumn()));
        } else {
            sql = String.format("DELETE FROM %s WHERE %s = :id",
                    sanitizeIdentifier(def.getTableName()),
                    sanitizeIdentifier(def.getIdColumn()));
        }

        log.debug("delete SQL: {}", sql);

        int rowsAffected = jdbc.update(sql, params);
        if (rowsAffected == 0) {
            throw new EntityNotFoundException(def.getEntityType(), id);
        }
    }

    // =========================================================================
    // Uniqueness check
    // =========================================================================

    /**
     * Check whether a record already exists matching ALL the given field→value pairs.
     * Used by the framework to enforce {@link EntityDefinition#getUniqueConstraints()} before INSERT.
     *
     * @param def          Entity definition (for table name, field-to-column mapping, soft delete).
     * @param fieldValues  Map of fieldName → value pairs to check (all must match).
     * @return {@code true} if at least one matching record exists.
     */
    public boolean existsByColumns(EntityDefinition<?> def, Map<String, Object> fieldValues) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM ").append(sanitizeIdentifier(def.getTableName()));

        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();

        for (Map.Entry<String, Object> entry : fieldValues.entrySet()) {
            String colName = resolveColumnName(def, entry.getKey());
            if (colName == null) {
                throw new IllegalArgumentException(
                        "Unknown field '" + entry.getKey() + "' in unique constraint for entity '" + def.getEntityType() + "'");
            }
            String paramName = "uc_" + entry.getKey().replaceAll("[^a-zA-Z0-9_]", "");
            conditions.add(sanitizeIdentifier(colName) + " = :" + paramName);
            params.addValue(paramName, entry.getValue());
        }

        // Exclude soft-deleted records
        if (def.isSoftDeleteEnabled()) {
            conditions.add(sanitizeIdentifier(def.getSoftDeleteColumn()) + " = false");
        }

        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        log.debug("existsByColumns SQL: {}", sql);
        Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
        return count != null && count > 0;
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    /**
     * Build the SELECT column list. If columns is null/empty, select all defined fields.
     */
    private String buildSelectColumns(EntityDefinition<?> def, List<String> columns) {
        if (columns != null && !columns.isEmpty()) {
            // Validate that each column exists in field definitions
            Set<String> knownColumns = def.getFieldDefinitions().stream()
                    .map(FieldDefinition::getColumnName)
                    .collect(Collectors.toSet());
            // Also accept field names and map them
            Map<String, String> fieldToCol = def.getFieldDefinitions().stream()
                    .collect(Collectors.toMap(FieldDefinition::getFieldName, FieldDefinition::getColumnName));

            List<String> resolvedColumns = new ArrayList<>();
            for (String col : columns) {
                if (knownColumns.contains(col)) {
                    resolvedColumns.add(sanitizeIdentifier(col));
                } else if (fieldToCol.containsKey(col)) {
                    resolvedColumns.add(sanitizeIdentifier(fieldToCol.get(col)));
                }
                // Unknown columns are silently ignored for safety
            }

            // Always include the ID column
            String idCol = sanitizeIdentifier(def.getIdColumn());
            if (!resolvedColumns.contains(idCol)) {
                resolvedColumns.add(0, idCol);
            }

            return String.join(", ", resolvedColumns);
        }

        // Default: all columns
        List<String> allCols = new ArrayList<>();
        allCols.add(sanitizeIdentifier(def.getIdColumn()));
        for (FieldDefinition fd : def.getFieldDefinitions()) {
            String col = sanitizeIdentifier(fd.getColumnName());
            if (!allCols.contains(col)) {
                allCols.add(col);
            }
        }
        return String.join(", ", allCols);
    }

    /**
     * Build WHERE conditions from filter map. Only allows columns known in field definitions.
     */
    private List<String> buildWhereConditions(EntityDefinition<?> def,
                                               Map<String, Object> filters,
                                               MapSqlParameterSource params) {
        List<String> conditions = new ArrayList<>();
        if (filters == null || filters.isEmpty()) {
            return conditions;
        }

        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String colName = resolveColumnName(def, entry.getKey());
            if (colName != null) {
                String paramName = "f_" + entry.getKey().replaceAll("[^a-zA-Z0-9_]", "");
                conditions.add(sanitizeIdentifier(colName) + " = :" + paramName);
                params.addValue(paramName, entry.getValue());
            }
        }
        return conditions;
    }

    /**
     * Resolve a field name or column name to the actual DB column name.
     * Returns null if not found.
     */
    private String resolveColumnName(EntityDefinition<?> def, String name) {
        for (FieldDefinition fd : def.getFieldDefinitions()) {
            if (fd.getFieldName().equals(name) || fd.getColumnName().equals(name)) {
                return fd.getColumnName();
            }
        }
        // Check ID column
        if (def.getIdField().equals(name) || def.getIdColumn().equals(name)) {
            return def.getIdColumn();
        }
        return null;
    }

    // =========================================================================
    // MANY-TO-MANY RELATION QUERY
    // =========================================================================

    /**
     * Fetch target-entity rows related to a source entity via a junction table.
     * <p>
     * Generates SQL of the form:
     * <pre>
     * SELECT t.col1, t.col2, ...
     * FROM target_table t
     * JOIN junction_table j ON t.target_pk = j.target_join_col
     * WHERE j.source_join_col = :sourceId
     * [ORDER BY ...] [LIMIT ... OFFSET ...]
     * </pre>
     */
    public List<Map<String, Object>> findByRelation(EntityDefinition<?> targetDef,
                                                     ManyToManyRelation relation,
                                                     Object sourceId,
                                                     Map<String, Object> filters,
                                                     List<String> projectionColumns,
                                                     String sortBy,
                                                     String sortDirection,
                                                     Integer page,
                                                     Integer size) {

        String targetTable = sanitizeIdentifier(targetDef.getTableName());
        String junctionTable = sanitizeIdentifier(relation.getJunctionTable());
        String sourceJoinCol = sanitizeIdentifier(relation.getSourceJoinColumn());
        String targetJoinCol = sanitizeIdentifier(relation.getTargetJoinColumn());
        String targetPk = sanitizeIdentifier(targetDef.getIdColumn());

        // Build SELECT columns prefixed with 't.' (respecting projection)
        String selectCols = buildPrefixedSelectColumns(targetDef, "t", projectionColumns);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(selectCols)
           .append(" FROM ").append(targetTable).append(" t")
           .append(" JOIN ").append(junctionTable).append(" j")
           .append(" ON t.").append(targetPk).append(" = j.").append(targetJoinCol)
           .append(" WHERE j.").append(sourceJoinCol).append(" = :sourceId");

        // Soft-delete on target entity
        if (targetDef.isSoftDeleteEnabled()) {
            sql.append(" AND t.").append(sanitizeIdentifier(targetDef.getSoftDeleteColumn())).append(" = false");
        }

        MapSqlParameterSource params = new MapSqlParameterSource("sourceId", sourceId);

        // Apply filters on target entity columns (with 't.' alias prefix)
        List<String> filterConditions = buildWhereConditions(targetDef, filters, params);
        for (String condition : filterConditions) {
            sql.append(" AND t.").append(condition);
        }

        // ORDER BY
        if (sortBy != null && !sortBy.isBlank()) {
            String colName = resolveColumnName(targetDef, sortBy);
            if (colName != null) {
                String dir = "DESC".equalsIgnoreCase(sortDirection) ? "DESC" : "ASC";
                sql.append(" ORDER BY t.").append(sanitizeIdentifier(colName)).append(" ").append(dir);
            }
        }

        // PAGINATION
        if (size != null && size > 0) {
            int pageNum = (page != null && page >= 0) ? page : 0;
            sql.append(" LIMIT :limit OFFSET :offset");
            params.addValue("limit", size);
            params.addValue("offset", pageNum * size);
        }

        log.debug("findByRelation SQL: {}", sql);
        return jdbc.queryForList(sql.toString(), params);
    }

    /**
     * Count the number of target-entity rows related to a source entity via a junction table.
     */
    public long countByRelation(EntityDefinition<?> targetDef,
                                ManyToManyRelation relation,
                                Object sourceId,
                                Map<String, Object> filters) {

        String targetTable = sanitizeIdentifier(targetDef.getTableName());
        String junctionTable = sanitizeIdentifier(relation.getJunctionTable());
        String sourceJoinCol = sanitizeIdentifier(relation.getSourceJoinColumn());
        String targetJoinCol = sanitizeIdentifier(relation.getTargetJoinColumn());
        String targetPk = sanitizeIdentifier(targetDef.getIdColumn());

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM ").append(targetTable).append(" t")
           .append(" JOIN ").append(junctionTable).append(" j")
           .append(" ON t.").append(targetPk).append(" = j.").append(targetJoinCol)
           .append(" WHERE j.").append(sourceJoinCol).append(" = :sourceId");

        if (targetDef.isSoftDeleteEnabled()) {
            sql.append(" AND t.").append(sanitizeIdentifier(targetDef.getSoftDeleteColumn())).append(" = false");
        }

        MapSqlParameterSource params = new MapSqlParameterSource("sourceId", sourceId);

        // Apply filters on target entity columns (with 't.' alias prefix)
        List<String> filterConditions = buildWhereConditions(targetDef, filters, params);
        for (String condition : filterConditions) {
            sql.append(" AND t.").append(condition);
        }

        log.debug("countByRelation SQL: {}", sql);
        Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
        return count != null ? count : 0L;
    }

    /**
     * Build SELECT columns prefixed with a table alias (e.g. "t.id, t.name, t.price").
     * When {@code projectionColumns} is non-null, only those columns are included
     * (the ID column is always included).
     */
    private String buildPrefixedSelectColumns(EntityDefinition<?> def, String alias,
                                               List<String> projectionColumns) {
        if (projectionColumns != null && !projectionColumns.isEmpty()) {
            // Resolve projection columns the same way buildSelectColumns does,
            // but prefix each with the table alias.
            Set<String> knownColumns = def.getFieldDefinitions().stream()
                    .map(FieldDefinition::getColumnName)
                    .collect(Collectors.toSet());
            Map<String, String> fieldToCol = def.getFieldDefinitions().stream()
                    .collect(Collectors.toMap(FieldDefinition::getFieldName, FieldDefinition::getColumnName));

            List<String> resolved = new ArrayList<>();
            for (String col : projectionColumns) {
                if (knownColumns.contains(col)) {
                    resolved.add(alias + "." + sanitizeIdentifier(col));
                } else if (fieldToCol.containsKey(col)) {
                    resolved.add(alias + "." + sanitizeIdentifier(fieldToCol.get(col)));
                }
            }
            // Always include the ID column
            String idCol = alias + "." + sanitizeIdentifier(def.getIdColumn());
            if (!resolved.contains(idCol)) {
                resolved.add(0, idCol);
            }
            return String.join(", ", resolved);
        }

        // Default: all columns
        List<String> cols = new ArrayList<>();
        cols.add(alias + "." + sanitizeIdentifier(def.getIdColumn()));
        for (FieldDefinition fd : def.getFieldDefinitions()) {
            String col = alias + "." + sanitizeIdentifier(fd.getColumnName());
            if (!cols.contains(col)) {
                cols.add(col);
            }
        }
        return String.join(", ", cols);
    }

    // =========================================================================
    // MANY-TO-MANY ASSOCIATION MANAGEMENT (ADD / REMOVE)
    // =========================================================================

    /**
     * Add associations between a source entity and one or more target entities
     * by inserting rows into the junction table.
     * <p>
     * Generates SQL of the form:
     * <pre>
     * INSERT INTO junction_table (source_join_col, target_join_col)
     * VALUES (:sourceId, :targetId)
     * </pre>
     * Duplicate rows are silently ignored (uses MERGE for H2 / INSERT IGNORE pattern).
     *
     * @return the number of rows actually inserted
     */
    public int addRelations(ManyToManyRelation relation, Object sourceId, List<Object> targetIds) {
        String junctionTable = sanitizeIdentifier(relation.getJunctionTable());
        String sourceCol = sanitizeIdentifier(relation.getSourceJoinColumn());
        String targetCol = sanitizeIdentifier(relation.getTargetJoinColumn());

        // Use MERGE to avoid duplicate-key errors (H2-compatible; for MySQL use INSERT IGNORE)
        String sql = String.format(
                "MERGE INTO %s (%s, %s) KEY (%s, %s) VALUES (:sourceId, :targetId)",
                junctionTable, sourceCol, targetCol, sourceCol, targetCol);

        int inserted = 0;
        for (Object targetId : targetIds) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("sourceId", sourceId);
            params.addValue("targetId", targetId);
            log.debug("addRelation SQL: {} params: sourceId={}, targetId={}", sql, sourceId, targetId);
            inserted += jdbc.update(sql, params);
        }
        return inserted;
    }

    /**
     * Remove associations between a source entity and one or more target entities
     * by deleting rows from the junction table.
     * <p>
     * Generates SQL of the form:
     * <pre>
     * DELETE FROM junction_table
     * WHERE source_join_col = :sourceId AND target_join_col = :targetId
     * </pre>
     *
     * @return the number of rows actually deleted
     */
    public int removeRelations(ManyToManyRelation relation, Object sourceId, List<Object> targetIds) {
        String junctionTable = sanitizeIdentifier(relation.getJunctionTable());
        String sourceCol = sanitizeIdentifier(relation.getSourceJoinColumn());
        String targetCol = sanitizeIdentifier(relation.getTargetJoinColumn());

        String sql = String.format(
                "DELETE FROM %s WHERE %s = :sourceId AND %s = :targetId",
                junctionTable, sourceCol, targetCol);

        int deleted = 0;
        for (Object targetId : targetIds) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("sourceId", sourceId);
            params.addValue("targetId", targetId);
            log.debug("removeRelation SQL: {} params: sourceId={}, targetId={}", sql, sourceId, targetId);
            deleted += jdbc.update(sql, params);
        }
        return deleted;
    }

    /**
     * Sanitize an identifier (table name, column name) to prevent SQL injection.
     * Only allows alphanumeric characters and underscores.
     */
    private String sanitizeIdentifier(String identifier) {
        if (identifier == null || !identifier.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid SQL identifier: " + identifier);
        }
        return identifier;
    }
}
