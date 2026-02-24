# CRUD Framework

A **generic CRUD framework** built on top of Spring Boot that processes Create, Read, Update, and Delete operations for any entity through a single REST endpoint. Developers define **one class per entity** — the framework handles everything else.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        CrudController                           │
│                     POST /api/crud                              │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                         CrudService                             │
│                    (Orchestrator)                                │
│                                                                 │
│  1. Resolve EntityDefinition from entityType                    │
│  2. Check RBAC permissions (PermissionService)                  │
│  3. Validate payload (ValidationService)                        │
│  4. Check uniqueness constraints (CREATE only)                  │
│  5. Publish PRE event                                           │
│  6. Call beforeSave() hook                                      │
│  7. Execute via DynamicCrudRepository                           │
│  8. Call afterSave() hook                                       │
│  9. Publish POST event                                          │
│ 10. Audit log                                                   │
│ 11. Return CrudResponse                                         │
└──────┬──────────┬──────────┬──────────┬─────────────────────────┘
       │          │          │          │
       ▼          ▼          ▼          ▼
  Permission   Validation  Dynamic    Audit
  Service      Service     CRUD Repo  Service
       │          │          │
       ▼          ▼          ▼
  Security    EntityDef   JDBC
  Context     (dev code)  Template
```

---

## Project Structure

```
crud-framework/
├── pom.xml                              # Parent POM
├── crud-framework-core/                 # Framework library
│   ├── pom.xml
│   └── src/main/java/com/framework/crud/
│       ├── config/
│       │   └── CrudAutoConfiguration.java
│       ├── controller/
│       │   └── CrudController.java
│       ├── definition/
│       │   └── EntityDefinition.java      ← Interface devs implement
│       ├── event/
│       │   ├── CrudEvent.java
│       │   └── CrudEventPublisher.java
│       ├── exception/
│       │   ├── CrudException.java
│       │   ├── CrudAccessDeniedException.java
│       │   ├── CrudValidationException.java
│       │   ├── DuplicateEntityException.java
│       │   ├── EntityNotFoundException.java
│       │   ├── UnsupportedEntityException.java
│       │   └── GlobalExceptionHandler.java
│       ├── model/
│       │   ├── CrudOperation.java
│       │   ├── CrudRequest.java
│       │   ├── CrudResponse.java
│       │   ├── FieldDefinition.java
│       │   ├── UniqueConstraint.java
│       │   └── ValidationResult.java
│       ├── registry/
│       │   └── EntityRegistry.java
│       ├── repository/
│       │   └── DynamicCrudRepository.java
│       ├── security/
│       │   ├── CrudSecurityContext.java
│       │   ├── SpringSecurityCrudContext.java
│       │   └── PermissionService.java
│       ├── service/
│       │   ├── CrudService.java
│       │   └── AuditService.java
│       └── validation/
│           └── ValidationService.java
│
└── crud-framework-example/              # Demo application
    ├── pom.xml
    └── src/main/
        ├── java/com/example/crud/
        │   ├── CrudExampleApplication.java
        │   ├── config/SecurityConfig.java
        │   └── definition/
        │       ├── ProductEntityDefinition.java
        │       └── CustomerEntityDefinition.java
        └── resources/
            ├── application.yml
            ├── schema.sql
            └── data.sql
```

---

## Quick Start

### 1. Add the dependency

```xml
<dependency>
    <groupId>com.framework</groupId>
    <artifactId>crud-framework-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Define your entity (ONE class per entity)

```java
@Component
public class ProductEntityDefinition implements EntityDefinition<Product> {

    @Override
    public String getEntityType() { return "product"; }

    @Override
    public String getTableName() { return "products"; }

    @Override
    public Class<Product> getEntityClass() { return Product.class; }

    @Override
    public List<FieldDefinition> getFieldDefinitions() {
        return List.of(
            FieldDefinition.of("name", "name", String.class)
                .required(true).maxLength(200),
            FieldDefinition.of("price", "price", Double.class)
                .required(true).minValue(0.01),
            FieldDefinition.of("status", "status", String.class)
                .pattern("^(active|inactive)$").defaultValue("active")
        );
    }

    @Override
    public Map<String, List<String>> getProjectionTypes() {
        return Map.of(
            "summary", List.of("id", "name", "price"),
            "detail",  List.of("id", "name", "price", "status")
        );
    }

    @Override
    public Map<CrudOperation, String> getRequiredPermissions() {
        return Map.of(
            CrudOperation.GET, "product:read",
            CrudOperation.CREATE, "product:write",
            CrudOperation.UPDATE, "product:write",
            CrudOperation.DELETE, "product:admin"
        );
    }

    @Override
    public List<UniqueConstraint> getUniqueConstraints() {
        return List.of(
            UniqueConstraint.of("name", "category")
                .withMessage("A product with this name already exists in this category")
        );
    }

    @Override
    public ValidationResult validate(CrudOperation op, Product entity) {
        // Custom business rules
        ValidationResult r = ValidationResult.builder();
        if (entity.getPrice() > 10000) {
            r.addError("price", "Price exceeds maximum allowed");
        }
        return r;
    }
}
```

