package com.framework.crud.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Defines a permission requirement for a specific combination of filter fields
 * on a LIST (GET-without-id) operation.
 * <p>
 * When a LIST request contains filters whose keys include <b>all</b> the fields
 * defined in this {@code FilterPermission}, the associated permission is required
 * instead of the entity's generic {@code listPermission}.
 * <p>
 * This allows developers to enforce fine-grained access control over which
 * data subsets a user can query. For example, listing all orders may require
 * the {@code "ListOrder"} permission, but listing orders for a specific customer
 * (filtering by {@code customer_id}) may require the {@code "ListCustomerOrders"}
 * permission.
 * <p>
 * <b>Matching rule:</b> A {@code FilterPermission} matches an incoming request
 * when <em>all</em> of its {@code filterFields} appear as keys in the request's
 * filter map. Extra filter keys in the request are allowed (superset match).
 * When multiple {@code FilterPermission}s match, the one with the <b>most</b>
 * matching fields (most specific) takes precedence.
 *
 * <h3>Usage Example</h3>
 * <pre>
 * // In OrderEntityDefinition.getPermissionConfig():
 * PermissionConfig.builder()
 *     .listPermission("ListOrder")
 *     .filterPermission(
 *         FilterPermission.of(Set.of("customer_id"), "ListCustomerOrders")
 *             .description("List orders for a specific customer")
 *     )
 *     .build();
 * </pre>
 */
public class FilterPermission {

    /** The set of filter field names that trigger this permission. */
    private final Set<String> filterFields;

    /** The permission string required when this filter combination is used. */
    private final String permission;

    /** Optional human-readable description of this filter permission. */
    private String description;

    private FilterPermission(Set<String> filterFields, String permission) {
        if (filterFields == null || filterFields.isEmpty()) {
            throw new IllegalArgumentException("filterFields must not be null or empty");
        }
        if (permission == null || permission.isBlank()) {
            throw new IllegalArgumentException("permission must not be null or blank");
        }
        this.filterFields = Collections.unmodifiableSet(new LinkedHashSet<>(filterFields));
        this.permission = permission;
    }

    /**
     * Create a FilterPermission for the given filter fields and permission.
     *
     * @param filterFields the set of filter field names (e.g. {@code Set.of("customer_id")})
     * @param permission   the permission string required (e.g. {@code "ListCustomerOrders"})
     * @return a new FilterPermission instance
     */
    public static FilterPermission of(Set<String> filterFields, String permission) {
        return new FilterPermission(filterFields, permission);
    }

    /**
     * Set an optional description for this filter permission.
     *
     * @param description human-readable description
     * @return this instance for chaining
     */
    public FilterPermission description(String description) {
        this.description = description;
        return this;
    }

    /**
     * Check whether this filter permission matches the given set of request filter keys.
     * A match occurs when <b>all</b> fields in {@code filterFields} are present
     * in the incoming filter key set.
     *
     * @param requestFilterKeys the filter keys from the incoming request
     * @return true if all filter fields are present in the request
     */
    public boolean matches(Set<String> requestFilterKeys) {
        if (requestFilterKeys == null || requestFilterKeys.isEmpty()) {
            return false;
        }
        return requestFilterKeys.containsAll(filterFields);
    }

    /**
     * Returns the number of filter fields in this permission.
     * Used for specificity ranking — more fields = more specific match.
     */
    public int specificity() {
        return filterFields.size();
    }

    // ---- Getters ----

    public Set<String> getFilterFields() {
        return filterFields;
    }

    public String getPermission() {
        return permission;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return String.format("FilterPermission{fields=%s, permission='%s'}", filterFields, permission);
    }
}
