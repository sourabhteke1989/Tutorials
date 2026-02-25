package com.framework.crud.model;

/**
 * Supported operations for many-to-many relation requests.
 * <p>
 * Used in {@link RelationRequest} to specify the action to perform
 * on the junction table:
 * <ul>
 *   <li>{@link #GET} — query related entities through the junction table</li>
 *   <li>{@link #ADD} — create associations (insert rows into the junction table)</li>
 *   <li>{@link #REMOVE} — remove associations (delete rows from the junction table)</li>
 * </ul>
 */
public enum RelationOperation {

    /** Query related entities via JOIN through the junction table. */
    GET,

    /** Insert association rows into the junction table. */
    ADD,

    /** Delete association rows from the junction table. */
    REMOVE;

    /**
     * Parse a string to RelationOperation (case-insensitive).
     * Returns {@link #GET} when the input is {@code null} or blank (default operation).
     *
     * @param value the string value to parse
     * @return the matching RelationOperation
     * @throws IllegalArgumentException if the value is not a valid operation
     */
    public static RelationOperation fromString(String value) {
        if (value == null || value.isBlank()) {
            return GET; // default operation
        }
        try {
            return RelationOperation.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid relation operation: '" + value + "'. Supported: GET, ADD, REMOVE");
        }
    }
}