That's it! The framework auto-discovers all `@Component` classes implementing `EntityDefinition` and wires up everything.

### 3. Use the API

All operations go through **one endpoint**: `POST /api/crud`

---

## API Reference

### Request Schema

```json
{
  "entityType": "product",
  "operation": "GET | CREATE | UPDATE | DELETE",
  "id": 1,
  "projectionType": "summary",
  "payload": { "name": "Widget", "price": 9.99 },
  "filters": { "status": "active", "category": "Electronics" },
  "page": 0,
  "size": 20,
  "sortBy": "name",
  "sortDirection": "ASC"
}
```

| Field            | Required | Used By          | Description                                       |
|------------------|----------|------------------|---------------------------------------------------|
| `entityType`     | Yes      | All              | Logical entity name (maps to table)               |
| `operation`      | Yes      | All              | `GET`, `CREATE`, `UPDATE`, `DELETE`                |
| `id`             | Cond.    | GET/UPDATE/DELETE | Primary key (required for single get/update/delete)|
| `projectionType` | No       | GET              | Named column set (e.g. "summary", "detail")       |
| `payload`        | Cond.    | CREATE/UPDATE    | Column values as JSON object                      |
| `filters`        | No       | GET (list)       | Equality filters (`column: value`)                |
| `page`           | No       | GET (list)       | Page number (0-based)                             |
| `size`           | No       | GET (list)       | Page size                                         |
| `sortBy`         | No       | GET (list)       | Column/field to sort by                           |
| `sortDirection`  | No       | GET (list)       | `ASC` or `DESC`                                   |

### Response Schema

```json
{
  "success": true,
  "message": "Entity created successfully",
  "data": { "id": 1, "name": "Widget", "price": 9.99 },
  "dataList": [ ... ],
  "errors": { "name": "is required" },
  "totalCount": 42,
  "page": 0,
  "size": 20,
  "timestamp": "2026-02-24T10:30:00Z"
}
```

---

## Example Requests

### GET single entity
```bash
curl -X POST http://localhost:8080/api/crud \
  -u admin:admin \
  -H "Content-Type: application/json" \
  -d '{
    "entityType": "product",
    "operation": "GET",
    "id": 1,
    "projectionType": "detail"
  }'
```

### GET list (paginated, filtered, sorted)
```bash
curl -X POST http://localhost:8080/api/crud \
  -u admin:admin \
  -H "Content-Type: application/json" \
  -d '{
    "entityType": "product",
    "operation": "GET",
    "projectionType": "summary",
    "filters": { "category": "Electronics" },
    "page": 0,
    "size": 10,
    "sortBy": "price",
    "sortDirection": "DESC"
  }'
```

### CREATE
```bash
curl -X POST http://localhost:8080/api/crud \
  -u editor:editor \
  -H "Content-Type: application/json" \
  -d '{
    "entityType": "product",
    "operation": "CREATE",
    "payload": {
      "name": "New Widget",
      "description": "A fantastic widget",
      "price": 19.99,
      "category": "Electronics",
      "status": "active",
      "stockQuantity": 100
    }
  }'
```

### UPDATE
```bash
curl -X POST http://localhost:8080/api/crud \
  -u editor:editor \
  -H "Content-Type: application/json" \
  -d '{
    "entityType": "product",
    "operation": "UPDATE",
    "id": 1,
    "payload": {
      "price": 24.99,
      "stockQuantity": 200
    }
  }'
```

### DELETE
```bash
curl -X POST http://localhost:8080/api/crud \
  -u admin:admin \
  -H "Content-Type: application/json" \
  -d '{
    "entityType": "product",
    "operation": "DELETE",
    "id": 1
  }'
```

