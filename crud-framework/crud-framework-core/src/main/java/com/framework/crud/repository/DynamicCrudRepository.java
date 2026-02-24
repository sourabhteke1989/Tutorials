package com.framework.crud.repository;

import com.framework.crud.definition.EntityDefinition;
import com.framework.crud.exception.EntityNotFoundException;
import com.framework.crud.model.FieldDefinition;
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
        // Filter to only insertable fields
        Map<String, String> fieldToColumn = def.getFieldDefinitions().stream()
                .filter(FieldDefinition::isInsertable)
                .filter(f -> payload.containsKey(f.getFieldName()))
                .collect(Collectors.toMap(FieldDefinition::getFieldName, FieldDefinition::getColumnName,
                        (a, b) -> a, LinkedHashMap::new));

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

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder);

        // Try to return the generated ID
        Object generatedId = null;
        if (keyHolder.getKeys() != null && !keyHolder.getKeys().isEmpty()) {
            // Take the first key
            generatedId = keyHolder.getKeys().values().iterator().next();
        } else if (keyHolder.getKey() != null) {
            generatedId = keyHolder.getKey();
        }

        // Fetch and return the created record
        if (generatedId != null) {
            try {
                return findById(def, generatedId, null);
            } catch (EntityNotFoundException e) {
                // Fallback — return the payload with the generated ID
                Map<String, Object> result = new LinkedHashMap<>(payload);
                result.put(def.getIdField(), generatedId);
                return result;
            }
        }

        // If no generated key, return payload as-is
        return new LinkedHashMap<>(payload);
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
