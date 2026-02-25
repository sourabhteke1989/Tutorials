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
│       │   ├── IdType.java
│       │   ├── ManyToManyRelation.java
│       │   ├── RelationRequest.java
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
│       │   ├── RelationService.java
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
        │       ├── CustomerEntityDefinition.java
        │       ├── CustomerProfileEntityDefinition.java  ← One-to-One demo
        │       ├── OrderEntityDefinition.java             ← One-to-Many demo
        │       └── TagEntityDefinition.java               ← Many-to-Many demo
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

## Querying & Filtering (No Separate Query Endpoint Needed)

The GET operation **already serves as a full query endpoint**. By combining `filters`, `sortBy`, `sortDirection`, `page`, and `size`, you can query any entity table by any registered field — no dedicated search/query operation is required.

### How It Works

When the `id` field is **omitted** from a GET request, the framework switches to **list mode** and dynamically builds SQL with:

1. **`filters`** — a `Map<String, Object>` of field/column → value pairs joined with `AND` in the `WHERE` clause
2. **`sortBy` / `sortDirection`** — mapped to `ORDER BY` (defaults to `ASC`)
3. **`page` / `size`** — mapped to `LIMIT` / `OFFSET` for pagination
4. **`projectionType`** — selects which columns are returned

```
GET by ID:   { "id": 1 }                       →  SELECT ... WHERE id = 1
GET list:    { }                                →  SELECT ... (all rows)
GET filtered:{ "filters": { "status": "active" } }  →  SELECT ... WHERE status = 'active'
```

### Filter Examples

**Single-field filter** — find all active products:

```json
{
  "entityType": "product",
  "operation": "GET",
  "filters": { "status": "active" }
}
```

**Multi-field filter (AND)** — find active Electronics products:

```json
{
  "entityType": "product",
  "operation": "GET",
  "filters": {
    "category": "Electronics",
    "status": "active"
  }
}
```

> All filter fields are combined with `AND`. The generated SQL for the request above:
> ```sql
> SELECT ... FROM products WHERE category = 'Electronics' AND status = 'active'
> ```

**Filter + Sort + Paginate** — cheapest active electronics, page by page:

```json
{
  "entityType": "product",
  "operation": "GET",
  "projectionType": "summary",
  "filters": {
    "category": "Electronics",
    "status": "active"
  },
  "sortBy": "price",
  "sortDirection": "ASC",
  "page": 0,
  "size": 10
}
```

**Response** (paginated):

```json
{
  "success": true,
  "message": "Entities retrieved successfully",
  "dataList": [
    { "id": 3, "name": "USB Cable", "price": 5.99, "category": "Electronics" },
    { "id": 1, "name": "Laptop", "price": 999.99, "category": "Electronics" }
  ],
  "totalCount": 2,
  "page": 0,
  "size": 10,
  "timestamp": "2026-02-25T10:30:00Z"
}
```

**Filter by any field** — customers by country:

```json
{
  "entityType": "customer",
  "operation": "GET",
  "filters": { "country": "USA" }
}
```

### Filter Behaviour Summary

| Scenario                          | Request Fields                          | Behaviour                                  |
|-----------------------------------|-----------------------------------------|--------------------------------------------|
| Get by ID                         | `id` present                            | Returns single entity (exact match on PK)  |
| Get all (unfiltered)              | No `id`, no `filters`                   | Returns all rows (paginated if `size` set) |
| Single-field query                | `filters: { "status": "active" }`       | `WHERE status = 'active'`                  |
| Multi-field query (AND)           | `filters: { "status": "active", "category": "Electronics" }` | `WHERE status = 'active' AND category = 'Electronics'` |
| Query + sort                      | `filters` + `sortBy` + `sortDirection`  | Adds `ORDER BY` clause                     |
| Query + paginate                  | `filters` + `page` + `size`             | Adds `LIMIT` / `OFFSET`; response includes `totalCount` |
| Query + sort + paginate + project | All fields                              | Full query with column projection          |

### Filter Security