---

## Features

### EntityDefinition Interface

The only thing a developer writes per entity. Methods:

| Method                    | Required | Description                                              |
|---------------------------|----------|----------------------------------------------------------|
| `getEntityType()`         | Yes      | Logical name (used in requests)                          |
| `getTableName()`          | Yes      | Physical DB table name                                   |
| `getEntityClass()`        | Yes      | POJO class for payload conversion                        |
| `getFieldDefinitions()`   | Yes      | Field metadata (required, maxLength, pattern, etc.)      |
| `getIdColumn()`           | No       | PK column name (default: `id`)                           |
| `getProjectionTypes()`    | No       | Named projection → column lists                         |
| `getRequiredPermissions()`| No       | Operation → permission string mapping                   |
| `getAllowedOperations()`  | No       | Which operations are supported (default: all)            |
| `getUniqueConstraints()`  | No       | Uniqueness constraints checked before CREATE (default: none) |
| `isSoftDeleteEnabled()`   | No       | Use soft delete (default: false)                         |
| `validate(op, entity)`    | No       | Custom business-rule validation                          |
| `beforeSave(op, payload)` | No       | Hook to enrich payload before persist                    |
| `afterSave(op, id, payload)` | No   | Hook after successful persist                            |

### FieldDefinition

Fluent API for defining field metadata:

```java
FieldDefinition.of("name", "column_name", String.class)
    .required(true)           // mandatory on CREATE
    .requiredOnUpdate(false)  // optional on UPDATE
    .insertable(true)         // can be set on CREATE
    .updatable(true)          // can be modified on UPDATE
    .maxLength(200)           // string max length
    .minValue(0)              // numeric minimum
    .maxValue(9999)           // numeric maximum
    .pattern("^[A-Z].*")     // regex pattern
    .defaultValue("active")  // default if not provided
    .displayName("Name")     // human-readable for errors
```

### Role-Based Access Control (RBAC)

1. Each `EntityDefinition` declares required permissions per operation
2. The framework checks the current user's authorities via `CrudSecurityContext`
3. Default implementation reads from Spring Security's `SecurityContextHolder`
4. Override `CrudSecurityContext` bean for custom auth systems

### Uniqueness Constraints (Duplicate Detection)

Prevent duplicate records on CREATE by defining which field combinations must be unique. The framework queries the database before INSERT and rejects the operation with **HTTP 409 Conflict** if a matching record already exists.

**Single-field uniqueness** (e.g. email must be unique):
```java
@Override
public List<UniqueConstraint> getUniqueConstraints() {
    return List.of(
        UniqueConstraint.of("email")
            .withMessage("A customer with this email address already exists")
    );
}
```

**Composite uniqueness** (e.g. product name + category together must be unique):
```java
@Override
public List<UniqueConstraint> getUniqueConstraints() {
    return List.of(
        UniqueConstraint.of("name", "category")
            .withMessage("A product with this name already exists in this category")
    );
}
```

**Multiple constraints** on the same entity:
```java
@Override
public List<UniqueConstraint> getUniqueConstraints() {
    return List.of(
        UniqueConstraint.of("email"),
        UniqueConstraint.of("firstName", "lastName", "phone")
    );
}
```

**How it works:**
1. Before every CREATE, the framework iterates over all `UniqueConstraint` entries
2. For each constraint, it extracts the field values from the payload
3. It runs a `SELECT COUNT(*)` with parameterized WHERE conditions
4. Soft-deleted records are excluded from the check
5. If a match is found, a `DuplicateEntityException` is thrown → **409 Conflict**

**Error response example:**
```json
{
  "success": false,
  "message": "A customer with this email address already exists",
  "timestamp": "2026-02-24T14:00:00Z"
}
```

#### How Multiple Constraints Interact

Multiple `UniqueConstraint` entries in the list are **independent checks**, not OR/AND combined into a single query. Each constraint is evaluated separately, and the CREATE is rejected if **any one** of them finds a duplicate.

**Within** a single constraint, fields are combined with **AND** — all listed fields must match together:
```java
// Rejects CREATE only if BOTH name AND category match an existing record
UniqueConstraint.of("name", "category")
```

