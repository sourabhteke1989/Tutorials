package com.framework.crud.exception;

import java.util.List;
import java.util.Map;

/**
 * Thrown when a CREATE operation would produce a duplicate entity
 * based on the uniqueness constraints defined in the EntityDefinition.
 */
public class DuplicateEntityException extends CrudException {

    private final String entityType;
    private final List<String> constraintFields;
    private final Map<String, Object> duplicateValues;

    public DuplicateEntityException(String entityType, List<String> constraintFields,
                                     Map<String, Object> duplicateValues, String message) {
        super("DUPLICATE_ENTITY", message);
        this.entityType = entityType;
        this.constraintFields = constraintFields;
        this.duplicateValues = duplicateValues;
    }

    public String getEntityType() {
        return entityType;
    }

    public List<String> getConstraintFields() {
        return constraintFields;
    }

    public Map<String, Object> getDuplicateValues() {
        return duplicateValues;
    }
}
