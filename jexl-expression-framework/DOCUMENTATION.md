# JEXL Expression Evaluation Framework

A Spring Boot 3.4.1 application that provides a generic, high-performance JEXL expression/script evaluation engine backed by a database-driven expression store with date-based versioning and in-memory compiled caching.

---

## Architecture Overview

```
┌──────────────┐       ┌───────────────────┐       ┌─────────────────────────┐
│   REST API   │──────▶│  Evaluation       │──────▶│  JEXL Evaluation Engine  │
│  Controller  │       │  Service          │       │  (execute pre-compiled)  │
└──────────────┘       └───────────────────┘       └─────────────────────────┘
                              │                               ▲
                              ▼                               │
                       ┌───────────────────┐                  │
                       │  Expression Cache  │─── holds ───────┘
                       │  Service          │   CompiledPayrollExpression
                       └───────────────────┘   (JexlScript / JexlExpression)
                              ▲
                              │ loads + compiles at startup
                              ▼
                       ┌───────────────────┐
                       │  H2 Database      │
                       │  payroll_          │
                       │  expressions      │
                       └───────────────────┘
```

---

## How It Works (End to End)

### 1. Expression Storage (`payroll_expressions` table)

All JEXL expressions/scripts are stored in a single database table:

| Column             | Type    | Description                                                                                    |
|--------------------|---------|------------------------------------------------------------------------------------------------|
| `id`               | INT PK  | Auto-increment primary key                                                                     |
| `expression_id`    | VARCHAR | Logical identifier (e.g., `PF`, `PT_MH`, `LWF_KA`, `ESI`)                                    |
| `expression`       | CLOB    | JEXL expression or script text                                                                 |
| `effective_from`   | DATE    | Start of the validity period                                                                   |
| `effective_to`     | DATE    | End of validity (nullable — null means "currently active")                                     |
| `context_variables`| VARCHAR | Comma-separated list of expected input variable names (documentation only)                     |

Multiple rows can share the same `expression_id` with different date ranges, enabling **expression versioning** (e.g., old PF ceiling vs new PF ceiling).

### 2. Startup: Compile & Cache

On application startup (`ApplicationReadyEvent`):

1. **Load** all rows from `payroll_expressions` via JPA.
2. **Classify** each expression as a **JEXL Script** or **JEXL Expression**:
   - **Script**: contains assignments (`=`), control flow (`if`, `for`, `foreach`, `while`), semicolons (`;`), or `return` statements.
   - **Expression**: single-line evaluation with no side-effects (e.g., `SALARY * 10 / 100`).
3. **Compile** each into a `JexlScript` or `JexlExpression` object (parsed AST).
4. **Store** the compiled objects in a `ConcurrentHashMap<String, List<CompiledPayrollExpression>>`, grouped by `expression_id` and sorted by `effective_from` descending.

> **Performance benefit**: Parsing JEXL text into an AST is done only once at startup (or on cache refresh). Every subsequent evaluation reuses the pre-compiled object — eliminating repeated parsing overhead.

### 3. API Evaluation Flow

When a user calls `POST /api/expressions/evaluate`:

```
Request ──▶ Resolve todayDate (from input or system clock)
        ──▶ Lookup expression_id in cache
        ──▶ Filter by date: effective_from ≤ todayDate AND (effective_to is null OR effective_to ≥ todayDate)
        ──▶ Pick the entry with the latest effective_from
        ──▶ Execute the pre-compiled JexlScript/JexlExpression with the provided context variables
        ──▶ Return result + metadata
```

### 4. Date-Based Expression Resolution

Given multiple entries for the same `expression_id`:

| expression_id | effective_from | effective_to |
|---------------|---------------|-------------|
| PF            | 2015-01-01    | 2019-12-31  |
| PF            | 2020-01-01    | NULL        |

- `todayDate = 2018-06-01` → uses the 2015 version (old PF ceiling of ₹12,000)
- `todayDate = 2025-04-01` → uses the 2020 version (current PF ceiling of ₹15,000)
- `todayDate = 2014-06-01` → **error** (no applicable expression)

---

## Project Structure

```
src/main/java/com/example/jexl/
├── JexlExpressionFrameworkApplication.java       # Spring Boot entry point
├── controller/
│   └── ExpressionController.java                 # REST API endpoints
├── dto/
│   ├── EvaluationRequest.java                    # Request body DTO
│   └── EvaluationResponse.java                   # Response body DTO
├── engine/
│   ├── CompiledPayrollExpression.java            # Wrapper: entity + compiled JEXL object
│   └── JexlEvaluationEngine.java                 # JEXL compile + execute logic
├── entity/
│   └── PayrollExpression.java                    # JPA entity
├── exception/
│   └── GlobalExceptionHandler.java               # Error handling
├── repository/
│   └── PayrollExpressionRepository.java          # Spring Data JPA
└── service/
    ├── ExpressionCacheService.java               # In-memory compiled cache
    └── ExpressionEvaluationService.java          # Orchestration layer

src/main/resources/
├── application.properties                        # App config (H2, JPA, server port)
├── schema.sql                                    # Table DDL
└── data.sql                                      # Seed data (44 expressions)

src/test/java/com/example/jexl/
└── JexlExpressionFrameworkApplicationTests.java  # 19 integration tests
```

