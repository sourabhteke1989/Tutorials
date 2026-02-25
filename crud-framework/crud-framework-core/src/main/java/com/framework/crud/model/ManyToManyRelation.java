package com.framework.crud.model;

/**
 * Defines a many-to-many relationship between two entities through a junction table.
 * <p>
 * Given entities X and Y with a junction table Z:
 * <pre>
 *   X  ←——  Z  ——→  Y
 *   (source)  (junction)  (target)
 * </pre>
 * This definition tells the framework how to JOIN through Z to fetch Y records
 * related to a given X record.
 * <p>
 * Example — Products ↔ Tags via product_tags:
 * <pre>
 * ManyToManyRelation.builder()
 *     .relationName("tags")
 *     .targetEntityType("tag")
 *     .junctionTable("product_tags")
 *     .sourceJoinColumn("product_id")
 *     .targetJoinColumn("tag_id")
 *     .getPermission("ListProductTags")
 *     .addPermission("AddTagToProduct")
 *     .removePermission("RemoveTagFromProduct")
 *     .build();
 * </pre>
 */
public class ManyToManyRelation {

    /** Logical name for this relation (e.g. "tags", "categories", "roles"). Used in requests. */
    private final String relationName;

    /** The target entity type string. Must match a registered EntityDefinition's entityType. */
    private final String targetEntityType;

    /** The junction/bridge table name (e.g. "product_tags", "user_roles"). */
    private final String junctionTable;

    /** Column in the junction table that references the SOURCE entity's PK (e.g. "product_id"). */
    private final String sourceJoinColumn;

    /** Column in the junction table that references the TARGET entity's PK (e.g. "tag_id"). */
    private final String targetJoinColumn;

    /** Permission required for GET (listing) related entities through this relation. Optional. */
    private final String getPermission;

    /** Permission required for ADD (creating a junction row). Optional. */
    private final String addPermission;

    /** Permission required for REMOVE (deleting a junction row). Optional. */
    private final String removePermission;

    private ManyToManyRelation(Builder builder) {
        this.relationName = builder.relationName;
        this.targetEntityType = builder.targetEntityType;
        this.junctionTable = builder.junctionTable;
        this.sourceJoinColumn = builder.sourceJoinColumn;
        this.targetJoinColumn = builder.targetJoinColumn;
        this.getPermission = builder.getPermission;
        this.addPermission = builder.addPermission;
        this.removePermission = builder.removePermission;
    }

    // ---- Getters ----

    public String getRelationName() {
        return relationName;
    }

    public String getTargetEntityType() {
        return targetEntityType;
    }

    public String getJunctionTable() {
        return junctionTable;
    }

    public String getSourceJoinColumn() {
        return sourceJoinColumn;
    }

    public String getTargetJoinColumn() {
        return targetJoinColumn;
    }

    public String getGetPermission() {
        return getPermission;
    }

    public String getAddPermission() {
        return addPermission;
    }

    public String getRemovePermission() {
        return removePermission;
    }

    // ---- Builder ----

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String relationName;
        private String targetEntityType;
        private String junctionTable;
        private String sourceJoinColumn;
        private String targetJoinColumn;
        private String getPermission;
        private String addPermission;
        private String removePermission;

        public Builder relationName(String relationName) {
            this.relationName = relationName;
            return this;
        }

        public Builder targetEntityType(String targetEntityType) {
            this.targetEntityType = targetEntityType;
            return this;
        }

        public Builder junctionTable(String junctionTable) {
            this.junctionTable = junctionTable;
            return this;
        }

        public Builder sourceJoinColumn(String sourceJoinColumn) {
            this.sourceJoinColumn = sourceJoinColumn;
            return this;
        }

        public Builder targetJoinColumn(String targetJoinColumn) {
            this.targetJoinColumn = targetJoinColumn;
            return this;
        }

        public Builder getPermission(String getPermission) {
            this.getPermission = getPermission;
            return this;
        }

        public Builder addPermission(String addPermission) {
            this.addPermission = addPermission;
            return this;
        }

        public Builder removePermission(String removePermission) {
            this.removePermission = removePermission;
            return this;
        }

        public ManyToManyRelation build() {
            if (relationName == null || relationName.isBlank()) {
                throw new IllegalArgumentException("relationName is required");
            }
            if (targetEntityType == null || targetEntityType.isBlank()) {
                throw new IllegalArgumentException("targetEntityType is required");
            }
            if (junctionTable == null || junctionTable.isBlank()) {
                throw new IllegalArgumentException("junctionTable is required");
            }
            if (sourceJoinColumn == null || sourceJoinColumn.isBlank()) {
                throw new IllegalArgumentException("sourceJoinColumn is required");
            }
            if (targetJoinColumn == null || targetJoinColumn.isBlank()) {
                throw new IllegalArgumentException("targetJoinColumn is required");
            }
            return new ManyToManyRelation(this);
        }
    }

    @Override
    public String toString() {
        return String.format("ManyToManyRelation{%s: %s via %s (%s → %s)}",
                relationName, targetEntityType, junctionTable, sourceJoinColumn, targetJoinColumn);
    }
}
