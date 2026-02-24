package com.framework.crud.exception;

/**
 * Thrown when a requested entity is not found in the database.
 */
public class EntityNotFoundException extends CrudException {

    public EntityNotFoundException(String entityType, Object id) {
        super("ENTITY_NOT_FOUND",
                String.format("Entity '%s' with id '%s' not found", entityType, id));
    }
}
