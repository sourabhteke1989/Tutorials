package com.framework.crud.controller;

import com.framework.crud.model.CrudRequest;
import com.framework.crud.model.CrudResponse;
import com.framework.crud.model.RelationRequest;
import com.framework.crud.registry.EntityRegistry;
import com.framework.crud.service.CrudService;
import com.framework.crud.service.RelationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Single REST controller that handles all CRUD operations.
 * <p>
 * Endpoint: {@code POST /api/crud}
 * <p>
 * All operations go through the same endpoint. The {@code operation} field
 * in the request body determines what action is taken.
 * <p>
 * Additionally provides:
 * <ul>
 *   <li>{@code POST /api/crud/relation} — query many-to-many related entities</li>
 *   <li>{@code GET /api/crud/entities} — list all registered entity types</li>
 *   <li>{@code GET /api/crud/health} — health check</li>
 * </ul>
 */
@RestController
@RequestMapping("${crud.framework.base-path:/api/crud}")
public class CrudController {

    private final CrudService crudService;
    private final RelationService relationService;
    private final EntityRegistry entityRegistry;

    public CrudController(CrudService crudService, RelationService relationService, EntityRegistry entityRegistry) {
        this.crudService = crudService;
        this.relationService = relationService;
        this.entityRegistry = entityRegistry;
    }

    /**
     * Main CRUD endpoint. Accepts a generic request and routes internally.
     */
    @PostMapping
    public ResponseEntity<CrudResponse> execute(@RequestBody CrudRequest request) {
        CrudResponse response = crudService.process(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Many-to-many relation endpoint.
     * Queries related entities through a junction table.
     */
    @PostMapping("/relation")
    public ResponseEntity<CrudResponse> queryRelation(@RequestBody RelationRequest request) {
        CrudResponse response = relationService.processRelation(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * List all registered entity types and their metadata.
     */
    @GetMapping("/entities")
    public ResponseEntity<Map<String, Object>> listEntities() {
        var entities = entityRegistry.getAll().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Map.of(
                                "tableName", e.getValue().getTableName(),
                                "entityClass", e.getValue().getEntityClass().getSimpleName(),
                                "allowedOperations", e.getValue().getAllowedOperations(),
                                "projectionTypes", e.getValue().getProjectionTypes().keySet(),
                                "fieldCount", e.getValue().getFieldDefinitions().size()
                        )
                ));
        return ResponseEntity.ok(Map.of("entities", entities));
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "framework", "crud-framework",
                "registeredEntities", entityRegistry.getAll().size()
        ));
    }
}
