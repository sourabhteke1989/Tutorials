package com.framework.crud.registry;

import com.framework.crud.definition.EntityDefinition;
import com.framework.crud.exception.UnsupportedEntityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry that discovers and indexes all {@link EntityDefinition} beans.
 * <p>
 * Spring auto-wires all {@code EntityDefinition<?>} implementations into this
 * component at startup. The registry validates uniqueness and provides fast
 * look-up by entity type string.
 */
@Component
public class EntityRegistry {

    private static final Logger log = LoggerFactory.getLogger(EntityRegistry.class);

    private final Map<String, EntityDefinition<?>> registry = new ConcurrentHashMap<>();

    /**
     * Constructor injection — Spring provides all EntityDefinition beans.
     */
    public EntityRegistry(List<EntityDefinition<?>> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            log.warn("No EntityDefinition beans found. The CRUD framework has no entities to manage.");
            return;
        }

        for (EntityDefinition<?> def : definitions) {
            String entityType = def.getEntityType();
            if (entityType == null || entityType.isBlank()) {
                throw new IllegalStateException(
                        "EntityDefinition " + def.getClass().getName() + " returned null/blank entityType");
            }

            String key = entityType.toLowerCase().trim();
            EntityDefinition<?> existing = registry.put(key, def);
            if (existing != null) {
                throw new IllegalStateException(
                        String.format("Duplicate entityType '%s' registered by [%s] and [%s]",
                                entityType, existing.getClass().getName(), def.getClass().getName()));
            }

            log.info("Registered entity: '{}' → table '{}' (class: {})",
                    entityType, def.getTableName(), def.getEntityClass().getSimpleName());
        }

        log.info("EntityRegistry initialised with {} entity definition(s)", registry.size());
    }

    /**
     * Look up an entity definition by its logical type string.
     *
     * @throws UnsupportedEntityException if the entity type is not registered
     */
    public EntityDefinition<?> getDefinition(String entityType) {
        if (entityType == null || entityType.isBlank()) {
            throw new IllegalArgumentException("entityType must not be null or blank");
        }
        EntityDefinition<?> def = registry.get(entityType.toLowerCase().trim());
        if (def == null) {
            throw new UnsupportedEntityException(entityType);
        }
        return def;
    }

    /**
     * Check if an entity type is registered.
     */
    public boolean isRegistered(String entityType) {
        return entityType != null && registry.containsKey(entityType.toLowerCase().trim());
    }

    /**
     * Return an unmodifiable view of all registered entity types.
     */
    public Map<String, EntityDefinition<?>> getAll() {
        return Collections.unmodifiableMap(registry);
    }
}
