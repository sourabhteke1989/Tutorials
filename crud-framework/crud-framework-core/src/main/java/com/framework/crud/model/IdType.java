package com.framework.crud.model;

/**
 * Supported primary key generation strategies.
 * <p>
 * Determines how the framework handles ID generation during CREATE:
 * <ul>
 *   <li>{@link #AUTO_INCREMENT} — Database auto-generates a sequential numeric ID (BIGINT).
 *       The framework retrieves the generated key via JDBC KeyHolder.</li>
 *   <li>{@link #UUID} — The framework generates a random UUID (v4) and inserts it
 *       as the primary key. Avoids auto-increment exhaustion for high-volume tables.</li>
 * </ul>
 */
public enum IdType {

    /**
     * Database-managed auto-increment (default).
     * Suitable for most entities using BIGINT/SERIAL primary keys.
     */
    AUTO_INCREMENT,

    /**
     * Framework-generated UUID (v4).
     * The ID column should be VARCHAR(36) or UUID type in the database.
     * The framework generates the UUID before INSERT and includes it in the payload.
     */
    UUID
}