- **Field validation**: Only fields registered in `getFieldDefinitions()` (or the ID column) are accepted. Unrecognised field names in `filters` are silently ignored — they never reach the SQL.
- **Parameterised queries**: All filter values are bound via `NamedParameterJdbcTemplate` as named parameters (`:f_status`, `:f_category`, etc.), preventing SQL injection.
- **Identifier sanitisation**: Column and table names pass through `sanitizeIdentifier()`, which rejects anything that is not `^[a-zA-Z_][a-zA-Z0-9_]*$`.
- **Soft-delete aware**: If the entity has soft delete enabled, a `deleted = false` condition is **always** appended — filtered results never include soft-deleted rows.

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
| `getIdType()`             | No       | ID strategy: `AUTO_INCREMENT` (default) or `UUID`        |
| `getProjectionTypes()`    | No       | Named projection → column lists                         |
| `getRequiredPermissions()`| No       | Operation → permission string mapping                   |
| `getAllowedOperations()`  | No       | Which operations are supported (default: all)            |
| `getUniqueConstraints()`  | No       | Uniqueness constraints checked before CREATE (default: none) |
| `getManyToManyRelations()` | No      | Many-to-many junction-table relations (default: none)        |
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

### ID Generation Strategy (Long vs UUID)

By default, the framework uses database auto-increment (`BIGINT`) for primary keys. For tables where auto-increment exhaustion is a concern or distributed IDs are needed, switch to UUID:

```java
@Override
public IdType getIdType() {
    return IdType.UUID;
}
```

**How it works:**

| Strategy | `IdType.AUTO_INCREMENT` (default) | `IdType.UUID` |
|---|---|---|
| ID column type | `BIGINT AUTO_INCREMENT` | `VARCHAR(36)` or native `UUID` |
| Who generates the ID | Database | Framework (`UUID.randomUUID()`) |
| When ID is generated | After INSERT (via KeyHolder) | Before INSERT (injected into payload) |
| ID value in response | Numeric (e.g. `1`, `42`) | UUID string (e.g. `"a1b2c3d4-..."`) |
| POJO ID field type | `Long` | `java.util.UUID` or `String` |

**Example — Customer entity with UUID:**
```java
@Component
public class CustomerEntityDefinition implements EntityDefinition<Customer> {

    public static class Customer {
        private UUID id;  // java.util.UUID — not String or Long
        // ... other fields

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
    }

    @Override
    public IdType getIdType() {
        return IdType.UUID;
    }

    // ... other methods
}
```

The POJO `id` field can be declared as either `java.util.UUID` or `String` — both work.
Jackson handles `UUID ↔ JSON string` serialization automatically.

**Database schema:**
```sql
CREATE TABLE customers (
    id VARCHAR(36) NOT NULL PRIMARY KEY,  -- UUID stored as string
    -- ... other columns
);
```

> **Note:** Both strategies work side-by-side. Product uses `AUTO_INCREMENT`, Customer uses `UUID` — each entity declares its own strategy independently.

### Entity Relationships

The framework supports all three relationship types between entities:

#### One-to-One (Customer → CustomerProfile)

If entity Y has a foreign key column referencing entity X's primary key, you can query the related Y record using the standard GET operation with `filters`. No special configuration is needed — the FK column is just another filterable field.

**Schema:**
```sql
CREATE TABLE customer_profiles (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL UNIQUE,   -- FK → customers.id
    bio         VARCHAR(1000),
    loyalty_tier VARCHAR(20) NOT NULL DEFAULT 'bronze',
    total_orders INT NOT NULL DEFAULT 0
);
```

**Entity definition** — the `customer_id` FK is registered as a normal field:
```java
FieldDefinition.of("customerId", "customer_id", String.class)
    .required(true).maxLength(36).displayName("Customer ID")
```

**Query** — get the profile for a specific customer:
```json
{
  "entityType": "customer_profile",
  "operation": "GET",
  "filters": { "customerId": "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d" }
}
```

**Generated SQL:**
```sql
SELECT * FROM customer_profiles WHERE customer_id = 'a1b2c3d4-...'
```

Since `customer_id` has a UNIQUE constraint, the result set contains at most one row — effectively a one-to-one lookup.

#### One-to-Many (Customer → Order)

