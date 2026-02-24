package com.framework.crud.exception;

/**
 * Thrown when a requested entity type is not registered or the operation is not supported.
 */
public class UnsupportedEntityException extends CrudException {

    public UnsupportedEntityException(String entityType) {
        super("UNSUPPORTED_ENTITY",
                String.format("Entity type '%s' is not registered", entityType));
    }

    public UnsupportedEntityException(String entityType, String operation) {
        super("UNSUPPORTED_OPERATION",
                String.format("Operation '%s' is not supported for entity '%s'", operation, entityType));
    }
}