---

## API Reference

### Evaluate Expression

```
POST /api/expressions/evaluate
Content-Type: application/json
```

**Request body**:
```json
{
  "expressionId": "PF",
  "todayDate": "2025-04-01",
  "context": {
    "pf_applicable_monthly_earnings": 25000,
    "company_pf_wage_ceiling": 15000,
    "is_company_pf_wage_ceiling_applicable": true
  }
}
```

- `expressionId` (required): The logical expression ID.
- `todayDate` (optional): Reference date for versioning (format: `yyyy-MM-dd`). Defaults to system date.
- `context` (required): Key-value map of input variables.

**Response**:
```json
{
  "expressionId": "PF",
  "result": {
    "EMPLOYEE_PF": 1800.0,
    "EMPLOYER_PF": 1800.0
  },
  "effectiveFrom": "2020-01-01",
  "effectiveTo": null,
  "contextUsed": { ... }
}
```

### List Expression IDs

```
GET /api/expressions/list
```

Returns all available expression IDs in the cache.

### Refresh Cache

```
POST /api/expressions/cache/refresh
```

Reloads and recompiles all expressions from the database.

---

## Available Expressions

### Payroll Calculations

| Expression ID       | Description                              | Context Variables                                                                 |
|---------------------|------------------------------------------|-----------------------------------------------------------------------------------|
| `PF`                | Provident Fund (employee + employer)     | `pf_applicable_monthly_earnings`, `company_pf_wage_ceiling`, `is_company_pf_wage_ceiling_applicable` |
| `GRATUITY_YEARLY`   | Yearly gratuity for offer letter         | `monthly_basic`, `monthly_dearness_allowance`                                     |
| `GRATUITY_ACTUAL`   | Actual gratuity by service period        | `basic_salary_monthly`, `dearness_allowance_monthly`, `completed_years_of_service`, `months_in_final_year`, `gratuity_max_limit` |
| `ESI`               | Employee State Insurance                 | `ESI_WAGES`, `MONTHLY_GROSS_SALARY`, `IS_DISABLED`, `DAILY_WAGE`                  |
| `BONUS_PCT`         | Simple bonus percentage (expression)     | `MONTHLY_GROSS_SALARY`, `BONUS_PERCENTAGE`                                        |

### Professional Tax (State-wise)

| Expression ID | State           | Context Variables                           |
|---------------|-----------------|---------------------------------------------|
| `PT_MH`       | Maharashtra     | `MONTHLY_GROSS_SALARY`, `GENDER`, `MONTH`   |
| `PT_KA`       | Karnataka       | `MONTHLY_GROSS_SALARY`, `MONTH`             |
| `PT_TN`       | Tamil Nadu      | `MONTHLY_GROSS_SALARY`                      |
| `PT_WB`       | West Bengal     | `MONTHLY_GROSS_SALARY`                      |
| `PT_GJ`       | Gujarat         | `MONTHLY_GROSS_SALARY`                      |
| `PT_AP`       | Andhra Pradesh  | `MONTHLY_GROSS_SALARY`                      |
| `PT_TS`       | Telangana       | `MONTHLY_GROSS_SALARY`                      |
| `PT_KL`       | Kerala          | `MONTHLY_GROSS_SALARY`                      |
| `PT_MP`       | Madhya Pradesh  | `MONTHLY_GROSS_SALARY`                      |
| `PT_OD`       | Odisha          | `MONTHLY_GROSS_SALARY`                      |
| `PT_JH`       | Jharkhand       | `MONTHLY_GROSS_SALARY`                      |
| `PT_AS`       | Assam           | `MONTHLY_GROSS_SALARY`                      |
| `PT_ML`       | Meghalaya       | `MONTHLY_GROSS_SALARY`                      |
| `PT_MN`       | Manipur         | `MONTHLY_GROSS_SALARY`                      |
| `PT_TR`       | Tripura         | `MONTHLY_GROSS_SALARY`                      |
| `PT_MZ`       | Mizoram         | `MONTHLY_GROSS_SALARY`                      |
| `PT_SK`       | Sikkim          | `MONTHLY_GROSS_SALARY`                      |
| `PT_BR`       | Bihar           | `MONTHLY_GROSS_SALARY`                      |
| `PT_CG`       | Chhattisgarh    | `MONTHLY_GROSS_SALARY`                      |
| `PT_PY`       | Puducherry      | `MONTHLY_GROSS_SALARY`                      |
| `PT_GA`       | Goa             | `MONTHLY_GROSS_SALARY`                      |