The same `filters` mechanism handles one-to-many. When the FK column is **not** unique, the query returns multiple rows.

**Schema:**
```sql
CREATE TABLE orders (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id     VARCHAR(36) NOT NULL,       -- FK → customers.id
    order_number    VARCHAR(50) NOT NULL UNIQUE,
    total_amount    DECIMAL(10,2) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'pending'
);
```

**Entity definition** — again, the FK is a regular field:
```java
FieldDefinition.of("customerId", "customer_id", String.class)
    .required(true).maxLength(36).displayName("Customer ID")
```

**Query** — get all orders for a customer (paginated, sorted):
```json
{
  "entityType": "order",
  "operation": "GET",
  "filters": { "customerId": "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d" },
  "sortBy": "totalAmount",
  "sortDirection": "DESC",
  "page": 0,
  "size": 10
}
```

**Generated SQL:**
```sql
SELECT * FROM orders
WHERE customer_id = 'a1b2c3d4-...'
ORDER BY total_amount DESC
LIMIT 10 OFFSET 0
```

**Response:**
```json
{
  "success": true,
  "message": "Entities retrieved successfully",
  "dataList": [
    { "id": 1, "customer_id": "a1b2c3d4-...", "order_number": "ORD-2026-0001", "total_amount": 129.97, "status": "delivered" },
    { "id": 2, "customer_id": "a1b2c3d4-...", "order_number": "ORD-2026-0002", "total_amount": 89.99,  "status": "shipped" },
    { "id": 3, "customer_id": "a1b2c3d4-...", "order_number": "ORD-2026-0003", "total_amount": 44.99,  "status": "pending" }
  ],
  "totalCount": 3,
  "page": 0,
  "size": 10,
  "timestamp": "2026-02-25T10:30:00Z"
}
```

**Additional filters** — combine FK with other fields (AND):
```json
{
  "entityType": "order",
  "operation": "GET",
  "filters": {
    "customerId": "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d",
    "status": "delivered"
  }
}
```

> Both one-to-one and one-to-many query the target entity table directly. No special relation configuration is needed — the FK column is just another filterable field registered in `getFieldDefinitions()`.

#### Many-to-Many (New: Relation Endpoint)

When two entities are connected through a **junction table** (e.g., Products ↔ Tags via `product_tags`), a regular filter-based GET cannot traverse the JOIN. The framework provides a dedicated endpoint and configuration for this.

**Step 1 — Define the relation in `EntityDefinition`:**

```java
@Override
public List<ManyToManyRelation> getManyToManyRelations() {
    return List.of(
        ManyToManyRelation.builder()
            .relationName("tags")              // logical name used in requests
            .targetEntityType("tag")           // must match a registered entity
            .junctionTable("product_tags")     // the bridge table
            .sourceJoinColumn("product_id")    // FK in junction → this entity's PK
            .targetJoinColumn("tag_id")        // FK in junction → target entity's PK
            .build()
    );
}
```

**Step 2 — Query via `POST /api/crud/relation`:**

```json
{
  "entityType": "product",
  "relationName": "tags",
  "id": 1
}
```

**Generated SQL:**

```sql
SELECT t.id, t.name, t.color
FROM tags t
JOIN product_tags j ON t.id = j.tag_id
WHERE j.product_id = 1
```

**Response:**

```json
{
  "success": true,
  "message": "Related 'tag' entities retrieved successfully via 'tags'",
  "dataList": [
    { "id": 1, "name": "electronics", "color": "#3498DB" },
    { "id": 5, "name": "ergonomic",   "color": "#9B59B6" }
  ],
  "timestamp": "2026-02-25T10:30:00Z"
}
```

**Pagination, sorting, filtering & projections** are all supported on the relation endpoint — exactly like regular GET:

```json
{
  "entityType": "product",
  "relationName": "tags",
  "id": 1,
  "filters": { "color": "#3498DB" },
  "projectionType": "summary",
  "sortBy": "name",
  "sortDirection": "ASC",
  "page": 0,
  "size": 10
}
```

**Generated SQL (with filters + projection):**

