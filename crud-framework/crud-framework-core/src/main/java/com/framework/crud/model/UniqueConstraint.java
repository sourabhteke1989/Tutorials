package com.framework.crud.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Defines a uniqueness constraint for an entity.
 * <p>
 * Before CREATE, the framework checks whether a record already exists
 * matching ALL the specified field values. If a duplicate is found,
 * the operation is rejected with a descriptive error.
 * <p>
 * Supports both single-column and composite (multi-column) uniqueness.
 *
 * <pre>
 * // Single column uniqueness (e.g. email must be unique)
 * UniqueConstraint.of("email")
 *
 * // Composite uniqueness (e.g. name + category must be unique together)
 * UniqueConstraint.of("name", "category")
 *     .withMessage("A product with this name already exists in this category")
 * </pre>
 */
public class UniqueConstraint {

    /** Field names (from payload/JSON, NOT column names) that form this constraint. */
    private final List<String> fieldNames;

    /** Custom error message. If null, a default message is generated. */
    private String message;

    private UniqueConstraint(List<String> fieldNames) {
        if (fieldNames == null || fieldNames.isEmpty()) {
            throw new IllegalArgumentException("UniqueConstraint must have at least one field");
        }
        this.fieldNames = Collections.unmodifiableList(fieldNames);
    }

    /**
     * Create a uniqueness constraint on one or more fields.
     *
     * @param fieldNames One or more field names (JSON payload keys) that must be unique together.
     */
    public static UniqueConstraint of(String... fieldNames) {
        return new UniqueConstraint(Arrays.asList(fieldNames));
    }

    /**
     * Set a custom error message shown when a duplicate is detected.
     */
    public UniqueConstraint withMessage(String message) {
        this.message = message;
        return this;
    }

    public List<String> getFieldNames() {
        return fieldNames;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Generate a default error message if none was provided.
     */
    public String getEffectiveMessage() {
        if (message != null && !message.isBlank()) {
            return message;
        }
        if (fieldNames.size() == 1) {
            return "A record with this " + fieldNames.get(0) + " already exists";
        }
        return "A record with this combination of [" + String.join(", ", fieldNames) + "] already exists";
    }
}
