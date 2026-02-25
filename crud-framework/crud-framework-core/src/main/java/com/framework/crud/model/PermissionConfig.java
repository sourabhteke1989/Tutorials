package com.framework.crud.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Comprehensive permission configuration for an entity, supporting fine-grained
 * access control for all CRUD operations plus filter-based permissions.
 * <p>
 * The permission model distinguishes between:
 * <ul>
 *   <li><b>LIST</b> — GET without an ID (returns multiple records). Requires {@code listPermission}.</li>
 *   <li><b>GET</b> — GET with an ID (returns a single record). Requires {@code getPermission}.</li>
 *   <li><b>CREATE</b> — Requires {@code createPermission}.</li>
 *   <li><b>UPDATE</b> — Requires {@code updatePermission}.</li>
 *   <li><b>DELETE</b> — Requires {@code deletePermission}.</li>
 *   <li><b>Filter permissions</b> — When a LIST request includes specific filter field
 *       combinations, a more specific permission may be required instead of the generic
 *       {@code listPermission}. See {@link FilterPermission}.</li>
 * </ul>
 * <p>
 * If a permission is {@code null} or blank, no permission check is performed for that
 * operation (open access).
 *
 * <h3>Usage Example</h3>
 * <pre>
 * PermissionConfig.builder()
 *     .listPermission("ListOrder")
 *     .getPermission("GetOrder")
 *     .createPermission("CreateOrder")
 *     .updatePermission("UpdateOrder")
 *     .deletePermission("DeleteOrder")
 *     .filterPermission(FilterPermission.of(Set.of("customer_id"), "ListCustomerOrders"))
 *     .build();
 * </pre>
 *
 * @see FilterPermission
 */
public class PermissionConfig {

    private final String listPermission;
    private final String getPermission;
    private final String createPermission;
    private final String updatePermission;
    private final String deletePermission;
    private final List<FilterPermission> filterPermissions;

    private PermissionConfig(Builder builder) {
        this.listPermission = builder.listPermission;
        this.getPermission = builder.getPermission;
        this.createPermission = builder.createPermission;
        this.updatePermission = builder.updatePermission;
        this.deletePermission = builder.deletePermission;
        this.filterPermissions = Collections.unmodifiableList(new ArrayList<>(builder.filterPermissions));
    }

    /**
     * Returns a new builder for constructing a PermissionConfig.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns an empty PermissionConfig (no permissions required for any operation).
     */
    public static PermissionConfig empty() {
        return new Builder().build();
    }

    // ---- Getters ----

    public String getListPermission() {
        return listPermission;
    }

    public String getGetPermission() {
        return getPermission;
    }

    public String getCreatePermission() {
        return createPermission;
    }

    public String getUpdatePermission() {
        return updatePermission;
    }

    public String getDeletePermission() {
        return deletePermission;
    }

    public List<FilterPermission> getFilterPermissions() {
        return filterPermissions;
    }

    /**
     * Returns whether this config has any permissions defined.
     */
    public boolean hasPermissions() {
        return isNotBlank(listPermission) || isNotBlank(getPermission)
                || isNotBlank(createPermission) || isNotBlank(updatePermission)
                || isNotBlank(deletePermission) || !filterPermissions.isEmpty();
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }

    @Override
    public String toString() {
        return String.format(
                "PermissionConfig{list='%s', get='%s', create='%s', update='%s', delete='%s', filters=%s}",
                listPermission, getPermission, createPermission, updatePermission,
                deletePermission, filterPermissions);
    }

    // ---- Builder ----

    public static class Builder {
        private String listPermission;
        private String getPermission;
        private String createPermission;
        private String updatePermission;
        private String deletePermission;
        private final List<FilterPermission> filterPermissions = new ArrayList<>();

        private Builder() {}

        public Builder listPermission(String listPermission) {
            this.listPermission = listPermission;
            return this;
        }

        public Builder getPermission(String getPermission) {
            this.getPermission = getPermission;
            return this;
        }

        public Builder createPermission(String createPermission) {
            this.createPermission = createPermission;
            return this;
        }

        public Builder updatePermission(String updatePermission) {
            this.updatePermission = updatePermission;
            return this;
        }

        public Builder deletePermission(String deletePermission) {
            this.deletePermission = deletePermission;
            return this;
        }

        /**
         * Add a filter permission to the configuration.
         * Filter permissions define special permission requirements for specific
         * filter field combinations on LIST operations.
         */
        public Builder filterPermission(FilterPermission filterPermission) {
            if (filterPermission != null) {
                this.filterPermissions.add(filterPermission);
            }
            return this;
        }

        /**
         * Add multiple filter permissions at once.
         */
        public Builder filterPermissions(List<FilterPermission> filterPermissions) {
            if (filterPermissions != null) {
                this.filterPermissions.addAll(filterPermissions);
            }
            return this;
        }

        public PermissionConfig build() {
            return new PermissionConfig(this);
        }
    }
}