```sql
SELECT t.id, t.name
FROM tags t
JOIN product_tags j ON t.id = j.tag_id
WHERE j.product_id = 1
  AND t.color = '#3498DB'
ORDER BY t.name ASC
LIMIT 10 OFFSET 0
```

> **How projections work:** The `projectionType` maps to the **target** entity's
> `getProjectionTypes()`. For example, if `TagEntityDefinition` defines
> `"summary" → ["id", "name"]`, then `"projectionType": "summary"` in the
> relation request will SELECT only `t.id, t.name`. When omitted or unknown,
> all columns are returned.

##### Managing Associations (ADD / REMOVE)

Beyond querying, the same endpoint supports **creating and removing** many-to-many
associations by setting the `operation` field.

**ADD — Create associations:**

```json
{
  "entityType": "product",
  "relationName": "tags",
  "id": 1,
  "operation": "ADD",
  "relatedIds": [3, 5, 7]
}
```

**Generated SQL (per relatedId):**

```sql
MERGE INTO product_tags (product_id, tag_id)
KEY (product_id, tag_id)
VALUES (1, 3)
```

> Duplicate associations are silently ignored — no error on re-adding.

**Response:**

```json
{
  "success": true,
  "message": "3 association(s) added between 'product' [id=1] and 'tag' [3, 5, 7] via 'tags'",
  "timestamp": "2026-02-25T11:00:00Z"
}
```

**REMOVE — Delete associations:**

```json
{
  "entityType": "product",
  "relationName": "tags",
  "id": 1,
  "operation": "REMOVE",
  "relatedIds": [3, 7]
}
```

**Generated SQL (per relatedId):**

```sql
DELETE FROM product_tags
WHERE product_id = 1 AND tag_id = 3
```

**Response:**

```json
{
  "success": true,
  "message": "2 association(s) removed between 'product' [id=1] and 'tag' [3, 7] via 'tags'",
  "timestamp": "2026-02-25T11:00:00Z"
}
```

**Permission model:**

| Operation | Required Permission                      |
|-----------|------------------------------------------|
| `GET`     | GET (read) on both source & target       |
| `ADD`     | CREATE (write) on the source entity      |
| `REMOVE`  | DELETE (admin) on the source entity      |
```

**Bidirectional** — define the relation on both sides:

```java
// In ProductEntityDefinition: "give me tags for this product"
ManyToManyRelation.builder()
    .relationName("tags")
    .targetEntityType("tag")
    .junctionTable("product_tags")
    .sourceJoinColumn("product_id")
    .targetJoinColumn("tag_id")
    .build()

// In TagEntityDefinition: "give me products with this tag"
ManyToManyRelation.builder()
    .relationName("products")
    .targetEntityType("product")
    .junctionTable("product_tags")
    .sourceJoinColumn("tag_id")
    .targetJoinColumn("product_id")
    .build()
