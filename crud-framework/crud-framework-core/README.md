# CRUD Framework Core — Complete Specification

> **Purpose of this document:** This is the authoritative, AI-consumable specification for `crud-framework-core`.
> It contains every interface, class, method signature, enum value, builder pattern, validation rule,
> and behavioral contract. An AI assistant (or developer) reading this document should be able to
> create a fully working `EntityDefinition` implementation — including POJO, fields, permissions,
> relations, validation, hooks, schema DDL, and security configuration — **without reading any source code**.

---

## Table of Contents

- [1. Quick-Start Checklist](#1-quick-start-checklist)
- [2. Maven Dependency](#2-maven-dependency)
- [3. Architecture & Processing Pipeline](#3-architecture--processing-pipeline)
- [4. EntityDefinition Interface — Full Contract](#4-entitydefinition-interface--full-contract)
  - [4.1 Required Methods](#41-required-methods)
  - [4.2 Optional Methods with Defaults](#42-optional-methods-with-defaults)
- [5. Model Classes — Complete API Reference](#5-model-classes--complete-api-reference)
  - [5.1 FieldDefinition](#51-fielddefinition)
  - [5.2 PermissionConfig](#52-permissionconfig)
  - [5.3 FilterPermission](#53-filterpermission)
  - [5.4 ManyToManyRelation](#54-manytomanyrelation)
  - [5.5 UniqueConstraint](#55-uniqueconstraint)
  - [5.6 ValidationResult](#56-validationresult)
  - [5.7 CrudOperation (enum)](#57-crudoperation-enum)
  - [5.8 RelationOperation (enum)](#58-relationoperation-enum)
  - [5.9 IdType (enum)](#59-idtype-enum)
  - [5.10 CrudRequest](#510-crudrequest)
  - [5.11 CrudResponse](#511-crudresponse)
  - [5.12 RelationRequest](#512-relationrequest)
  - [5.13 CrudEvent](#513-crudevent)
- [6. Step-by-Step: Creating a New Entity](#6-step-by-step-creating-a-new-entity)
  - [6.1 Blank Template](#61-blank-template)
  - [6.2 Step-by-Step Instructions](#62-step-by-step-instructions)
- [7. Complete Working Examples](#7-complete-working-examples)
  - [7.1 Minimal Entity (fewest overrides)](#71-minimal-entity-fewest-overrides)
  - [7.2 Full-Featured Entity (all features)](#72-full-featured-entity-all-features)
  - [7.3 UUID Entity](#73-uuid-entity)
  - [7.4 Read-Only Entity](#74-read-only-entity)
  - [7.5 Entity with M:N Relation](#75-entity-with-mn-relation)
  - [7.6 Entity with Filter Permissions](#76-entity-with-filter-permissions)
- [8. Database Schema Requirements](#8-database-schema-requirements)
- [9. Security Configuration](#9-security-configuration)
- [10. API Endpoints & Request/Response Examples](#10-api-endpoints--requestresponse-examples)
- [11. RBAC Permission Resolution — Complete Rules](#11-rbac-permission-resolution--complete-rules)
- [12. Validation — Complete Rules](#12-validation--complete-rules)
- [13. Event System](#13-event-system)
- [14. Audit Logging](#14-audit-logging)
- [15. Exception Hierarchy](#15-exception-hierarchy)
- [16. Auto-Configuration & Overrides](#16-auto-configuration--overrides)
- [17. Design Decisions & Constraints](#17-design-decisions--constraints)
- [18. Decision Matrix — Which Features to Use](#18-decision-matrix--which-features-to-use)
- [19. Naming Conventions](#19-naming-conventions)
- [20. Common Mistakes & Troubleshooting](#20-common-mistakes--troubleshooting)

---

## 1. Quick-Start Checklist

To add a new entity to a service using this framework, complete these steps **in order**:

1. **Create the database table** — DDL in `schema.sql` (or migration)
2. **Create the EntityDefinition class** — one `@Component` class implementing `EntityDefinition<T>`
3. **Define the inner POJO class** — matching the database columns
4. **Implement 4 required methods** — `getEntityType()`, `getTableName()`, `getEntityClass()`, `getFieldDefinitions()`
5. **Override optional methods as needed** — permissions, ID type, soft delete, projections, relations, validation, hooks
6. **Add permissions to SecurityConfig** — if `PermissionConfig` is defined
7. **Add seed data** — optional, in `data.sql`
8. **Test via API** — `POST /api/crud`

---

## 2. Maven Dependency

```xml
<dependency>
    <groupId>com.framework</groupId>
    <artifactId>crud-framework-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Requirements:** Java 17+, Spring Boot 3.x, Spring JDBC, Spring Security, a JDBC-compatible database.

---

## 3. Architecture & Processing Pipeline

```
┌─────────────────────────────────────────────────────────────────────┐
│                        CrudController                               │
│     POST /api/crud          POST /api/crud/relation                 │
│     GET  /api/crud/entities GET  /api/crud/health                   │
└────────────┬───────────────────────────┬────────────────────────────┘
             │                           │
     ┌───────▼──────┐           ┌────────▼────────┐
     │  CrudService  │           │ RelationService  │
     │ (orchestrator)│           │ (M:N operations) │
     └──┬──┬──┬──┬──┘           └──┬──┬──┬────────┘
        │  │  │  │                 │  │  │
   ┌────┘  │  │  └────┐      ┌────┘  │  └────┐
   ▼       ▼  ▼       ▼      ▼       ▼       ▼
Permission Validation  Audit  Permission  Audit
Service    Service    Service  Service   Service
                │                        │
          ┌─────▼────────────────────────▼──┐
          │      DynamicCrudRepository       │
          │   (SQL generation, parameterized │
          │    queries, Spring JDBC)          │
          └──────────────┬──────────────────┘
                         │
                    ┌────▼────┐
                    │ Database │
                    └─────────┘
```

**CRUD request processing pipeline (exact order):**

1. `CrudController` receives `POST /api/crud` with `CrudRequest` JSON body
2. Validate entity type and operation are present
3. Look up `EntityDefinition` from `EntityRegistry` by `entityType`
4. Check operation is in `getAllowedOperations()`
5. `PermissionService.checkAccess()` — verify the user has the required permission
6. **For GET:** Execute query via `DynamicCrudRepository`, audit via `AuditService.auditQuery()`
7. **For CREATE/UPDATE:**
   - `ValidationService` performs structural validation (field rules)
   - `EntityDefinition.validate()` performs custom business validation
   - `UniqueConstraint` check (CREATE only) — query DB for duplicates
   - Publish `CrudEvent(phase=PRE)`
   - Call `EntityDefinition.beforeSave()`
   - Execute SQL via `DynamicCrudRepository`
   - Call `EntityDefinition.afterSave()`
   - Publish `CrudEvent(phase=POST)`
   - `AuditService.audit()`
8. **For DELETE:**
   - Publish `CrudEvent(phase=PRE)`
   - Execute DELETE (or soft-delete UPDATE)
   - Call `EntityDefinition.afterSave()`
   - Publish `CrudEvent(phase=POST)`
   - `AuditService.audit()`

---

## 4. EntityDefinition Interface — Full Contract

**Package:** `com.framework.crud.definition`

**Import:** `import com.framework.crud.definition.EntityDefinition;`

**Generic type `T`:** The POJO class used for Jackson deserialization during validation. The framework calls `objectMapper.convertValue(payloadMap, getEntityClass())` before calling `validate()`.

### 4.1 Required Methods

These 4 methods **must** be implemented. The class will not compile without them.

#### `getEntityType()`

```java
@Override
public String getEntityType() {
    return "product";
}
```

- **Return type:** `String`
- **Purpose:** Unique logical identifier for this entity, used in the `entityType` field of `CrudRequest`
- **Constraints:** Must be unique across all registered `EntityDefinition` beans in the application
- **Convention:** lowercase, singular, snake_case for multi-word (e.g., `"customer_profile"`)

#### `getTableName()`

```java
@Override
public String getTableName() {
    return "products";
}
```

- **Return type:** `String`
- **Purpose:** Physical database table name
- **Constraints:** Must match `^[a-zA-Z_][a-zA-Z0-9_]*$` (framework validates identifiers to prevent SQL injection)
- **Convention:** lowercase, plural, snake_case

#### `getEntityClass()`

```java
@Override
public Class<Product> getEntityClass() {
    return Product.class;
}
```

- **Return type:** `Class<T>`
- **Purpose:** POJO class for Jackson deserialization
- **Requirements:** Must have:
  - A public no-args constructor (or be a static inner class with a public no-args constructor)
  - Public getter and setter for every field
  - Field names must use **camelCase** and match the `fieldName` in `FieldDefinition`

#### `getFieldDefinitions()`

```java
@Override
public List<FieldDefinition> getFieldDefinitions() {
    return List.of(
        FieldDefinition.of("name", "name", String.class).required(true).maxLength(200),
        FieldDefinition.of("price", "price", Double.class).required(true).minValue(0.01)
    );
}
```

- **Return type:** `List<FieldDefinition>`
- **Purpose:** Defines every field the entity exposes (excluding the primary key `id`)
- **Critical rules:**
  - Do **NOT** include the primary key (`id`) — the framework handles it automatically
  - Every field in the payload that is not in this list is rejected as "Unknown field"
  - The `fieldName` must match the POJO's field name (camelCase)
  - The `columnName` must match the database column name (snake_case)

### 4.2 Optional Methods with Defaults

These methods have default implementations. Override only when needed.

| Method | Signature | Default | When to Override |
|---|---|---|---|
| `getIdColumn()` | `String getIdColumn()` | `"id"` | PK column has a different name |
| `getIdField()` | `String getIdField()` | `"id"` | PK field in JSON has a different name |
| `getIdType()` | `IdType getIdType()` | `IdType.AUTO_INCREMENT` | Use UUID-based primary keys |
| `isSoftDeleteEnabled()` | `boolean isSoftDeleteEnabled()` | `false` | Want logical delete instead of physical |
| `getSoftDeleteColumn()` | `String getSoftDeleteColumn()` | `"deleted"` | Soft-delete flag column has a different name |
| `getProjectionTypes()` | `Map<String, List<String>> getProjectionTypes()` | `Collections.emptyMap()` | Want named subsets of columns |
| `getPermissionConfig()` | `PermissionConfig getPermissionConfig()` | `PermissionConfig.empty()` | Want RBAC on this entity |
| `getAllowedOperations()` | `Set<CrudOperation> getAllowedOperations()` | `Set.of(CrudOperation.values())` | Restrict allowed operations |
| `getUniqueConstraints()` | `List<UniqueConstraint> getUniqueConstraints()` | `Collections.emptyList()` | Prevent duplicate records |
| `getManyToManyRelations()` | `List<ManyToManyRelation> getManyToManyRelations()` | `Collections.emptyList()` | Has M:N relationships |
| `validate(op, entity)` | `ValidationResult validate(CrudOperation operation, T entity)` | `ValidationResult.success()` | Custom business rules |
| `beforeSave(op, payload)` | `void beforeSave(CrudOperation operation, Map<String, Object> payload)` | no-op | Transform payload before persist |
| `afterSave(op, id, payload)` | `void afterSave(CrudOperation operation, Object id, Map<String, Object> payload)` | no-op | Post-persist side effects |

---

## 5. Model Classes — Complete API Reference

### 5.1 FieldDefinition

**Package:** `com.framework.crud.model`

**Import:** `import com.framework.crud.model.FieldDefinition;`

**Factory method:**

```java
FieldDefinition.of(String fieldName, String columnName, Class<?> fieldType)
```

| Parameter | Type | Description |
|---|---|---|
| `fieldName` | `String` | JSON payload key / POJO field name (camelCase) |
| `columnName` | `String` | Database column name (snake_case) |
| `fieldType` | `Class<?>` | Java type: `String.class`, `Integer.class`, `Long.class`, `Double.class`, `Boolean.class` |

**Fluent builder methods (all return `FieldDefinition` for chaining):**

| Method | Parameter | Default | Description |
|---|---|---|---|
| `.required(boolean)` | `true`/`false` | `false` | Field must be present on CREATE |
| `.requiredOnUpdate(boolean)` | `true`/`false` | `false` | Field must be present on UPDATE |
| `.insertable(boolean)` | `true`/`false` | `true` | Field is included in INSERT SQL |
| `.updatable(boolean)` | `true`/`false` | `true` | Field is included in UPDATE SQL |
| `.defaultValue(Object)` | any | `null` | Used when field is absent on INSERT |
| `.maxLength(Integer)` | positive int | `null` | Max string length (validated on CREATE & UPDATE) |
| `.minValue(Number)` | numeric | `null` | Min value for numeric fields (inclusive) |
| `.maxValue(Number)` | numeric | `null` | Max value for numeric fields (inclusive) |
| `.pattern(String)` | regex | `null` | Regex the string value must match |
| `.displayName(String)` | text | `fieldName` | Human-readable name in error messages |

**Complete example with all options:**

```java
FieldDefinition.of("email", "email", String.class)
    .required(true)
    .requiredOnUpdate(false)
    .insertable(true)
    .updatable(true)
    .defaultValue(null)
    .maxLength(255)
    .minValue(null)
    .maxValue(null)
    .pattern("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    .displayName("Email Address")
```

**Supported `fieldType` values and their validation behavior:**

| Java Type | DB Column Type | Validations Applied |
|---|---|---|
| `String.class` | `VARCHAR(n)` | `maxLength`, `pattern` |
| `Integer.class` | `INT` | `minValue`, `maxValue` |
| `Long.class` | `BIGINT` | `minValue`, `maxValue` |
| `Double.class` | `DECIMAL(p,s)` | `minValue`, `maxValue` |
| `Boolean.class` | `BOOLEAN` | none (type check only) |

---

### 5.2 PermissionConfig

**Package:** `com.framework.crud.model`

**Import:** `import com.framework.crud.model.PermissionConfig;`

**Factory methods:**

```java
// Empty config — no permissions required for any operation
PermissionConfig.empty()

// Builder pattern
PermissionConfig.builder()
    .listPermission("ListProduct")       // GET without ID
    .getPermission("GetProduct")         // GET with ID
    .createPermission("CreateProduct")   // CREATE
    .updatePermission("UpdateProduct")   // UPDATE
    .deletePermission("DeleteProduct")   // DELETE
    .filterPermission(FilterPermission)  // Add one filter permission
    .filterPermissions(List<FilterPermission>)  // Add multiple filter permissions
    .build()
```

**Builder methods — all return `Builder` for chaining:**

| Method | Parameter | Description |
|---|---|---|
| `.listPermission(String)` | permission name | Required for GET without ID (list) |
| `.getPermission(String)` | permission name | Required for GET with ID (single record) |
| `.createPermission(String)` | permission name | Required for CREATE |
| `.updatePermission(String)` | permission name | Required for UPDATE |
| `.deletePermission(String)` | permission name | Required for DELETE |
| `.filterPermission(FilterPermission)` | single filter permission | Add one filter-based permission |
| `.filterPermissions(List<FilterPermission>)` | list | Add multiple filter-based permissions at once |

**Key behaviors:**

- If a permission string is `null` or blank → that operation has **no permission check** (open access)
- `PermissionConfig.empty()` → all operations open, all filtering allowed
- `hasPermissions()` returns `true` if **any** permission or filter permission is defined
- When `hasPermissions()` is `true`, filtering is **strictly controlled** (see [Section 11](#11-rbac-permission-resolution--complete-rules))

**Instance methods:**

| Method | Return Type | Description |
|---|---|---|
| `getListPermission()` | `String` | List permission name (may be null) |
| `getGetPermission()` | `String` | Get permission name (may be null) |
| `getCreatePermission()` | `String` | Create permission name (may be null) |
| `getUpdatePermission()` | `String` | Update permission name (may be null) |
| `getDeletePermission()` | `String` | Delete permission name (may be null) |
| `getFilterPermissions()` | `List<FilterPermission>` | Unmodifiable list of filter permissions |
| `hasPermissions()` | `boolean` | True if any permission or filter permission is defined |

---

### 5.3 FilterPermission

**Package:** `com.framework.crud.model`

**Import:** `import com.framework.crud.model.FilterPermission;`

**Factory method:**

```java
FilterPermission.of(Set<String> filterFields, String permission)
```

| Parameter | Type | Description |
|---|---|---|
| `filterFields` | `Set<String>` | Set of filter field names that trigger this permission. Must not be empty. These are JSON field names (camelCase), NOT column names. |
| `permission` | `String` | The named permission required when this filter combination is used. Must not be blank. |

**Chaining method:**

```java
FilterPermission.of(Set.of("customer_id"), "ListCustomerOrders")
    .description("List orders for a specific customer")
```

| Method | Parameter | Description |
|---|---|---|
| `.description(String)` | text | Optional human-readable description |

**Instance methods:**

| Method | Return Type | Description |
|---|---|---|
| `getFilterFields()` | `Set<String>` | Unmodifiable set of filter field names |
| `getPermission()` | `String` | The permission string |
| `getDescription()` | `String` | Optional description (may be null) |
| `matches(Set<String> requestFilterKeys)` | `boolean` | True if ALL filter fields are present in the request keys |
| `specificity()` | `int` | Number of filter fields (used for ranking — more = more specific) |

**Matching algorithm:**

1. For each `FilterPermission`, check if `requestFilterKeys.containsAll(filterFields)` → that is a "match"
2. A match means the request contains **at least** all the fields in the permission (superset OK)
3. When multiple permissions match, the one with the **highest `specificity()`** wins
4. If no permissions match and the entity has permissions defined → **ACCESS DENIED**
5. If the entity has permissions but zero FilterPermissions configured → **ACCESS DENIED** (no filtering allowed)

---

### 5.4 ManyToManyRelation

**Package:** `com.framework.crud.model`

**Import:** `import com.framework.crud.model.ManyToManyRelation;`

**Builder pattern (no public constructor):**

```java
ManyToManyRelation.builder()
    .relationName("tags")                  // Required — logical name used in RelationRequest
    .targetEntityType("tag")               // Required — must match another EntityDefinition's entityType
    .junctionTable("product_tags")         // Required — physical junction table name
    .sourceJoinColumn("product_id")        // Required — FK column pointing to THIS entity's PK
    .targetJoinColumn("tag_id")            // Required — FK column pointing to TARGET entity's PK
    .getPermission("ListProductTags")      // Optional — permission for GET (listing related)
    .addPermission("AddTagToProduct")      // Optional — permission for ADD
    .removePermission("RemoveTagFromProduct") // Optional — permission for REMOVE
    .build()
```

**Builder methods — all return `Builder` for chaining:**

| Method | Parameter | Required | Description |
|---|---|---|---|
| `.relationName(String)` | name | **Yes** | Logical name used in `RelationRequest.relationName` |
| `.targetEntityType(String)` | entity type | **Yes** | Must match a registered `EntityDefinition.getEntityType()` |
| `.junctionTable(String)` | table name | **Yes** | Physical junction/pivot table name |
| `.sourceJoinColumn(String)` | column name | **Yes** | FK in junction table → this entity's PK |
| `.targetJoinColumn(String)` | column name | **Yes** | FK in junction table → target entity's PK |
| `.getPermission(String)` | permission | No | Permission for GET/LIST related entities |
| `.addPermission(String)` | permission | No | Permission for ADD associations |
| `.removePermission(String)` | permission | No | Permission for REMOVE associations |

**Validation:** `build()` throws `IllegalArgumentException` if any required field is null or blank.

**Instance methods (getters only):**

| Method | Return Type |
|---|---|
| `getRelationName()` | `String` |
| `getTargetEntityType()` | `String` |
| `getJunctionTable()` | `String` |
| `getSourceJoinColumn()` | `String` |
| `getTargetJoinColumn()` | `String` |
| `getGetPermission()` | `String` (nullable) |
| `getAddPermission()` | `String` (nullable) |
| `getRemovePermission()` | `String` (nullable) |

**Bidirectional relations:** Both sides can define the relation separately. For example, `ProductEntityDefinition` defines `tags` relation (product → tag), and `TagEntityDefinition` defines `products` relation (tag → product), both pointing to the **same** junction table but with swapped source/target join columns.

---

### 5.5 UniqueConstraint

**Package:** `com.framework.crud.model`

**Import:** `import com.framework.crud.model.UniqueConstraint;`

**Factory method:**

```java
UniqueConstraint.of(String... fieldNames)
```

| Parameter | Type | Description |
|---|---|---|
| `fieldNames` | `String...` | One or more **JSON field names** (not column names) that must be unique together |

**Chaining method:**

```java
UniqueConstraint.of("name", "category")
    .withMessage("A product with this name already exists in this category")
```

| Method | Parameter | Description |
|---|---|---|
| `.withMessage(String)` | custom message | Error message when duplicate detected. If not set, a default is generated. |

**Instance methods:**

| Method | Return Type | Description |
|---|---|---|
| `getFieldNames()` | `List<String>` | Unmodifiable list of field names |
| `getMessage()` | `String` | Custom message (may be null) |
| `getEffectiveMessage()` | `String` | Returns custom message, or generates default if null |

**Behavior:**
- Checked **before INSERT** only (not on UPDATE)
- Framework queries DB: `SELECT COUNT(*) FROM table WHERE field1 = :v1 AND field2 = :v2`
- If count > 0 → throws `DuplicateEntityException` (HTTP 409)
- Default messages:
  - Single field: `"A record with this {fieldName} already exists"`
  - Multiple fields: `"A record with this combination of [{f1}, {f2}] already exists"`

---

### 5.6 ValidationResult

**Package:** `com.framework.crud.model`

**Import:** `import com.framework.crud.model.ValidationResult;`

**Factory methods:**

```java
// No errors — validation passed
ValidationResult.success()

// Pre-built error map
ValidationResult.failure(Map.of("field", "error message"))

// Builder mode — add errors incrementally
ValidationResult.builder()
    .addError("price", "Price must be positive")
    .addError("status", "Invalid status value")
```

**Instance methods:**

| Method | Return Type | Description |
|---|---|---|
| `.addError(String field, String message)` | `ValidationResult` | Add a field error. Automatically marks result as invalid. Returns `this` for chaining. |
| `.merge(ValidationResult other)` | `ValidationResult` | Merge another result's errors into this one. Returns `this`. |
| `.isValid()` | `boolean` | True if no errors were added |
| `.getErrors()` | `Map<String, String>` | Unmodifiable map of field → error message |

**Usage pattern in `validate()` method:**

```java
@Override
public ValidationResult validate(CrudOperation operation, Product entity) {
    ValidationResult result = ValidationResult.builder();

    // Always start with builder()
    // Add errors conditionally
    if ("discontinued".equals(entity.getStatus()) && entity.getPrice() > 500) {
        result.addError("price", "Discontinued products cannot cost more than 500");
    }

    // Return the result — framework checks isValid()
    return result;
}
```

**Important:** Always use `ValidationResult.builder()` in the `validate()` method (not `success()`), so you can add errors conditionally. If no errors are added, `isValid()` returns `true` and the framework treats it as success.

---

### 5.7 CrudOperation (enum)

**Package:** `com.framework.crud.model`

**Import:** `import com.framework.crud.model.CrudOperation;`

```java
public enum CrudOperation {
    GET,
    CREATE,
    UPDATE,
    DELETE
}
```

**Static method:**

```java
CrudOperation.fromString(String value)  // Case-insensitive parse. Throws IllegalArgumentException on invalid.
```

**Used in:**
- `CrudRequest.operation` — determines what CRUD operation to execute
- `EntityDefinition.getAllowedOperations()` — restrict which operations are permitted
- `EntityDefinition.validate(CrudOperation, T)` — first parameter
- `EntityDefinition.beforeSave(CrudOperation, Map)` — first parameter
- `EntityDefinition.afterSave(CrudOperation, Object, Map)` — first parameter

---

### 5.8 RelationOperation (enum)

**Package:** `com.framework.crud.model`

**Import:** `import com.framework.crud.model.RelationOperation;`

```java
public enum RelationOperation {
    GET,      // Query related entities via JOIN
    ADD,      // Insert rows into junction table
    REMOVE    // Delete rows from junction table
}
```

**Static method:**

```java
RelationOperation.fromString(String value)  // Case-insensitive. Returns GET when input is null/blank.
```

---

### 5.9 IdType (enum)

**Package:** `com.framework.crud.model`

**Import:** `import com.framework.crud.model.IdType;`

```java
public enum IdType {
    AUTO_INCREMENT,  // DB auto-generates numeric ID (BIGINT). Framework reads back via KeyHolder.
    UUID             // Framework generates UUID.randomUUID() before INSERT. Column: VARCHAR(36).
}
```

**Usage:**

```java
@Override
public IdType getIdType() {
    return IdType.UUID;
}
```

**Impact on database schema:**
- `AUTO_INCREMENT` → `id BIGINT AUTO_INCREMENT PRIMARY KEY`
- `UUID` → `id VARCHAR(36) NOT NULL PRIMARY KEY`

**Impact on POJO:**
- `AUTO_INCREMENT` → `private Long id;`
- `UUID` → `private java.util.UUID id;` (or `private String id;`)

---

### 5.10 CrudRequest

**Package:** `com.framework.crud.model`

JSON body for `POST /api/crud`:

```json
{
  "entityType":     "product",          // String  — Required
  "operation":      "GET",              // String  — Required (GET|CREATE|UPDATE|DELETE)
  "id":             1,                  // Object  — Required for GET-by-ID, UPDATE, DELETE
  "payload":        { "key": "value" }, // Map     — Required for CREATE, UPDATE
  "filters":        { "key": "value" }, // Map     — Optional, for GET list operations
  "projectionType": "summary",          // String  — Optional, named column set
  "sortBy":         "name",             // String  — Optional, column name
  "sortDirection":  "ASC",              // String  — Optional, ASC or DESC (default: ASC)
  "page":           0,                  // Integer — Optional, 0-based page number
  "size":           20                  // Integer — Optional, page size (default: 20)
}
```

---

### 5.11 CrudResponse

**Package:** `com.framework.crud.model`

Uses `@JsonInclude(NON_NULL)` — only non-null fields appear.

| Field | Type | Present When |
|---|---|---|
| `success` | `boolean` | Always |
| `message` | `String` | Always |
| `data` | `Map<String, Object>` | Single-record responses (GET-by-ID, CREATE, UPDATE) |
| `dataList` | `List<Map<String, Object>>` | List responses (GET without ID) |
| `totalCount` | `Long` | List responses (total matching records) |
| `page` | `Integer` | List responses |
| `size` | `Integer` | List responses |
| `errors` | `Map<String, String>` | Validation failures (field → message) |
| `timestamp` | `Instant` | Always (auto-set) |

---

### 5.12 RelationRequest

**Package:** `com.framework.crud.model`

JSON body for `POST /api/crud/relation`:

```json
{
  "entityType":     "product",         // String       — Required, source entity
  "relationName":   "tags",            // String       — Required, matches getManyToManyRelations().relationName
  "id":             1,                 // Object       — Required, source entity PK
  "operation":      "GET",             // String       — Optional (GET|ADD|REMOVE), default: GET
  "relatedIds":     [3, 5, 7],         // List<Object> — Required for ADD/REMOVE
  "filters":        { "t.name": "X" }, // Map          — Optional, filter target entity
  "projectionType": "summary",         // String       — Optional, project target entity columns
  "sortBy":         "t.name",          // String       — Optional
  "sortDirection":  "ASC",             // String       — Optional
  "page":           0,                 // Integer      — Optional
  "size":           20                 // Integer      — Optional
}
```

**Note on `t.` prefix:** When filtering or sorting related entities in a relation query, use the `t.` prefix for target entity column/field names (e.g., `"t.name"`, `"t.color"`). This prefix is used internally as a table alias.

---

### 5.13 CrudEvent

**Package:** `com.framework.crud.event`

**Import:** `import com.framework.crud.event.CrudEvent;`

```java
public class CrudEvent {
    public enum Phase { PRE, POST }

    // Fields (all via getters)
    String entityType;
    CrudOperation operation;
    Phase phase;
    Object id;                    // null for CREATE/PRE
    Map<String, Object> payload;  // null for DELETE
    String username;
    Instant timestamp;
}
```

---

## 6. Step-by-Step: Creating a New Entity

### 6.1 Blank Template

Copy this template, replace all `TODO` comments:

```java
package com.example.crud.definition;  // TODO: your package

import com.framework.crud.definition.EntityDefinition;
import com.framework.crud.model.CrudOperation;
import com.framework.crud.model.FieldDefinition;
import com.framework.crud.model.FilterPermission;
import com.framework.crud.model.IdType;
import com.framework.crud.model.ManyToManyRelation;
import com.framework.crud.model.PermissionConfig;
import com.framework.crud.model.UniqueConstraint;
import com.framework.crud.model.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class TodoEntityDefinition implements EntityDefinition<TodoEntityDefinition.Todo> {
    // TODO: Rename class to match your entity (e.g., InvoiceEntityDefinition)

    // ================================================================
    // STEP 1: Define the entity POJO (inner class)
    // ================================================================
    // Fields must use camelCase names matching the fieldName in getFieldDefinitions().
    // Must have public getters & setters. Must have a no-args constructor.

    public static class Todo {
        // TODO: Change to java.util.UUID if using IdType.UUID
        private Long id;

        // TODO: Add all entity fields here
        private String title;
        private String status;

        // TODO: Generate getters & setters for ALL fields including id
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    // ================================================================
    // STEP 2: Implement required methods
    // ================================================================

    @Override
    public String getEntityType() {
        return "todo";  // TODO: unique entity name (lowercase, singular)
    }

    @Override
    public String getTableName() {
        return "todos";  // TODO: database table name (lowercase, plural)
    }

    @Override
    public Class<Todo> getEntityClass() {
        return Todo.class;  // TODO: match your POJO class
    }

    @Override
    public List<FieldDefinition> getFieldDefinitions() {
        // TODO: Define ALL fields (DO NOT include the "id" primary key field)
        return List.of(
            FieldDefinition.of("title", "title", String.class)
                .required(true)
                .maxLength(200)
                .displayName("Title"),

            FieldDefinition.of("status", "status", String.class)
                .required(true)
                .pattern("^(open|in_progress|done)$")
                .defaultValue("open")
                .displayName("Status")
        );
    }

    // ================================================================
    // STEP 3: Override optional methods as needed (delete what you don't need)
    // ================================================================

    // --- Uncomment if using UUID primary keys ---
    // @Override
    // public IdType getIdType() { return IdType.UUID; }

    // --- Uncomment if using soft delete ---
    // @Override
    // public boolean isSoftDeleteEnabled() { return true; }
    // @Override
    // public String getSoftDeleteColumn() { return "deleted"; }

    // --- Uncomment to define projections ---
    // @Override
    // public Map<String, List<String>> getProjectionTypes() {
    //     return Map.of(
    //         "summary", List.of("id", "title", "status"),
    //         "full",    List.of("id", "title", "status", "created_at")
    //     );
    // }

    // --- Uncomment to restrict allowed operations ---
    // @Override
    // public Set<CrudOperation> getAllowedOperations() {
    //     return Set.of(CrudOperation.GET, CrudOperation.CREATE);  // read + create only
    // }

    // --- Uncomment to add RBAC permissions ---
    // @Override
    // public PermissionConfig getPermissionConfig() {
    //     return PermissionConfig.builder()
    //         .listPermission("ListTodo")
    //         .getPermission("GetTodo")
    //         .createPermission("CreateTodo")
    //         .updatePermission("UpdateTodo")
    //         .deletePermission("DeleteTodo")
    //         // .filterPermission(FilterPermission.of(Set.of("status"), "ListTodosByStatus"))
    //         .build();
    // }

    // --- Uncomment to add uniqueness constraints ---
    // @Override
    // public List<UniqueConstraint> getUniqueConstraints() {
    //     return List.of(
    //         UniqueConstraint.of("title").withMessage("A todo with this title already exists")
    //     );
    // }

    // --- Uncomment to add M:N relations ---
    // @Override
    // public List<ManyToManyRelation> getManyToManyRelations() {
    //     return List.of(
    //         ManyToManyRelation.builder()
    //             .relationName("labels")
    //             .targetEntityType("label")
    //             .junctionTable("todo_labels")
    //             .sourceJoinColumn("todo_id")
    //             .targetJoinColumn("label_id")
    //             .getPermission("ListTodoLabels")
    //             .addPermission("AddLabelToTodo")
    //             .removePermission("RemoveLabelFromTodo")
    //             .build()
    //     );
    // }

    // --- Uncomment to add custom validation ---
    // @Override
    // public ValidationResult validate(CrudOperation operation, Todo entity) {
    //     ValidationResult result = ValidationResult.builder();
    //     // Add business rules here
    //     return result;
    // }

    // --- Uncomment to add lifecycle hooks ---
    // @Override
    // public void beforeSave(CrudOperation operation, Map<String, Object> payload) {
    //     // Transform payload before database operation
    // }
    //
    // @Override
    // public void afterSave(CrudOperation operation, Object id, Map<String, Object> payload) {
    //     // Post-persist side effects
    // }
}
```

### 6.2 Step-by-Step Instructions

**STEP 1: Design your database table**

```sql
CREATE TABLE IF NOT EXISTS todos (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,   -- or VARCHAR(36) for UUID
    title       VARCHAR(200) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'open',
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Rules:
- Primary key column **must** match `getIdColumn()` (default: `"id"`)
- Column names use `snake_case`
- For soft delete: add `deleted BOOLEAN NOT NULL DEFAULT FALSE`
- For M:N: create a junction table with composite PK and foreign keys
- Add indexes on commonly filtered columns

**STEP 2: Create the `@Component` class**

- Must be annotated with `@Component` (or registered as a `@Bean`)
- Must implement `EntityDefinition<YourPojo>`
- Convention: name it `{Entity}EntityDefinition` (e.g., `InvoiceEntityDefinition`)

**STEP 3: Define the inner POJO class**

- Use a `public static class` inside the definition
- Field names in **camelCase** matching `FieldDefinition.fieldName`
- Include the `id` field with the correct type:
  - `Long id` for `AUTO_INCREMENT`
  - `java.util.UUID id` (or `String id`) for `UUID`
- Public getter and setter for **every** field

**STEP 4: Implement the 4 required methods**

See [Section 4.1](#41-required-methods). In `getFieldDefinitions()`:
- Do **NOT** include the primary key field
- Use `FieldDefinition.of(fieldName, columnName, type)` for each field
- Set `.required(true)` for NOT NULL columns without defaults
- Set `.pattern(regex)` for enum-like columns
- Set `.defaultValue(value)` for columns with DB defaults you want the framework to supply
- Set `.maxLength(n)` matching the column's VARCHAR length

**STEP 5: Override optional methods**

Based on your requirements (see [Decision Matrix](#18-decision-matrix--which-features-to-use)).

**STEP 6: Register permissions in SecurityConfig (if RBAC is used)**

```java
// In SecurityConfig.userDetailsService():
var admin = User.builder()
    .username("admin")
    .password(encoder.encode("password"))
    .authorities(
        "ListTodo", "GetTodo", "CreateTodo", "UpdateTodo", "DeleteTodo",
        // Add filter permissions
        "ListTodosByStatus",
        // Add relation permissions
        "ListTodoLabels", "AddLabelToTodo", "RemoveLabelFromTodo",
        "ROLE_ADMIN"
    )
    .build();
```

Every permission string used in `PermissionConfig` and `ManyToManyRelation` must be granted to the appropriate users as a Spring Security authority.

---

## 7. Complete Working Examples

### 7.1 Minimal Entity (fewest overrides)

Simplest possible entity — only implements the 4 required methods.

**SQL:**
```sql
CREATE TABLE IF NOT EXISTS notes (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    title   VARCHAR(200) NOT NULL,
    content VARCHAR(5000)
);
```

**Java:**
```java
@Component
public class NoteEntityDefinition implements EntityDefinition<NoteEntityDefinition.Note> {

    public static class Note {
        private Long id;
        private String title;
        private String content;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    @Override public String getEntityType() { return "note"; }
    @Override public String getTableName() { return "notes"; }
    @Override public Class<Note> getEntityClass() { return Note.class; }

    @Override
    public List<FieldDefinition> getFieldDefinitions() {
        return List.of(
            FieldDefinition.of("title", "title", String.class).required(true).maxLength(200),
            FieldDefinition.of("content", "content", String.class).maxLength(5000)
        );
    }
}
```

**Behavior:** All CRUD operations allowed, no permissions, no soft delete, no projections, auto-increment ID.

---

### 7.2 Full-Featured Entity (all features)

```sql
CREATE TABLE IF NOT EXISTS products (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    description     VARCHAR(2000),
    price           DECIMAL(10,2) NOT NULL,
    category        VARCHAR(100) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'active',
    stock_quantity  INT NOT NULL DEFAULT 0,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tags (
    id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    name  VARCHAR(100) NOT NULL UNIQUE,
    color VARCHAR(7)
);

CREATE TABLE IF NOT EXISTS product_tags (
    product_id BIGINT NOT NULL,
    tag_id     BIGINT NOT NULL,
    PRIMARY KEY (product_id, tag_id),
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (tag_id) REFERENCES tags(id)
);
```

```java
@Component
public class ProductEntityDefinition implements EntityDefinition<ProductEntityDefinition.Product> {

    public static class Product {
        private Long id;
        private String name;
        private String description;
        private Double price;
        private String category;
        private String status;
        private Integer stockQuantity;
        // getters & setters for ALL fields
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getStockQuantity() { return stockQuantity; }
        public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    }

    @Override public String getEntityType() { return "product"; }
    @Override public String getTableName() { return "products"; }
    @Override public Class<Product> getEntityClass() { return Product.class; }

    @Override
    public List<FieldDefinition> getFieldDefinitions() {
        return List.of(
            FieldDefinition.of("name", "name", String.class)
                .required(true).maxLength(200).displayName("Product Name"),
            FieldDefinition.of("description", "description", String.class)
                .maxLength(2000).displayName("Description"),
            FieldDefinition.of("price", "price", Double.class)
                .required(true).minValue(0.01).maxValue(999999.99).displayName("Price"),
            FieldDefinition.of("category", "category", String.class)
                .required(true).maxLength(100).displayName("Category"),
            FieldDefinition.of("status", "status", String.class)
                .required(true).pattern("^(active|inactive|discontinued)$")
                .defaultValue("active").displayName("Status"),
            FieldDefinition.of("stockQuantity", "stock_quantity", Integer.class)
                .required(true).minValue(0).displayName("Stock Quantity")
        );
    }

    @Override
    public boolean isSoftDeleteEnabled() { return true; }

    @Override
    public String getSoftDeleteColumn() { return "deleted"; }

    @Override
    public Map<String, List<String>> getProjectionTypes() {
        return Map.of(
            "summary",   List.of("id", "name", "price", "status"),
            "detail",    List.of("id", "name", "description", "price", "category", "status", "stock_quantity"),
            "inventory", List.of("id", "name", "stock_quantity", "status")
        );
    }

    @Override
    public PermissionConfig getPermissionConfig() {
        return PermissionConfig.builder()
            .listPermission("ListProduct")
            .getPermission("GetProduct")
            .createPermission("CreateProduct")
            .updatePermission("UpdateProduct")
            .deletePermission("DeleteProduct")
            .filterPermission(
                FilterPermission.of(Set.of("category"), "ListProductsByCategory")
                    .description("List products filtered by category")
            )
            .build();
    }

    @Override
    public Set<CrudOperation> getAllowedOperations() {
        return Set.of(CrudOperation.GET, CrudOperation.CREATE, CrudOperation.UPDATE, CrudOperation.DELETE);
    }

    @Override
    public List<UniqueConstraint> getUniqueConstraints() {
        return List.of(
            UniqueConstraint.of("name", "category")
                .withMessage("A product with this name already exists in this category")
        );
    }

    @Override
    public List<ManyToManyRelation> getManyToManyRelations() {
        return List.of(
            ManyToManyRelation.builder()
                .relationName("tags")
                .targetEntityType("tag")
                .junctionTable("product_tags")
                .sourceJoinColumn("product_id")
                .targetJoinColumn("tag_id")
                .getPermission("ListProductTags")
                .addPermission("AddTagToProduct")
                .removePermission("RemoveTagFromProduct")
                .build()
        );
    }

    @Override
    public ValidationResult validate(CrudOperation operation, Product entity) {
        ValidationResult result = ValidationResult.builder();
        if ("discontinued".equals(entity.getStatus()) && entity.getPrice() != null && entity.getPrice() > 500) {
            result.addError("price", "Discontinued products cannot have a price greater than 500");
        }
        if ("discontinued".equals(entity.getStatus())
                && entity.getStockQuantity() != null && entity.getStockQuantity() > 0) {
            result.addError("stockQuantity", "Discontinued products must have zero stock");
        }
        return result;
    }

    @Override
    public void beforeSave(CrudOperation operation, Map<String, Object> payload) {
        if (payload.containsKey("name") && payload.get("name") instanceof String name) {
            payload.put("name", name.trim());
        }
    }
}
```

**Related permissions to add to SecurityConfig:**
```
ListProduct, GetProduct, CreateProduct, UpdateProduct, DeleteProduct,
ListProductsByCategory,
ListProductTags, AddTagToProduct, RemoveTagFromProduct
```

---

### 7.3 UUID Entity

```sql
CREATE TABLE IF NOT EXISTS customers (
    id         VARCHAR(36) NOT NULL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    status     VARCHAR(20) NOT NULL DEFAULT 'active'
);
```

```java
@Component
public class CustomerEntityDefinition implements EntityDefinition<CustomerEntityDefinition.Customer> {

    public static class Customer {
        private java.util.UUID id;    // UUID type for the PK field
        private String firstName;
        private String lastName;
        private String email;
        private String status;
        // getters & setters
        public java.util.UUID getId() { return id; }
        public void setId(java.util.UUID id) { this.id = id; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    @Override public String getEntityType() { return "customer"; }
    @Override public String getTableName() { return "customers"; }
    @Override public Class<Customer> getEntityClass() { return Customer.class; }

    @Override
    public IdType getIdType() {
        return IdType.UUID;    // <-- This is the key override
    }

    @Override
    public List<FieldDefinition> getFieldDefinitions() {
        return List.of(
            FieldDefinition.of("firstName", "first_name", String.class).required(true).maxLength(100),
            FieldDefinition.of("lastName", "last_name", String.class).required(true).maxLength(100),
            FieldDefinition.of("email", "email", String.class).required(true).maxLength(255)
                .pattern("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"),
            FieldDefinition.of("status", "status", String.class).required(true)
                .pattern("^(active|inactive|suspended)$").defaultValue("active")
        );
    }

    @Override
    public List<UniqueConstraint> getUniqueConstraints() {
        return List.of(UniqueConstraint.of("email").withMessage("A customer with this email already exists"));
    }

    @Override
    public PermissionConfig getPermissionConfig() {
        return PermissionConfig.builder()
            .listPermission("ListCustomer").getPermission("GetCustomer")
            .createPermission("CreateCustomer").updatePermission("UpdateCustomer")
            .deletePermission("DeleteCustomer")
            .build();
    }
}
```

---

### 7.4 Read-Only Entity

```java
@Component
public class AuditLogEntityDefinition implements EntityDefinition<AuditLogEntityDefinition.AuditLog> {

    public static class AuditLog {
        private Long id;
        private String action;
        private String entityType;
        private String username;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getEntityType() { return entityType; }
        public void setEntityType(String entityType) { this.entityType = entityType; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }

    @Override public String getEntityType() { return "audit_log"; }
    @Override public String getTableName() { return "audit_logs"; }
    @Override public Class<AuditLog> getEntityClass() { return AuditLog.class; }

    @Override
    public Set<CrudOperation> getAllowedOperations() {
        return Set.of(CrudOperation.GET);   // <-- READ-ONLY
    }

    @Override
    public List<FieldDefinition> getFieldDefinitions() {
        return List.of(
            FieldDefinition.of("action", "action", String.class),
            FieldDefinition.of("entityType", "entity_type", String.class),
            FieldDefinition.of("username", "username", String.class)
        );
    }
}
```

---

### 7.5 Entity with M:N Relation

Both sides of the relation need their own `EntityDefinition`. The junction table is shared.

**Side A: Product → Tags**
```java
@Override
public List<ManyToManyRelation> getManyToManyRelations() {
    return List.of(
        ManyToManyRelation.builder()
            .relationName("tags")                 // used in RelationRequest
            .targetEntityType("tag")              // must match TagEntityDefinition.getEntityType()
            .junctionTable("product_tags")        // shared junction table
            .sourceJoinColumn("product_id")       // FK to THIS entity (product)
            .targetJoinColumn("tag_id")           // FK to TARGET entity (tag)
            .getPermission("ListProductTags")
            .addPermission("AddTagToProduct")
            .removePermission("RemoveTagFromProduct")
            .build()
    );
}
```

**Side B: Tag → Products** (reverse direction, same junction table)
```java
@Override
public List<ManyToManyRelation> getManyToManyRelations() {
    return List.of(
        ManyToManyRelation.builder()
            .relationName("products")             // different name for the reverse side
            .targetEntityType("product")          // points back to product
            .junctionTable("product_tags")        // SAME junction table
            .sourceJoinColumn("tag_id")           // SWAPPED — FK to THIS entity (tag)
            .targetJoinColumn("product_id")       // SWAPPED — FK to TARGET entity (product)
            .getPermission("ListTaggedProducts")
            .addPermission("AssociateProductWithTag")
            .removePermission("DetachProductFromTag")
            .build()
    );
}
```

**Junction table DDL:**
```sql
CREATE TABLE IF NOT EXISTS product_tags (
    product_id BIGINT NOT NULL,
    tag_id     BIGINT NOT NULL,
    PRIMARY KEY (product_id, tag_id),
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (tag_id) REFERENCES tags(id)
);
```

---

### 7.6 Entity with Filter Permissions

```java
@Override
public PermissionConfig getPermissionConfig() {
    return PermissionConfig.builder()
        .listPermission("ListOrder")
        .getPermission("GetOrder")
        .createPermission("CreateOrder")
        .updatePermission("UpdateOrder")
        .deletePermission("DeleteOrder")
        // Allow filtering by customer_id with a specific permission
        .filterPermission(
            FilterPermission.of(Set.of("customer_id"), "ListCustomerOrders")
                .description("List orders for a specific customer")
        )
        // Allow filtering by customer_id + status (composite, more specific)
        .filterPermission(
            FilterPermission.of(Set.of("customer_id", "status"), "ListCustomerOrdersByStatus")
                .description("List orders for a customer filtered by status")
        )
        .build();
}
```

**Permission resolution for this config:**

| Request Filters | Matched FilterPermission | Required Permission |
|---|---|---|
| _(none)_ | _(none)_ | `ListOrder` |
| `{"customer_id": "abc"}` | `Set.of("customer_id")` | `ListCustomerOrders` |
| `{"customer_id": "abc", "status": "pending"}` | `Set.of("customer_id", "status")` (higher specificity) | `ListCustomerOrdersByStatus` |
| `{"status": "pending"}` | _(no match)_ | **ACCESS DENIED** |

---

## 8. Database Schema Requirements

### Rules for Table Design

| Requirement | Details |
|---|---|
| **Primary key column** | Must match `getIdColumn()` (default: `"id"`) |
| **AUTO_INCREMENT PK** | `id BIGINT AUTO_INCREMENT PRIMARY KEY` |
| **UUID PK** | `id VARCHAR(36) NOT NULL PRIMARY KEY` |
| **Column names** | Must match `FieldDefinition.columnName`. Use snake_case. |
| **Identifier format** | Must match `^[a-zA-Z_][a-zA-Z0-9_]*$` |
| **Soft delete column** | `deleted BOOLEAN NOT NULL DEFAULT FALSE` (name must match `getSoftDeleteColumn()`) |
| **Timestamp columns** | Optional `created_at TIMESTAMP`, `updated_at TIMESTAMP` — framework does NOT manage these, use DB defaults |
| **Junction tables** | Composite PK of both FK columns. Both columns must reference the appropriate entity tables. |

### Template: Standard Table

```sql
CREATE TABLE IF NOT EXISTS {table_name} (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    -- entity fields here --
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Template: Table with Soft Delete

```sql
CREATE TABLE IF NOT EXISTS {table_name} (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    -- entity fields here --
    deleted     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Template: UUID Table

```sql
CREATE TABLE IF NOT EXISTS {table_name} (
    id          VARCHAR(36) NOT NULL PRIMARY KEY,
    -- entity fields here --
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Template: Junction Table

```sql
CREATE TABLE IF NOT EXISTS {source}_{target} (
    {source}_id  {source_pk_type} NOT NULL,
    {target}_id  {target_pk_type} NOT NULL,
    PRIMARY KEY ({source}_id, {target}_id),
    FOREIGN KEY ({source}_id) REFERENCES {source_table}(id),
    FOREIGN KEY ({target}_id) REFERENCES {target_table}(id)
);
```

---

## 9. Security Configuration

When `PermissionConfig` is defined on an entity, the corresponding permission strings must be granted to users as Spring Security authorities.

### Template: SecurityConfig

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/crud/health").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> {});
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        var admin = User.builder()
            .username("admin")
            .password(encoder.encode("admin"))
            .authorities(
                // {Entity} CRUD permissions
                "List{Entity}", "Get{Entity}", "Create{Entity}", "Update{Entity}", "Delete{Entity}",
                // Filter permissions
                "ListFilteredPermissionName",
                // Relation permissions
                "List{Source}{Target}s", "Add{Target}To{Source}", "Remove{Target}From{Source}",
                "ROLE_ADMIN"
            )
            .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### Permission Naming Convention

```
{Action}{EntityName}
```

| Action | Used For | Example |
|---|---|---|
| `List` | GET without ID | `ListProduct` |
| `Get` | GET with ID | `GetProduct` |
| `Create` | CREATE | `CreateProduct` |
| `Update` | UPDATE | `UpdateProduct` |
| `Delete` | DELETE | `DeleteProduct` |
| `List{Filter}` | Filter-specific | `ListCustomerOrders` |
| `List{Source}{Target}s` | Relation GET | `ListProductTags` |
| `Add{Target}To{Source}` | Relation ADD | `AddTagToProduct` |
| `Remove{Target}From{Source}` | Relation REMOVE | `RemoveTagFromProduct` |

### Role Hierarchy Example

| Role | Permissions |
|---|---|
| `ROLE_ADMIN` | All CRUD + all filter + all relation permissions |
| `ROLE_EDITOR` | List, Get, Create, Update + filter + relation GET/ADD (no Delete, no REMOVE) |
| `ROLE_VIEWER` | List, Get + filter + relation GET only |

---

## 10. API Endpoints & Request/Response Examples

### POST /api/crud — All CRUD Operations

**List all:**
```json
// Request
{ "entityType": "product", "operation": "GET" }

// Response (200)
{
  "success": true,
  "message": "Entities retrieved successfully",
  "dataList": [
    { "id": 1, "name": "Laptop", "price": 999.99, "category": "Electronics", "status": "active", "stock_quantity": 50 },
    { "id": 2, "name": "Pen", "price": 1.99, "category": "Office", "status": "active", "stock_quantity": 1000 }
  ],
  "totalCount": 2,
  "page": 0,
  "size": 20,
  "timestamp": "2026-02-25T10:30:00Z"
}
```

**Get by ID:**
```json
// Request
{ "entityType": "product", "operation": "GET", "id": 1 }

// Response (200)
{
  "success": true,
  "message": "Entity retrieved successfully",
  "data": { "id": 1, "name": "Laptop", "price": 999.99, "category": "Electronics", "status": "active", "stock_quantity": 50 },
  "timestamp": "2026-02-25T10:30:00Z"
}
```

**List with filters, sort, pagination, projection:**
```json
// Request
{
  "entityType": "product",
  "operation": "GET",
  "filters": { "category": "Electronics" },
  "projectionType": "summary",
  "sortBy": "price",
  "sortDirection": "DESC",
  "page": 0,
  "size": 10
}

// Response (200)
{
  "success": true,
  "message": "Entities retrieved successfully",
  "dataList": [
    { "id": 1, "name": "Laptop", "price": 999.99, "status": "active" }
  ],
  "totalCount": 1,
  "page": 0,
  "size": 10,
  "timestamp": "2026-02-25T10:30:00Z"
}
```

**Create:**
```json
// Request
{
  "entityType": "product",
  "operation": "CREATE",
  "payload": { "name": "Tablet", "price": 499.99, "category": "Electronics", "status": "active", "stockQuantity": 25 }
}

// Response (200)
{
  "success": true,
  "message": "Entity created successfully",
  "data": { "id": 3, "name": "Tablet", "price": 499.99, "category": "Electronics", "status": "active", "stock_quantity": 25 },
  "timestamp": "2026-02-25T10:30:00Z"
}
```

**Update (partial):**
```json
// Request — only send fields you want to change
{
  "entityType": "product",
  "operation": "UPDATE",
  "id": 3,
  "payload": { "price": 449.99 }
}

// Response (200)
{
  "success": true,
  "message": "Entity updated successfully",
  "data": { "id": 3, "name": "Tablet", "price": 449.99, "category": "Electronics", "status": "active", "stock_quantity": 25 },
  "timestamp": "2026-02-25T10:30:00Z"
}
```

**Delete:**
```json
// Request
{ "entityType": "product", "operation": "DELETE", "id": 3 }

// Response (200)
{
  "success": true,
  "message": "Entity deleted successfully",
  "timestamp": "2026-02-25T10:30:00Z"
}
```

### POST /api/crud/relation — M:N Operations

**Get related entities:**
```json
// Request — get all tags for product #1
{ "entityType": "product", "relationName": "tags", "id": 1 }

// Response (200)
{
  "success": true,
  "message": "Related entities retrieved",
  "dataList": [
    { "id": 1, "name": "electronics", "color": "#3498DB" },
    { "id": 2, "name": "premium", "color": "#E74C3C" }
  ],
  "totalCount": 2,
  "page": 0,
  "size": 20,
  "timestamp": "2026-02-25T10:30:00Z"
}
```

**Add associations:**
```json
// Request
{ "entityType": "product", "relationName": "tags", "id": 1, "operation": "ADD", "relatedIds": [3, 5] }

// Response (200)
{ "success": true, "message": "2 associations added successfully", "timestamp": "2026-02-25T10:30:00Z" }
```

**Remove associations:**
```json
// Request
{ "entityType": "product", "relationName": "tags", "id": 1, "operation": "REMOVE", "relatedIds": [3] }

// Response (200)
{ "success": true, "message": "1 associations removed successfully", "timestamp": "2026-02-25T10:30:00Z" }
```

### GET /api/crud/entities — List Registered Entities

Returns metadata about all registered entity definitions.

### GET /api/crud/health — Health Check

Returns framework status. Publicly accessible (no auth).

---

## 11. RBAC Permission Resolution — Complete Rules

### For CRUD Operations (`POST /api/crud`)

```
Step 1: entity = registry.lookup(request.entityType)
Step 2: config = entity.getPermissionConfig()
Step 3: IF config == PermissionConfig.empty() (hasPermissions() == false)
            → NO permission check → ALLOW
Step 4: SWITCH on request.operation:

    CASE CREATE → permission = config.getCreatePermission()
    CASE UPDATE → permission = config.getUpdatePermission()
    CASE DELETE → permission = config.getDeletePermission()
    CASE GET:
        IF request.id != null
            → permission = config.getGetPermission()
        ELSE (LIST operation):
            IF request.filters is empty or null
                → permission = config.getListPermission()
            ELSE (has filters):
                IF config.hasPermissions() == false
                    → permission = config.getListPermission()    // backward compat
                ELSE IF config.getFilterPermissions() is empty
                    → THROW AccessDeniedException
                       ("No filter combinations are configured for entity '{type}'")
                ELSE
                    matchingPermissions = filterPermissions where matches(filterKeys) == true
                    IF matchingPermissions is empty
                        → THROW AccessDeniedException
                           ("Only configured filter combinations are permitted for entity '{type}'")
                    ELSE
                        bestMatch = max by specificity()
                        permission = bestMatch.getPermission()

Step 5: IF permission is null or blank → ALLOW (no check needed)
Step 6: IF user.hasPermission(permission) → ALLOW
Step 7: ELSE → THROW AccessDeniedException
```

### For Relation Operations (`POST /api/crud/relation`)

```
Step 1: entity = registry.lookup(request.entityType)
Step 2: relation = entity.getManyToManyRelations().find(r -> r.name == request.relationName)
Step 3: SWITCH on request.operation:
    CASE GET    → permission = relation.getGetPermission()
    CASE ADD    → permission = relation.getAddPermission()
    CASE REMOVE → permission = relation.getRemovePermission()
Step 4: IF permission is null or blank → ALLOW
Step 5: IF user.hasPermission(permission) → ALLOW
Step 6: ELSE → THROW AccessDeniedException
```

### Security Context Interface

```java
public interface CrudSecurityContext {
    String getCurrentUsername();
    Set<String> getCurrentRoles();
    Set<String> getCurrentPermissions();
    boolean hasPermission(String permission);
    boolean hasAnyRole(String... roles);
}
```

Default implementation `SpringSecurityCrudContext` reads from `SecurityContextHolder`. Override by registering your own `CrudSecurityContext` bean.

---

## 12. Validation — Complete Rules

### Structural Validation (automatic, on CREATE and UPDATE)

The framework applies these checks in order for each field in the payload:

1. **Unknown field check** — if payload contains a key not in `getFieldDefinitions()` → error
2. **Insertable check** (CREATE) — if field has `insertable=false` → error
3. **Updatable check** (UPDATE) — if field has `updatable=false` → error
4. **Required check** (CREATE) — if `required=true` and field is missing from payload and no `defaultValue` → error
5. **RequiredOnUpdate check** (UPDATE) — if `requiredOnUpdate=true` and field is missing → error
6. **Type check** — value must be convertible to `fieldType`
7. **Max length** — if `maxLength != null` and value is a String longer than `maxLength` → error
8. **Min value** — if `minValue != null` and numeric value < `minValue` → error
9. **Max value** — if `maxValue != null` and numeric value > `maxValue` → error
10. **Pattern** — if `pattern != null` and String value does not match regex → error

### Default Value Application

If a field has `defaultValue` set and the field is **not present** in the CREATE payload, the framework inserts the default value into the payload before SQL execution.

### Custom Validation

After structural validation passes, the framework:
1. Converts the payload `Map<String, Object>` to the POJO `T` via Jackson `ObjectMapper`
2. Calls `entityDefinition.validate(operation, entity)`
3. Merges the returned `ValidationResult` errors with any structural errors
4. If any errors exist → returns HTTP 400 with the error map

---

## 13. Event System

The framework publishes `CrudEvent` via Spring's `ApplicationEventPublisher` for every CRUD operation.

**Two events per write operation:**
- `CrudEvent(phase=PRE)` — before database operation (after validation)
- `CrudEvent(phase=POST)` — after database operation

**Listen to events:**

```java
@Component
public class MyEventListener {

    @EventListener
    public void onCrudEvent(CrudEvent event) {
        if ("product".equals(event.getEntityType())
                && event.getPhase() == CrudEvent.Phase.POST
                && event.getOperation() == CrudOperation.CREATE) {
            // New product created — trigger notification, cache invalidation, etc.
        }
    }
}
```

---

## 14. Audit Logging

All operations are logged automatically by `AuditService` at INFO level.

**Four dedicated methods:**

| Method | Signature | Covers |
|---|---|---|
| `audit` | `audit(entityType, operation, id, payload)` | CREATE, UPDATE, DELETE |
| `auditQuery` | `auditQuery(entityType, id, filters, projectionType, sortBy, sortDirection, page, size)` | GET (list & single) |
| `auditRelationQuery` | `auditRelationQuery(sourceEntityType, relationName, targetEntityType, sourceId, filters, projectionType, sortBy, sortDirection, page, size)` | Relation GET |
| `auditRelationMutation` | `auditRelationMutation(sourceEntityType, relationName, targetEntityType, operation, sourceId, relatedIds, affectedCount)` | Relation ADD/REMOVE |

**Override:** Provide your own `AuditService` bean to customize (e.g., persist to database).

---

## 15. Exception Hierarchy

| Exception Class | HTTP Status | When Thrown |
|---|---|---|
| `CrudValidationException` | 400 Bad Request | Payload fails structural/custom validation |
| `UnsupportedEntityException` | 400 Bad Request | Entity type not registered, or operation not in `getAllowedOperations()` |
| `CrudAccessDeniedException` | 403 Forbidden | User lacks required permission or filter not allowed |
| `EntityNotFoundException` | 404 Not Found | GET/UPDATE/DELETE with ID that doesn't exist |
| `DuplicateEntityException` | 409 Conflict | `UniqueConstraint` violated on CREATE |
| `CrudException` | 500 Internal Server Error | General framework error |

All exceptions extend `CrudException`. They are handled by `GlobalExceptionHandler` which returns a structured JSON error response.

**Error response format:**
```json
{
  "success": false,
  "message": "Access denied on entity 'product': operation CREATE requires permission 'CreateProduct'",
  "timestamp": "2026-02-25T10:30:00Z"
}
```

**Validation error response format:**
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "name": "Product Name is required",
    "price": "Price must be at least 0.01"
  },
  "timestamp": "2026-02-25T10:30:00Z"
}
```

---

## 16. Auto-Configuration & Overrides

The framework uses Spring Boot auto-configuration via `CrudAutoConfiguration`:

- **Component scan:** `@ComponentScan(basePackages = "com.framework.crud")` — all framework beans are auto-registered
- **Auto-config import:** Registered in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- **Entity discovery:** All `@Component` classes implementing `EntityDefinition` are automatically discovered and registered

### Overridable Beans

| Bean | Default | Override By |
|---|---|---|
| `CrudSecurityContext` | `SpringSecurityCrudContext` (reads from Spring Security) | Declare your own `CrudSecurityContext` bean |
| `AuditService` | Logs to SLF4J at INFO level | Declare your own `AuditService` bean |

---

## 17. Design Decisions & Constraints

| Decision | Rationale |
|---|---|
| Single `POST /api/crud` endpoint | Simplifies client — one URL, operation in body |
| Configuration over code | Entity defs are pure configuration. No controllers/repos/SQL to write |
| Spring JDBC (not JPA) | Full SQL control, no entity scanning overhead, lighter footprint |
| Named parameters | All values via `NamedParameterJdbcTemplate` — prevents SQL injection |
| Identifier sanitization | Table/column names validated against `^[a-zA-Z_][a-zA-Z0-9_]*$` |
| Partial updates | UPDATE only touches payload fields — no need to send full entity |
| Filter-permission enforcement | Entities with `PermissionConfig` require explicit `FilterPermission` for any filtering |
| Event-driven extensibility | Spring events for PRE/POST phases — decoupled cross-cutting concerns |
| Auto-configuration | Add dependency + create `EntityDefinition` beans — no manual wiring |

### Constraints

- No support for JOIN queries in CRUD operations (only M:N via relation endpoint)
- Filters are equality-only (`WHERE column = :value`) — no range, LIKE, IN operators
- No built-in `updated_at` management — use database triggers or `beforeSave()`
- Uniqueness constraints are checked on CREATE only, not UPDATE
- Pagination is offset-based (not cursor-based)
- Sort by a single column only

---

## 18. Decision Matrix — Which Features to Use

| Scenario | Features to Enable |
|---|---|
| Simple table, all operations, no auth | Only 4 required methods |
| Read-only reference data | `getAllowedOperations() = Set.of(GET)` |
| String-based UUIDs for primary key | `getIdType() = UUID`, PK column `VARCHAR(36)` |
| Logical delete (archive without removing) | `isSoftDeleteEnabled() = true`, add `deleted` column |
| Response size control (mobile vs web) | `getProjectionTypes()` with named sets |
| Prevent duplicate records | `getUniqueConstraints()` |
| Access control (who can do what) | `getPermissionConfig()` + SecurityConfig |
| Control which filters are queryable | `FilterPermission` inside `PermissionConfig` |
| Many-to-many associations | `getManyToManyRelations()` + junction table DDL |
| Cross-field business rules | `validate(operation, entity)` |
| Data transformation before save | `beforeSave(operation, payload)` |
| Side effects after save | `afterSave(operation, id, payload)` |
| React to CRUD events | `@EventListener` on `CrudEvent` |

---

## 19. Naming Conventions

| Item | Convention | Example |
|---|---|---|
| Entity type | lowercase, singular, snake_case | `"product"`, `"customer_profile"` |
| Table name | lowercase, plural, snake_case | `"products"`, `"customer_profiles"` |
| Column name | lowercase, snake_case | `"stock_quantity"`, `"first_name"` |
| Field name (POJO/JSON) | camelCase | `"stockQuantity"`, `"firstName"` |
| Definition class name | `{Entity}EntityDefinition` | `ProductEntityDefinition` |
| POJO class name | `{Entity}` (inner class) | `Product` (inside `ProductEntityDefinition`) |
| Junction table | `{source}_{target}` | `product_tags` |
| Permission: CRUD | `{Action}{Entity}` | `ListProduct`, `CreateOrder` |
| Permission: filter | Descriptive | `ListCustomerOrders`, `ListProductsByCategory` |
| Permission: relation GET | `List{Source}{Target}s` | `ListProductTags` |
| Permission: relation ADD | `Add{Target}To{Source}` | `AddTagToProduct` |
| Permission: relation REMOVE | `Remove{Target}From{Source}` | `RemoveTagFromProduct` |
| Relation name | lowercase, plural | `"tags"`, `"products"`, `"categories"` |

---

## 20. Common Mistakes & Troubleshooting

| Mistake | Symptom | Fix |
|---|---|---|
| Including `id` in `getFieldDefinitions()` | Validation errors on CREATE/UPDATE | Remove the `id` field — framework handles it automatically |
| fieldName doesn't match POJO field name | Custom validation receives null values | Ensure `FieldDefinition.fieldName` exactly matches the POJO's camelCase field name |
| columnName doesn't match DB column | SQL errors at runtime | Ensure `FieldDefinition.columnName` exactly matches the database column name |
| Using column name in filters instead of field name | Filter silently ignored | Use the camelCase `fieldName` in request filters (it's mapped to `columnName` automatically) |
| filterFields in FilterPermission use column names | FilterPermission never matches | Use JSON field names (camelCase), not column names, in `FilterPermission.of(Set.of(...))` |
| Permission string typo | 403 Forbidden on operations | Ensure exact match between `PermissionConfig` strings and `SecurityConfig` authorities |
| Missing `@Component` annotation | Entity not discovered | Add `@Component` to the class or register as `@Bean` |
| Missing POJO getters/setters | Jackson deserialization fails | Add public getter and setter for every field |
| POJO is not a static inner class | Cannot instantiate | Use `public static class` for inner POJOs |
| Soft delete without `deleted` column | SQL error on DELETE | Add `deleted BOOLEAN NOT NULL DEFAULT FALSE` to the table |
| Junction table missing | SQL error on relation operations | Create the junction table with composite PK and FKs |
| `targetEntityType` doesn't match any registered entity | Runtime error on relation query | Ensure it matches exactly the `getEntityType()` of the target entity definition |
| Using `ValidationResult.success()` in `validate()` | Cannot add errors conditionally | Use `ValidationResult.builder()` instead |
| Filtering on entity with permissions but no FilterPermissions | 403 Forbidden | Either add `FilterPermission`s or use `PermissionConfig.empty()` for open access |