**Across** multiple constraints, each is an **independent rule** — a violation of any single constraint blocks the CREATE:
```java
return List.of(
    UniqueConstraint.of("email"),                         // Constraint 1
    UniqueConstraint.of("firstName", "lastName", "phone") // Constraint 2
);
// CREATE is rejected if:
//   - an existing record has the same email (constraint 1 violated), OR
//   - an existing record has the same firstName + lastName + phone (constraint 2 violated)
```

| Scenario | Behavior |
|---|---|
| `UniqueConstraint.of("a", "b")` | Duplicate only if **both** `a` AND `b` match an existing record |
| `List.of(UC.of("a"), UC.of("b"))` | Duplicate if `a` matches **or** `b` matches (independent checks) |

> **Note:** If a constraint field is missing from the payload (null), that constraint is skipped.
> Custom error messages are optional — the framework generates a sensible default.

### Validation Pipeline

1. **Structural validation** (automatic):
   - Required field presence
   - Max-length enforcement
   - Min/max numeric range
   - Regex pattern matching
   - Insertable/updatable enforcement
   - Unknown field detection
2. **Uniqueness validation** (automatic, CREATE only):
   - Checks all `UniqueConstraint` entries against existing records
   - Returns 409 Conflict on duplicate
3. **Custom validation** (developer-defined):
   - `EntityDefinition.validate()` receives typed entity POJO
   - Return `ValidationResult` with field-level errors

### Event System

Listen to CRUD events using Spring's `@EventListener`:

```java
@EventListener
public void onCrudEvent(CrudEvent event) {
    if (event.getPhase() == CrudEvent.Phase.POST
            && event.getOperation() == CrudOperation.CREATE) {
        // Send notification, invalidate cache, etc.
    }
}
```

### Soft Delete

Enable per entity:
```java
@Override
public boolean isSoftDeleteEnabled() { return true; }

@Override
public String getSoftDeleteColumn() { return "deleted"; }
```
- DELETE sets the flag instead of removing the row
- GET queries automatically filter out soft-deleted records

### Audit Logging

Every operation is logged with user, entity, operation, and ID. Replace `AuditService` bean to write to a DB table or external system.

### Utility Endpoints

| Endpoint                | Method | Auth     | Description                    |
|-------------------------|--------|----------|--------------------------------|
| `/api/crud`             | POST   | Required | Main CRUD endpoint             |
| `/api/crud/entities`    | GET    | Required | List registered entity types   |
| `/api/crud/health`      | GET    | Public   | Health check                   |

---

## Running the Example

```bash
# From project root
mvn clean install
cd crud-framework-example
mvn spring-boot:run
```

The example starts on port **8080** with H2 in-memory database.

**Demo users:**
| Username | Password | Permissions                          |
|----------|----------|--------------------------------------|
| admin    | admin    | All (read, write, admin)             |
| editor   | editor   | Read + write (no delete)             |
| viewer   | viewer   | Read only                            |

H2 Console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:cruddb`)

---

## Extending the Framework

### Custom Security Context
```java
@Bean
public CrudSecurityContext crudSecurityContext() {
    return new MyCustomSecurityContext(); // e.g. JWT-based
}
```

### Custom Audit Service
```java
@Service
public class DatabaseAuditService extends AuditService {
    @Override
    public void audit(String entityType, CrudOperation op, Object id, Map<String, Object> payload) {
        // Write to audit_log table
    }
}
```

### Event Listener
```java
@Component
public class NotificationListener {
    @EventListener
    public void handle(CrudEvent event) {
        if (event.getPhase() == CrudEvent.Phase.POST) {
            // Send email, push notification, etc.
        }
    }
}
```

---

## Design Decisions & Additional Considerations

| Concern                  | How It's Handled                                                 |
|--------------------------|------------------------------------------------------------------|
| SQL Injection            | All values via named parameters; identifiers are whitelist-validated |
| Unknown fields           | Rejected during validation                                       |
| Duplicate Prevention     | `UniqueConstraint` checked before INSERT; returns 409 Conflict   |
| Pagination               | `page`/`size` in request; `totalCount` in response               |
| Sorting                  | `sortBy`/`sortDirection` validated against known columns         |
| Soft Delete              | Configurable per entity; filters GET results automatically       |
| Transaction Management   | `@Transactional` on CrudService.process()                       |
| Extensibility            | beforeSave/afterSave hooks + Spring event system                 |
| Error Handling           | Consistent CrudResponse with field-level error maps              |