```

#### Relation Request Schema

```json
{
  "entityType":     "product",       // source entity type (required)
  "relationName":   "tags",          // relation name from getManyToManyRelations() (required)
  "id":             1,               // source entity PK (required)
  "operation":      "GET",           // GET (default), ADD, or REMOVE
  "relatedIds":     [3, 5],          // target PKs — required for ADD/REMOVE
  "filters":        { "color": "#3498DB" }, // filter target entity columns (GET only)
  "projectionType": "summary",       // named projection on target entity (GET only)
  "page":           0,               // pagination (GET only)
  "size":           20,              // pagination (GET only)
  "sortBy":         "name",          // sort by target entity field (GET only)
  "sortDirection":  "ASC"            // ASC or DESC (GET only)
}
```

| Field            | Required         | Description                                                       |
|------------------|------------------|-------------------------------------------------------------------|
| `entityType`     | Yes              | Source entity type (must have the relation defined)                |
| `relationName`   | Yes              | Logical name matching a `ManyToManyRelation`                      |
| `id`             | Yes              | Primary key of the source entity                                  |
| `operation`      | No (default GET) | `GET` — query, `ADD` — create associations, `REMOVE` — delete assoc.|
| `relatedIds`     | For ADD/REMOVE   | List of target entity PKs to associate or disassociate            |
| `filters`        | No (GET only)    | Key-value map to filter target entity columns                     |
| `projectionType` | No (GET only)    | Named projection defined in the **target** entity's definition    |
| `page`           | No (GET only)    | Page number (0-based)                                             |
| `size`           | No (GET only)    | Page size; when provided, response includes `totalCount`          |
| `sortBy`         | No (GET only)    | Target entity field/column to sort by                             |
| `sortDirection`  | No (GET only)    | `ASC` (default) or `DESC`                                         |

#### Relationship Summary

| Relationship  | Read                    | Write (Create/Update)        | Endpoint                  | Special Config Needed?                 | Demo Entities                     |
|---------------|-------------------------|------------------------------|---------------------------|----------------------------------------|-----------------------------------|
| One-to-One    | GET + `filters`         | Regular CREATE/UPDATE        | `POST /api/crud`          | No — FK is just a field in the payload  | Customer → CustomerProfile        |
| One-to-Many   | GET + `filters`         | Regular CREATE/UPDATE        | `POST /api/crud`          | No — FK is just a field in the payload  | Customer → Order                  |
| Many-to-Many  | Relation GET            | Relation ADD / REMOVE        | `POST /api/crud/relation` | Yes — define relation + junction table  | Product ↔ Tag via product_tags    |

> **One-to-One / One-to-Many writes:** The FK column (e.g. `customerId`) is a
> regular field in the entity definition. Creating or updating the child entity
> with the FK value in the payload establishes or changes the association — no
> special mechanism needed.

#### Example Entity Relationship Diagram

```
┌──────────────┐     1:1 (FK)      ┌────────────────────┐
│   Customer   │─────────────────→│  CustomerProfile    │
│   (UUID PK)  │                   │  (customer_id FK)   │
└──────┬───────┘                   └────────────────────┘
       │
       │  1:N (FK)
       ▼
┌──────────────┐
│    Order     │
│(customer_id) │
└──────────────┘

┌──────────────┐    N:M (junction)  ┌──────────────┐
│   Product    │◆──────────────────◆│     Tag      │
│              │  product_tags      │              │
└──────────────┘                    └──────────────┘
```

### Audit Logging

Every operation is logged with user, entity, operation, and ID. Replace `AuditService` bean to write to a DB table or external system.

### Utility Endpoints

| Endpoint                | Method | Auth     | Description                              |
|-------------------------|--------|----------|------------------------------------------|
| `/api/crud`             | POST   | Required | Main CRUD endpoint                       |
| `/api/crud/relation`    | POST   | Required | Many-to-many relation query endpoint     |
| `/api/crud/entities`    | GET    | Required | List registered entity types             |
| `/api/crud/health`      | GET    | Public   | Health check                             |

### Exception Handling & Error Responses

The framework provides a centralized `GlobalExceptionHandler` that catches every exception and returns a consistent `CrudResponse` JSON envelope. No raw stack traces leak to the client.

#### Exception Hierarchy

All framework exceptions extend the base `CrudException` class which carries an `errorCode` alongside the message.

```
RuntimeException
 └── CrudException                   (base — errorCode + message)
      ├── CrudValidationException    (fieldErrors map)
      ├── EntityNotFoundException
      ├── CrudAccessDeniedException
      ├── DuplicateEntityException   (constraintFields + duplicateValues)
      └── UnsupportedEntityException
