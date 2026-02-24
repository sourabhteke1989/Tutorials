package com.framework.crud.event;

import com.framework.crud.model.CrudOperation;

import java.time.Instant;
import java.util.Map;

/**
 * Event published before and after every CRUD operation.
 * <p>
 * Developers can listen to these events using Spring's {@code @EventListener}
 * or {@code ApplicationListener<CrudEvent>} for cross-cutting concerns
 * like notifications, cache invalidation, or custom audit trails.
 */
public class CrudEvent {

    public enum Phase {
        /** Fired before the operation executes (validation already passed). */
        PRE,
        /** Fired after the operation completes successfully. */
        POST
    }

    private final String entityType;
    private final CrudOperation operation;
    private final Phase phase;
    private final Object id;
    private final Map<String, Object> payload;
    private final String username;
    private final Instant timestamp;

    public CrudEvent(String entityType, CrudOperation operation, Phase phase,
                     Object id, Map<String, Object> payload, String username) {
        this.entityType = entityType;
        this.operation = operation;
        this.phase = phase;
        this.id = id;
        this.payload = payload;
        this.username = username;
        this.timestamp = Instant.now();
    }

    public String getEntityType() {
        return entityType;
    }

    public CrudOperation getOperation() {
        return operation;
    }

    public Phase getPhase() {
        return phase;
    }

    public Object getId() {
        return id;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public String getUsername() {
        return username;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("CrudEvent{phase=%s, entity='%s', op=%s, id=%s, user='%s'}",
                phase, entityType, operation, id, username);
    }
}