### Labour Welfare Fund (State-wise)

| Expression ID | State           | Frequency                | Context Variables |
|---------------|-----------------|--------------------------|-------------------|
| `LWF_MH`      | Maharashtra     | Half-yearly (Jun, Dec)   | `MONTH`           |
| `LWF_GJ`      | Gujarat         | Monthly                  | —                 |
| `LWF_KA`      | Karnataka       | Half-yearly (Jun, Dec)   | `MONTH`           |
| `LWF_TN`      | Tamil Nadu      | Half-yearly (Jan, Jul)   | `MONTH`           |
| `LWF_AP`      | Andhra Pradesh  | Monthly                  | —                 |
| `LWF_TS`      | Telangana       | Monthly                  | —                 |
| `LWF_KL`      | Kerala          | Monthly                  | —                 |
| `LWF_MP`      | Madhya Pradesh  | Half-yearly (Jun, Dec)   | `MONTH`           |
| `LWF_PB`      | Punjab          | Monthly                  | —                 |
| `LWF_HR`      | Haryana         | Monthly                  | —                 |
| `LWF_RJ`      | Rajasthan       | Half-yearly (Jun, Dec)   | `MONTH`           |
| `LWF_GA`      | Goa             | Half-yearly (Jun, Dec)   | `MONTH`           |
| `LWF_CH`      | Chandigarh      | Monthly                  | —                 |
| `LWF_BR`      | Bihar           | Annual (Jan)             | `MONTH`           |
| `LWF_AS`      | Assam           | Half-yearly (Jun, Dec)   | `MONTH`           |
| `LWF_PY`      | Puducherry      | Annual (Jan)             | `MONTH`           |

---

## JEXL Script vs Expression

The engine automatically detects whether stored text is a **script** or an **expression**:

| Feature              | JEXL Expression                    | JEXL Script                                      |
|----------------------|------------------------------------|--------------------------------------------------|
| Syntax               | Single evaluation                  | Multi-statement with control flow                 |
| Assignments          | Not supported                      | `x = 10; y = x * 2;`                             |
| Control flow         | Not supported                      | `if`, `for`, `foreach`, `while`                   |
| Return               | Implicit (result of expression)    | Last expression value or explicit `return`        |
| Example              | `SALARY * 10 / 100`               | `pt = 0; if (income > 10000) { pt = 200; } pt;`  |
| Compiled as          | `JexlExpression`                   | `JexlScript`                                      |

**Detection heuristic** (after stripping comments and string literals):
- Contains `;` → script
- Contains `if`, `for`, `foreach`, `while`, `var`, or `return` → script
- Contains assignment `=` (but not `==`, `!=`, `<=`, `>=`) → script
- Otherwise → expression

---

## Compiled Expression Cache (Performance Design)

### Problem
Creating `JexlScript` / `JexlExpression` objects involves parsing JEXL source text into an AST. Doing this on every API call wastes CPU.

### Solution
```
Startup:
  DB rows ──▶ JexlEngine.createScript() / createExpression() ──▶ CompiledPayrollExpression
                                                                    ├── PayrollExpression (entity metadata)
                                                                    ├── Object (compiled JexlScript or JexlExpression)
                                                                    └── boolean isScript

Evaluation:
  CompiledPayrollExpression ──▶ JexlScript.execute(context)      // no parsing
                            or  JexlExpression.evaluate(context)  // no parsing
```

The `ConcurrentHashMap<String, List<CompiledPayrollExpression>>` is keyed by `expression_id`. Each list is sorted by `effective_from DESC` for fast date-based lookup.

---

## Running the Application

### Prerequisites
- Java 17+
- Maven 3.8+

### Build & Test
```bash
mvn clean test
```

### Run
```bash
mvn spring-boot:run
```

The app starts on `http://localhost:8080`. The H2 console is available at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:payrolldb`, user: `sa`, no password).

### Test Scripts

**PowerShell** (Windows):
```powershell
.\test-api.ps1
```

**Bash** (Linux/Mac):
```bash
bash test-api.sh
```

Both scripts run 23 end-to-end API tests covering all expression types, date-based versioning, and cache refresh.

---

## Adding a New Expression

1. Insert a row into `payroll_expressions` (via `data.sql` or direct SQL):
   ```sql
   INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables)
   VALUES ('HRA', 'hra = BASIC_SALARY * HRA_PERCENTAGE / 100; { "HRA": hra }', '2025-01-01', NULL, 'BASIC_SALARY,HRA_PERCENTAGE');
   ```

2. Call `POST /api/expressions/cache/refresh` to recompile the cache.

3. Evaluate:
   ```json
   {
     "expressionId": "HRA",
     "context": { "BASIC_SALARY": 30000, "HRA_PERCENTAGE": 40 }
   }
   ```