```

#### Error Response Reference

| Exception                    | HTTP Status        | Error Code              | When It Fires                                                                 |
|------------------------------|--------------------|-------------------------|-------------------------------------------------------------------------------|
| `CrudValidationException`    | `400 Bad Request`  | `VALIDATION_FAILED`     | Required field missing, field format invalid, business rule rejected           |
| `UnsupportedEntityException` | `400 Bad Request`  | `UNSUPPORTED_ENTITY` / `UNSUPPORTED_OPERATION` | Unknown `entityType` or an operation not allowed for the entity   |
| `IllegalArgumentException`   | `400 Bad Request`  | —                       | Malformed request body, invalid pagination params                             |
| `CrudAccessDeniedException`  | `403 Forbidden`    | `ACCESS_DENIED`         | Current user lacks the required authority/permission for the operation         |
| `EntityNotFoundException`    | `404 Not Found`    | `ENTITY_NOT_FOUND`      | GET/UPDATE/DELETE by ID when record does not exist (or is soft-deleted)        |
| `DuplicateEntityException`   | `409 Conflict`     | `DUPLICATE_ENTITY`      | CREATE violates a `UniqueConstraint` defined on the entity                    |
| `CrudException`              | `500 Internal`     | `CRUD_ERROR`            | Catch-all for any unhandled framework-level error                             |
| `Exception`                  | `500 Internal`     | —                       | Catch-all for completely unexpected errors                                    |

#### Error Response Shapes

**Standard error** (404 Not Found, 403 Forbidden, 409 Conflict, 500, etc.)

```json
{
  "success": false,
  "message": "Entity 'product' with id '99' not found",
  "timestamp": "2026-02-25T10:30:00Z"
}
```

**Validation error** (400 Bad Request — with field-level details)

```json
{
  "success": false,
  "message": "Payload validation failed",
  "errors": {
    "name": "is required",
    "price": "must be positive"
  },
  "timestamp": "2026-02-25T10:30:00Z"
}
```

**Duplicate entity error** (409 Conflict)

```json
{
  "success": false,
  "message": "A customer with the same [email] already exists",
  "timestamp": "2026-02-25T10:30:00Z"
}
```

**Unsupported entity** (400 Bad Request)

```json
{
  "success": false,
  "message": "Entity type 'invoice' is not registered",
  "timestamp": "2026-02-25T10:30:00Z"
}
```

**Unsupported operation** (400 Bad Request)

```json
{
  "success": false,
  "message": "Operation 'DELETE' is not supported for entity 'audit_log'",
  "timestamp": "2026-02-25T10:30:00Z"
}
```

**Access denied** (403 Forbidden)

```json
{
  "success": false,
  "message": "Access denied: operation 'DELETE' on entity 'product' requires permission 'PRODUCT_DELETE'",
  "timestamp": "2026-02-25T10:30:00Z"
}
```

#### Exception Evaluation Order

The `GlobalExceptionHandler` evaluates exceptions in the following order. The **first matching handler wins**, so more specific exceptions are checked before their parent types:

```
1.  DuplicateEntityException      → 409
2.  CrudValidationException       → 400
3.  EntityNotFoundException       → 404
4.  CrudAccessDeniedException     → 403
5.  UnsupportedEntityException    → 400
6.  CrudException (catch-all)     → 500
7.  IllegalArgumentException      → 400
8.  Exception (global catch-all)  → 500
```

> **Tip:** All error responses are logged server-side at `WARN` level (client errors) or `ERROR` level (server errors) with full context, so operators can correlate client responses with server logs.

#### Throwing Custom Exceptions

Developers can throw any of the framework exceptions from within `EntityDefinition` lifecycle hooks such as `validate()`, `beforeSave()`, or `afterSave()`:

```java
@Override
public void validate(MyEntity entity, String operation) {
    if ("CREATE".equals(operation) && entity.getQuantity() > 1000) {
        throw new CrudValidationException(Map.of(
            "quantity", "cannot exceed 1000 for a single order"
        ));
    }
}

@Override
public void beforeSave(MyEntity entity, String operation) {
    if (externalService.isBlocked(entity.getSku())) {
        throw new CrudException("BLOCKED_SKU",
                "SKU '" + entity.getSku() + "' is blocked by compliance");
    }
}
```

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
| Many-to-Many Relations   | `ManyToManyRelation` + `/api/crud/relation` endpoint for JOIN queries |
| ID Strategy              | `AUTO_INCREMENT` or `UUID` per entity via `getIdType()`          |
| Pagination               | `page`/`size` in request; `totalCount` in response               |
| Sorting                  | `sortBy`/`sortDirection` validated against known columns         |
| Soft Delete              | Configurable per entity; filters GET results automatically       |
| Transaction Management   | `@Transactional` on CrudService.process()                       |
| Extensibility            | beforeSave/afterSave hooks + Spring event system                 |
| Error Handling           | Consistent CrudResponse with field-level error maps              |
