# API Gateway — Requirements Document

> **Purpose:** This document captures the functional and non-functional requirements for the API Gateway component. It is implementation-agnostic and intended to serve as the specification for a fresh, high-quality build.

---

## 1. Purpose & Scope

The API Gateway is the **single entry point** for all external client requests (browser, mobile, third-party) to the YourOrg multi-tenant SaaS platform. It sits in front of all backend services and UI applications.

**In scope:**
- Request routing to backend services, UI applications, and the company website
- Authentication (JWT token validation)
- Endpoint access control (public vs. private whitelist)
- Request enrichment (identity header injection)
- Multi-tenant host-based routing
- Cross-Origin Resource Sharing (CORS)
- HTTP-to-HTTPS redirection
- Support for both microservices and monolith deployment topologies
- Rate limiting and throttling
- Centralized monitoring, logging, and observability
- Resilience and fault tolerance (circuit breaking, retries, fallbacks)

**Out of scope:**
- Authorization / permission checks (handled by downstream services)
- Request/response transformation beyond header injection
- Service discovery (routes are statically configured)

---

## 2. Multi-Tenancy Model

### REQ-MT-001: Subdomain-Based Tenant Identification
The gateway must identify the tenant (organization) from the request hostname. Each tenant accesses the platform via a subdomain: `{tenantId}.yourorg.cloud`, `{tenantId}.yourorg.com`, etc.

### REQ-MT-002: Configurable Allowed Host Domains
The list of allowed base domains (e.g., `yourorg.cloud`, `yourorg.com`, `yourorg.org`, `yourorg.net`) must be externally configurable and loaded at startup.

### REQ-MT-003: Host-Based Route Matching
- **Tenant routes** (backend services and UI applications) must match only requests whose hostname follows the pattern `{tenantId}.{allowedHost}`.
- **Company website routes** must match only requests whose hostname is a bare allowed host (no subdomain).

### REQ-MT-004: Tenant ID Extraction
The `tenantId` segment extracted from the subdomain must be injected as the request header `X-Tenant-ID` into all downstream backend service requests.

---

## 3. Routing

### 3.1 General Routing Requirements

#### REQ-RT-001: Externalized Route Configuration
All routing definitions must be loaded from an external JSON configuration file at startup. The file path must be configurable via an application property / environment variable.

#### REQ-RT-002: Deployment Mode Support
The gateway must support at least two deployment topologies through separate configuration files:
1. **Microservices mode** — each backend service runs independently on its own host/port.
2. **Monolith mode** — all backend services are served by a single monolith application at one host/port.

Switching between modes must require only a configuration file change, with no code modifications.

#### REQ-RT-003: Route Configuration Structure
The route configuration must define:
- A list of allowed host domains
- A company website endpoint URL
- A landing application endpoint URL
- A map of backend services (service key → downstream URL)
- A map of UI applications (application key → downstream URL)

### 3.2 Backend Service Routes

#### REQ-RT-010: Backend Service URL Pattern
Backend service requests must follow the pattern: `/{serviceKey}/api/**`
- `{serviceKey}` is the identifier of the target backend service (e.g., `user-mgmt`, `emp-mgmt`, `payroll-mgmt`).
- The `/api/` segment distinguishes API calls from UI or website traffic.

#### REQ-RT-011: Path Rewriting for Backend Services
The gateway must strip the `/{serviceKey}/api` prefix before forwarding to the downstream service.
- Example: `/emp-mgmt/api/onboarding/start` → downstream receives `/onboarding/start`.

#### REQ-RT-012: Backend Service Header Injection
For every backend service request, the gateway must inject:
| Header | Value |
|---|---|
| `X-Tenant-ID` | Tenant ID extracted from the request subdomain |
| `X-Service-ID` | The `{serviceKey}` identifying the target service |

#### REQ-RT-013: Authentication Filter on Backend Routes
Every backend service route must have the Authentication Filter applied (see Section 4).

#### REQ-RT-014: Company Website Backend Access
Backend service API endpoints must also be accessible from the company website's bare domain (without `{tenantId}` subdomain). These routes must:
- Match requests from allowed hosts (bare domain) with path `/{serviceKey}/api/**`
- Inject the `X-Service-ID` header
- Apply the Authentication Filter
- NOT inject `X-Tenant-ID` from the subdomain (it does not exist on bare domains)

### 3.3 UI Application Routes

#### REQ-RT-020: UI Application URL Pattern
UI application requests must follow the pattern: `/{appKey}/**`
- `{appKey}` is the identifier of the target UI application (e.g., `user-mgmt`, `emp-mgmt`, `payroll-mgmt`).

#### REQ-RT-021: UI Application Header Injection
For every UI application request, the gateway must inject:
| Header | Value |
|---|---|
| `X-Tenant-ID` | Tenant ID extracted from the request subdomain |
| `X-Environment` | A default environment code (e.g., `production`, `staging`) |
| `X-Application-ID` | The `{appKey}` identifying the UI application |

#### REQ-RT-022: No Authentication on UI Routes
UI application routes must NOT apply the Authentication Filter. UI applications serve static assets; authentication is handled at the API level.

#### REQ-RT-023: Landing Application Fallback
A catch-all route for the pattern `/**` on tenant subdomains must route to the configured landing application. This is the lowest-priority tenant route and serves as the default UI.

### 3.4 Company Website Route

#### REQ-RT-030: Company Website Catch-All
Requests to the bare allowed host domains (no subdomain) that do not match any backend service pattern must be routed to the company website endpoint.

#### REQ-RT-031: Company Website Path Rewriting
The gateway must apply path rewriting on company website routes using the same strip-first-segment approach.

---

## 4. Authentication

### 4.1 Authentication Filter

#### REQ-AU-001: Filter Applicability
The Authentication Filter must execute on every backend service API route. It must NOT execute on UI application or company website routes.

#### REQ-AU-002: Service Recognition
The filter must read the `X-Service-ID` header (injected during routing) and look up the security configuration for that service. If the service code is not recognized in the security configuration, the request must be rejected with HTTP `403 Forbidden` and a descriptive error message.

#### REQ-AU-003: Public Endpoint Bypass
If the request path and HTTP method match a configured **public endpoint** for the identified service, the request must be forwarded downstream without any authentication.

#### REQ-AU-004: Token Extraction
For non-public endpoints, the filter must extract the JWT access token from the `Authorization` header using the `Bearer` scheme. If the header is missing or does not contain a `Bearer` token, the request must be rejected with HTTP `400 Bad Request`.

#### REQ-AU-005: Authentication Mode Header
Every request to a private endpoint must include the header `X-Auth-Mode` indicating the authentication provider active for the tenant. Accepted values are:
- `internal` — YourOrg-managed JWT authentication
- `external` — Delegated to the tenant's external identity provider (SSO/OIDC)

If this header is absent or contains an unrecognized value, the request must be rejected with HTTP `400 Bad Request` and a clear error message listing accepted values.

#### REQ-AU-006: Internal JWT Token Validation
When `X-Auth-Mode: internal`:
- The filter must validate the JWT token's signature, expiry, issuer, and audience.
- The filter must extract the following standard and custom claims: `sub` (user ID), `name` (display name), `phone_number` (mobile), `tenant_id` (tenant identifier).
- If the token is invalid, expired, or tampered with, the request must be rejected with HTTP `401 Unauthorized`.
- The gateway must never log or expose the raw token value.

#### REQ-AU-007: External Authentication
When `X-Auth-Mode: external`, the gateway must support delegating token validation to the tenant's configured external identity provider (OIDC/OAuth 2.0). Until this is fully implemented, such requests must be rejected with HTTP `501 Not Implemented` and a message indicating the feature is unavailable.

#### REQ-AU-008: Tenant ID Cross-Validation
After successful token validation, the `tenant_id` claim from the token must be compared against the `X-Tenant-ID` header (derived from the request subdomain). If they do not match, the request must be rejected with HTTP `401 Unauthorized`. This is the primary defence against cross-tenant data access and must never be bypassed.

#### REQ-AU-009: Private Endpoint Whitelist Check
After authentication, the filter must verify that the request path and HTTP method match a configured **private endpoint** for the identified service. If there is no match, the request must be rejected with HTTP `403 Forbidden`. This ensures only explicitly whitelisted endpoints are accessible.

#### REQ-AU-010: Request Enrichment After Authentication
Upon successful authentication and authorization, the filter must mutate the downstream request by injecting the following verified identity headers. The downstream service must treat these as trusted, gateway-asserted values:
| Header | Source | Value |
|---|---|---|
| `X-User-ID` | JWT `sub` claim | UUID of the authenticated user |
| `X-User-Name` | JWT `name` claim | Display name (empty string if absent) |
| `X-User-Phone` | JWT `phone_number` claim | Mobile number (empty string if absent) |
| `X-Tenant-ID` | JWT `tenant_id` claim | Verified tenant identifier (replaces subdomain-derived value) |
| `X-Internal-Request` | Gateway constant | `false` — distinguishes external from internal service-to-service calls |

The gateway must strip any of these headers from the original incoming request before performing validation, to prevent clients from spoofing identity headers.

#### REQ-AU-011: Error Response Format
All authentication/authorization failures must return:
- An appropriate HTTP status code (see REQ-EH-001)
- A structured JSON error body as defined in REQ-EH-002
- The `X-Correlation-ID` response header containing the request's correlation ID

The filter must set the `Content-Type: application/json` response header on all error responses.

### 4.2 Endpoint Matching

#### REQ-AU-020: URL Template Pattern Matching
Endpoint URLs in the security configuration may contain path parameter placeholders (e.g., `/user/{resourceId}/groups`). The gateway must convert these to regex patterns:
- `{paramName}` → matches one or more characters excluding `/`
- The full URL must be anchored (exact match from start to end)

#### REQ-AU-021: Method-Aware Matching
Endpoint matching must consider both the URL path AND the HTTP method. A `GET /user/list` must not match a `POST /user/list` entry.

#### REQ-AU-022: Precompiled Patterns
URL patterns must be compiled into regex at startup (when the configuration is loaded) and reused for every request. Patterns must not be recompiled per-request.

---

## 5. Security Configuration

#### REQ-SC-001: Externalized Security Configuration
All endpoint access rules must be loaded from an external JSON configuration file at startup. The file path must be configurable via an application property / environment variable.

#### REQ-SC-002: Per-Service Endpoint Registry
The security configuration must be organized by service key. Each service entry must define:
- **Public endpoints**: list of `{url, method}` pairs accessible without authentication
- **Private endpoints**: list of `{url, method}` pairs accessible only with a valid JWT

#### REQ-SC-003: Deny-by-Default
Any endpoint NOT listed in either the public or private endpoint list for its service must be denied access (HTTP 403). The gateway operates on a whitelist model.

> **CORS preflight exception:** HTTP `OPTIONS` preflight requests must always be allowed through without authentication, regardless of the endpoint whitelist. The CORS preflight response must not contain any authentication challenge headers.

#### REQ-SC-004: Supported Services
The security configuration must support endpoint definitions for at minimum the following services:
- `user-mgmt` — User management and authentication
- `org-setting` — Organization settings and registration
- `document-store` — Document storage and retrieval
- `emp-mgmt` — Employee management, onboarding, leave, attendance, resignation
- `payroll-mgmt` — Payroll processing, tax, compensation, payslips, reports

#### REQ-SC-005: Common Cross-Cutting Endpoints
All services must expose the following public endpoints:
- `GET /crud/health` — Health check
- `GET /crud/entities` — Entity metadata

All services must support the following private endpoints:
- `POST /crud` — Generic CRUD operations
- `POST /crud/relation` — Relation CRUD operations
- `POST /crud/file-upload-request` — File upload token request
- `POST /crud/file-download-request` — File download token request

---

## 6. JWT Token Management

#### REQ-JW-001: Configurable JWT Properties
The following JWT properties must be externally configurable:
| Property | Description | Security Constraint |
|---|---|---|
| Secret Key | Signing/verification key | **Minimum 256 bits (32 bytes) for HMAC-SHA256; minimum 2048 bits for RSA.** Must be injected via environment variable or secrets vault — never hardcoded. |
| Expiration | Token validity duration in milliseconds | Recommended maximum: 900000 (15 minutes) for access tokens |
| Issuer | Expected `iss` claim value | Must exactly match the claim; reject on mismatch |
| Audience | Expected `aud` claim value | Must exactly match the claim; reject on mismatch |

> **Security note:** Short-lived tokens (15 min) reduce the blast radius of a compromised token. The gateway must not implement token blocklisting — revocation must be handled at the issuing service level.

#### REQ-JW-002: Pluggable JWT Implementation
The JWT validation engine must be abstracted behind an interface, allowing the underlying library to be swapped without changing the gateway logic. At minimum, two implementations must be supported:
1. JJWT-based implementation
2. Nimbus JOSE+JWT-based implementation

The active implementation must be selectable via a configuration property.

#### REQ-JW-003: Token Validation Contract
Token validation must verify:
- The token signature is valid (using the configured secret key)
- The token has not expired (`exp` claim)
- The token is not used before its not-before time (`nbf` claim, if present)
- The issuer claim (`iss`) matches the configured issuer
- The audience claim (`aud`) matches the configured audience

On success, it must return a structured object containing the following fields mapped from JWT claims:
| Structured Field | JWT Claim | Description |
|---|---|---|
| `userId` | `sub` | Unique user identifier |
| `userName` | `name` | Display name |
| `phoneNumber` | `phone_number` | Mobile number |
| `tenantId` | `tenant_id` | Tenant (organization) identifier |

The interface must throw a typed exception (e.g., `InvalidAccessTokenException`) for any validation failure, allowing the caller to distinguish between missing-token (400) and invalid-token (401) scenarios.

---

## 7. CORS (Cross-Origin Resource Sharing)

#### REQ-CO-001: Development-Mode CORS
When the gateway runs in development mode, a permissive CORS policy must be applied:
- All origin patterns allowed
- Methods: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`, `HEAD`
- All headers allowed
- Credentials allowed
- Applied globally to all paths

#### REQ-CO-002: Conditional CORS Activation
The permissive CORS filter must ONLY be active when a development-mode flag is enabled. In production, CORS must be handled by the reverse proxy / load balancer in front of the gateway, or a restrictive policy must be configured.

#### REQ-CO-003: Development Mode Configuration
The development mode flag must be configurable via an application property / environment variable, defaulting to `false` in production.

---

## 8. HTTPS Redirect

#### REQ-HS-001: HTTP to HTTPS Redirect
The gateway must support a filter that redirects all incoming HTTP requests to their HTTPS equivalent.

#### REQ-HS-002: Redirect Behavior
- The redirect must use HTTP status `301 Moved Permanently`. A permanent redirect is correct for HTTPS enforcement, as it allows browsers and clients to cache the redirect and avoid the extra round-trip on subsequent requests.
- The redirect URL must be identical to the original, with only the scheme changed from `http` to `https`
- This filter must execute at the highest priority order (before all other filters)
- The filter must preserve the original request path, query parameters, and fragment in the redirect URL

#### REQ-HS-003: Conditional Activation
The HTTPS redirect must be configurable and disabled by default for local development. It should be enabled in production deployments.

---

## 9. User Management Client

#### REQ-CL-001: Reactive HTTP Client for User Management
The gateway must include an HTTP client capable of communicating with the User Management backend service for supplementary operations.

#### REQ-CL-002: Client Capabilities
The client must support the following operations:
1. **Get Tenant Details** — Fetch basic tenant (organization) information by tenant ID
2. **Get User Details by Username** — Look up a user by their username within a tenant
3. **Get User Details by Token** — Look up a user by their access token
4. **Get User Access Resource IDs** — Retrieve the list of resource IDs a user has access to, filtered by application ID, resource type, and permission
5. **Has User Access to Resource** — Check if a specific user has a specific permission on a specific resource

#### REQ-CL-003: Client Configuration
The client must resolve the User Management service URL from the same router configuration used for routing, ensuring consistency.

---

## 10. Data Models

#### REQ-DM-001: Tenant Basic Detail
The gateway must model a tenant (organization) with the following attributes:
- `tenantId` (unique identifier — matches the subdomain segment)
- `name` (display name)
- `region` (region code)
- `country` (country code)
- `timezone` (IANA timezone identifier)
- `authMode` (authentication mode: `internal` or `external`)
- `status` (`active` or `inactive`)
- `authUrl` (external IdP / SSO login URL, applicable when `authMode` is `external`)

#### REQ-DM-002: User
The gateway must model an authenticated user with the following attributes:
- `userId` (UUID — maps to JWT `sub` claim)
- `tenantId` (tenant identifier — maps to JWT `tenant_id` claim)
- `userName` (display name — maps to JWT `name` claim)
- `email` (email address)
- `phoneNumber` (mobile number — maps to JWT `phone_number` claim)
- `status` (`active` or `inactive`)
- `authorities` (set of permission/role strings for authorization decisions)

#### REQ-DM-003: Tenant Access Verification
The user model must expose a method to verify whether a user belongs to a given tenant. This is used in REQ-AU-008 to validate the `tenant_id` claim against the `X-Tenant-ID` derived from the subdomain.

---

## 11. Configuration & Environment

#### REQ-CF-001: Environment Variable Override
All configuration properties must support override via environment variables for containerized deployments.

#### REQ-CF-002: Key Configuration Properties
| Property | Env Variable | Description | Default |
|---|---|---|---|
| Server port | `SERVER_PORT` | HTTP listen port | `80` |
| Router config file path | `GATEWAY_ROUTER_CONFIG_FILE_PATH` | Path to routing JSON | (monolith local config) |
| Security config file path | `GATEWAY_SECURITY_CONFIG_FILE_PATH` | Path to security JSON | (security config) |
| JWT generator type | `JWT_GENERATOR` | Which JWT library to use (`jjwt` or `nimbus`) | `jjwt` |
| JWT secret key | `JWT_SECRET_KEY` | Token signing/verification key | **(required — no default)** |
| JWT expiration | `JWT_EXPIRATION` | Token validity in milliseconds | `900000` (15 min) |
| JWT issuer | `JWT_ISSUER` | Expected `iss` claim value | `yourorg.com` |
| JWT audience | `JWT_AUDIENCE` | Expected `aud` claim value | `yourorg.com` |
| Development mode | `DEVELOPMENT_MODE` | Enable permissive CORS and additional debug output | `false` |
| Root log level | `ROOT_LOG_LEVEL` | Root logger level | `INFO` |
| Application log level | `APP_LOG_LEVEL` | Application package log level | `INFO` |
| Log file path | `LOG_FILE_PATH` | Directory for log file output | `.` (stdout only) |

> **Security note:** `JWT_SECRET_KEY` must be provided via a secrets manager or injected secret volume in production. It must never be hardcoded in source code or committed to version control.

#### REQ-CF-003: JSON Property Naming Convention
All JSON serialization/deserialization must use `snake_case` naming strategy for consistency with configuration files and API contracts.

#### REQ-CF-004: Reactive Runtime
The gateway must run on a non-blocking, reactive runtime to handle high concurrency with minimal threads.

---

## 12. Deployment

#### REQ-DP-001: Container Support
The gateway must be containerizable via a Dockerfile and runnable as a standalone container image.

#### REQ-DP-002: Docker Image Naming
Docker images must follow the naming convention: `yourorg/api-gateway:{version}`.

#### REQ-DP-003: Default Port
The gateway must listen on port `80` by default, overridable via environment variable.

#### REQ-DP-004: SSL Termination
SSL/TLS termination is expected to be handled by a reverse proxy (e.g., Nginx) in front of the gateway. The gateway itself must support optional SSL configuration (keystore-based) for environments without a reverse proxy.

---

## 13. Centralized Monitoring, Logging & Observability

### 13.1 Structured Logging

#### REQ-OB-001: Structured Log Format
All gateway log output must use a structured format (e.g., JSON) suitable for ingestion by centralized log aggregation systems (ELK, Loki, Splunk, CloudWatch, etc.). Each log entry must include at minimum:
- `timestamp` — ISO 8601, UTC
- `level` — log severity (DEBUG, INFO, WARN, ERROR)
- `logger` — source component name
- `correlationId` — per-request trace ID (see REQ-OB-004)
- `message` — human-readable description
- Contextual fields (included where available per request):
  - `tenantId` — tenant identifier from `X-Tenant-ID`
  - `userId` — user identifier from `X-User-ID`
  - `serviceId` — service identifier from `X-Service-ID`
  - `requestPath` — the incoming request URI path
  - `httpMethod` — GET, POST, PUT, DELETE, etc.
  - `httpStatus` — response status code (on response-side log entries)
  - `durationMs` — request processing time in milliseconds (on response-side log entries)

#### REQ-OB-002: Configurable Log Levels
Log levels must be independently configurable per package/component via application properties or environment variables. At minimum, the following must be independently tunable:
- Root log level
- Gateway application log level
- Authentication filter log level
- Router log level

#### REQ-OB-003: Authentication Filter Logging
The Authentication Filter must produce logs at the following levels:
- **DEBUG**: request path being evaluated, public endpoint match result, private endpoint match result, successful authentication (including `tenantId` and `userId`)
- **WARN**: missing required headers (`X-Auth-Mode`, `Authorization`), invalid or expired tokens, token validation failures (reason included)
- **ERROR**: unexpected runtime errors, `tenant_id` claim mismatch between token and subdomain `X-Tenant-ID`, service ID not found in security configuration

The gateway must never log token values, passwords, or other secrets at any log level.

#### REQ-OB-004: Distributed Tracing / Correlation ID
The gateway must generate or propagate a unique **Correlation ID** for every incoming request to enable end-to-end distributed tracing across all services:
- The gateway must honour the W3C `traceparent` header (Trace Context standard) when present in the incoming request and propagate it downstream unchanged.
- If no `traceparent` header is present, the gateway must also check for `X-Correlation-ID` and `X-Request-ID` headers and propagate whichever is found.
- If no trace header is present, the gateway must generate a new UUID v4 and inject it as both `X-Correlation-ID` and `traceparent`.
- The Correlation ID must be:
  - Bound to all log entries produced while processing that request (via MDC or equivalent)
  - Injected into the downstream request so backend services can continue the trace
  - Included in all error responses returned to the client as the `X-Correlation-ID` header
- The gateway must never modify or truncate an existing trace context received from a client.

#### REQ-OB-005: Configurable Log Output Destination
Log file path, log format, and log levels must be configurable via environment variables. The gateway must support writing logs to:
- Standard output (stdout/stderr) for containerized deployments
- File-based output with configurable path for VM-based deployments

### 13.2 Metrics

#### REQ-OB-010: Request Metrics
The gateway must expose quantitative metrics for monitoring dashboards and alerting. At minimum:
- **Request count** — total requests received, broken down by: route/service, HTTP method, response status code
- **Request latency** — histogram/percentile distribution of request processing time, broken down by route/service
- **Active connections** — current number of active/in-flight requests
- **Error rate** — count of 4xx and 5xx responses, broken down by route/service and error type

#### REQ-OB-011: Authentication Metrics
The gateway must track authentication-specific metrics:
- Count of successful authentications
- Count of authentication failures (broken down by reason: expired token, invalid signature, missing token, org mismatch)
- Count of public endpoint bypasses
- Count of requests rejected due to unrecognized service code

#### REQ-OB-012: Rate Limiting Metrics
The gateway must track rate limiting metrics:
- Count of requests throttled (HTTP 429), broken down by limiting dimension (tenant, IP, endpoint)
- Current utilization percentage of rate limit quotas per tenant

#### REQ-OB-013: Resilience Metrics
The gateway must track circuit breaker and retry metrics:
- Circuit breaker state transitions (closed → open, open → half-open, half-open → closed)
- Count of requests short-circuited by open circuit breakers, per downstream service
- Count of retry attempts per downstream service
- Fallback invocation count per downstream service

#### REQ-OB-014: Metrics Endpoint
The gateway must expose a metrics endpoint (e.g., `/actuator/metrics` or `/actuator/prometheus`) in a format compatible with standard monitoring systems (Prometheus, Micrometer, CloudWatch). This endpoint must be excluded from authentication.

### 13.3 Health & Readiness

#### REQ-OB-020: Health Check Endpoint
The gateway must expose a health check endpoint (e.g., `/actuator/health`) that reports:
- Overall gateway health status (UP / DOWN)
- Downstream service connectivity status (for each configured backend service)
- Circuit breaker states

#### REQ-OB-021: Readiness and Liveness Probes
The gateway must expose separate liveness and readiness probe endpoints for container orchestration:
- **Liveness** (`/actuator/health/liveness`): indicates the gateway process is running and not deadlocked
- **Readiness** (`/actuator/health/readiness`): indicates the gateway has loaded its configuration and is ready to accept traffic

#### REQ-OB-022: Health Endpoint Security
Health and metrics endpoints must be accessible without authentication but should be restricted to internal/management traffic (e.g., via a separate management port or IP allowlist) in production.

### 13.4 Alerting Foundation

#### REQ-OB-030: Alertable Conditions
The metrics and logging infrastructure must support defining alerts for the following conditions:
- Error rate exceeds a configurable threshold (e.g., >5% of requests returning 5xx over 5 minutes)
- Downstream service circuit breaker opens
- Authentication failure rate spikes above baseline
- Rate limit utilization exceeds a configurable threshold per tenant
- Request latency p99 exceeds a configurable threshold
- Gateway health check reports DOWN

Note: The gateway does not need to implement alerting itself, but must provide sufficient metrics and log data for external alerting systems to act on.

---

## 14. Error Handling

#### REQ-EH-001: Consistent Error Status Codes
The gateway must return the following HTTP status codes for error conditions:
| Condition | Status Code | Notes |
|---|---|---|
| Service ID (`X-Service-ID`) not found in security config | `403 Forbidden` | |
| Missing or invalid `X-Auth-Mode` header | `400 Bad Request` | Include accepted values in the error message |
| Missing `Authorization: Bearer` header on private endpoint | `401 Unauthorized` | |
| Invalid, expired, or tampered JWT token | `401 Unauthorized` | Do not reveal whether the token exists or why it failed |
| `tenant_id` claim does not match subdomain `X-Tenant-ID` | `401 Unauthorized` | This is an authentication failure, not a server error |
| Endpoint not in public or private whitelist | `403 Forbidden` | |
| External auth not yet implemented (`X-Auth-Mode: external`) | `501 Not Implemented` | |
| Downstream circuit breaker open | `503 Service Unavailable` | Include `Retry-After` header |
| Downstream timeout | `504 Gateway Timeout` | |
| Unexpected internal errors | `500 Internal Server Error` | |

#### REQ-EH-002: Structured Error Response Body
All error responses must return a JSON body with a consistent structure:
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Human-readable description of the failure",
  "correlationId": "<X-Correlation-ID value>",
  "timestamp": "<ISO 8601 UTC timestamp>"
}
```
The `message` field must be informative enough to aid debugging but must not reveal internal details (class names, stack traces, configuration values, or token contents).

#### REQ-EH-003: No Stack Traces in Responses
Error responses must never expose Java stack traces, internal class names, configuration file paths, or any implementation-specific details to clients, regardless of environment.

#### REQ-EH-004: Security-Neutral Error Messages for Auth Failures
For authentication failures (401), the gateway must return a message that does not distinguish between "token not found" and "token invalid". Revealing whether a token exists or is merely expired aids token enumeration attacks. Use a generic message such as `"Authentication required"` or `"Invalid or missing credentials"`.

---

## 15. Rate Limiting & Throttling

### 15.1 General Rate Limiting Requirements

#### REQ-RL-001: Request Rate Limiting
The gateway must enforce configurable rate limits to protect backend services from excessive traffic, abuse, and denial-of-service conditions. Rate limiting must be applied before the request is forwarded to any downstream service.

#### REQ-RL-002: Multi-Dimensional Rate Limiting
The gateway must support rate limiting across multiple dimensions, independently or in combination:
| Dimension | Description |
|---|---|
| **Tenant** | Limit total requests per tenant per time window |
| **Client IP** | Limit requests from a single IP address per time window |
| **User** | Limit requests per authenticated user per time window |
| **Service / Route** | Limit requests to a specific backend service per time window |
| **Endpoint** | Limit requests to a specific API endpoint per time window |

#### REQ-RL-003: Configurable Rate Limit Policies
Rate limit policies must be externally configurable (via configuration file or application properties) without code changes. Each policy must define:
- **Dimension**: which attribute to limit by (tenant, IP, user, service, endpoint, or combination)
- **Limit**: maximum number of requests allowed in the time window
- **Time window**: duration of the rate limit window (e.g., 60 seconds, 1 hour)
- **Scope**: which routes or services the policy applies to (global, per-service, or per-endpoint)

#### REQ-RL-004: Default Rate Limits
The gateway must apply sensible default rate limits even if no explicit policy is configured. Defaults must be overridable. Suggested defaults:
| Dimension | Default Limit | Window |
|---|---|---|
| Per tenant | 1000 requests | 1 minute |
| Per IP (unauthenticated) | 100 requests | 1 minute |
| Per user | 500 requests | 1 minute |

### 15.2 Rate Limiting Behavior

#### REQ-RL-010: HTTP 429 Response
When a rate limit is exceeded, the gateway must reject the request with HTTP status `429 Too Many Requests` and include:
- A descriptive error message in the response body indicating which limit was exceeded
- Standard rate limit headers in the response:
  - `X-RateLimit-Limit` — the maximum number of requests allowed in the current window
  - `X-RateLimit-Remaining` — the number of requests remaining in the current window
  - `X-RateLimit-Reset` — the time (epoch seconds) when the current window resets
  - `Retry-After` — the number of seconds the client should wait before retrying

#### REQ-RL-011: Rate Limit Headers on All Responses
Rate limit headers (`X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`) should be included on all successful responses (not just 429), so clients can proactively manage their request rate.

#### REQ-RL-012: Rate Limiting Algorithm
The gateway must use a proven rate limiting algorithm that provides smooth request distribution. Acceptable algorithms include:
- **Token Bucket** — allows short bursts while enforcing average rate
- **Sliding Window Log** — precise per-window accounting
- **Sliding Window Counter** — memory-efficient approximation

The chosen algorithm must handle edge cases at window boundaries gracefully (no sudden allow/deny cliffs).

#### REQ-RL-013: Rate Limit Exemptions
The gateway must support exempting specific requests from rate limiting:
- Health check and metrics endpoints must be exempt
- Internal service-to-service calls (identified by a trusted header or source IP range) should be exempt
- Specific tenants or IPs may be allowlisted via configuration

### 15.3 Rate Limit State Management

#### REQ-RL-020: In-Memory Rate Limit State (Single Instance)
For single-instance deployments, the gateway must maintain rate limit counters in-memory with minimal overhead.

#### REQ-RL-021: Distributed Rate Limit State (Multi-Instance)
For horizontally scaled deployments with multiple gateway instances, the gateway must support a shared external store (e.g., Redis) for rate limit counters to ensure consistent enforcement across all instances. The external store integration must be optional and configurable — the gateway must function with local-only rate limiting when no external store is configured.

#### REQ-RL-022: Graceful Degradation on State Store Failure
If the external rate limit state store becomes unavailable, the gateway must degrade gracefully:
- Fall back to local in-memory rate limiting per instance
- Log a warning indicating degraded rate limiting mode
- Resume distributed rate limiting automatically when the state store recovers

---

## 16. Resilience & Fault Tolerance

### 16.1 Circuit Breaker

#### REQ-RS-001: Circuit Breaker Per Downstream Service
The gateway must implement a circuit breaker for each configured downstream backend service. The circuit breaker must prevent cascading failures by temporarily stopping requests to an unhealthy service.

#### REQ-RS-002: Circuit Breaker States
The circuit breaker must implement the standard three-state model:
- **Closed** (normal): requests flow through. Failures are counted.
- **Open** (tripped): requests are immediately rejected without forwarding to the downstream service. A fallback response is returned.
- **Half-Open** (probing): a limited number of trial requests are forwarded. If they succeed, the circuit closes. If they fail, the circuit re-opens.

#### REQ-RS-003: Configurable Circuit Breaker Thresholds
Circuit breaker behavior must be configurable per downstream service with the following parameters:
| Parameter | Description | Suggested Default |
|---|---|---|
| Failure rate threshold | Percentage of failed requests that triggers opening | 50% |
| Slow call rate threshold | Percentage of slow calls that triggers opening | 80% |
| Slow call duration threshold | Duration after which a call is considered slow | 5 seconds |
| Minimum number of calls | Minimum calls in evaluation window before the breaker can trip | 10 |
| Sliding window size | Number of calls or time window for evaluating failure rate | 20 calls |
| Wait duration in open state | How long the circuit stays open before transitioning to half-open | 30 seconds |
| Permitted calls in half-open | Number of trial calls allowed in half-open state | 5 |

#### REQ-RS-004: Circuit Breaker Fallback Responses
When the circuit breaker is open, the gateway must return:
- HTTP status `503 Service Unavailable`
- A descriptive error message indicating the downstream service is temporarily unavailable
- A `Retry-After` header suggesting when the client may retry

### 16.2 Retry Mechanism

#### REQ-RS-010: Automatic Retries
The gateway must support automatic retries for failed requests to downstream services. Retries must be configurable per service.

#### REQ-RS-011: Retry Configuration
Retry behavior must be configurable with the following parameters:
| Parameter | Description | Suggested Default |
|---|---|---|
| Max retry attempts | Maximum number of retry attempts (excluding initial request) | 2 |
| Retry backoff interval | Base delay between retries | 500 ms |
| Retry backoff multiplier | Multiplier applied to delay on each successive retry | 2.0 |
| Max backoff interval | Maximum delay cap | 5 seconds |
| Retryable status codes | HTTP status codes that trigger a retry | 502, 503, 504 |
| Retryable exceptions | Connection-level exceptions that trigger a retry | connection refused, timeout |

#### REQ-RS-012: Idempotency-Aware Retries
Retries must respect HTTP method semantics:
- **Safe methods** (`GET`, `HEAD`, `OPTIONS`): always retryable
- **Idempotent methods** (`PUT`, `DELETE`): retryable by default
- **Non-idempotent methods** (`POST`, `PATCH`): NOT retried by default. Retrying POST/PATCH must be explicitly opted-in via configuration or a request header (e.g., `Idempotency-Key`)

#### REQ-RS-013: Retry with Jitter
Retry backoff must include a randomized jitter component to prevent thundering herd scenarios when multiple requests retry against the same recovering service.

### 16.3 Timeouts

#### REQ-RS-020: Connection Timeout
The gateway must enforce a configurable **connection timeout** for establishing a TCP connection to downstream services. If the connection is not established within this period, the request must fail immediately.
| Parameter | Suggested Default |
|---|---|
| Connection timeout | 5 seconds |

#### REQ-RS-021: Response Timeout
The gateway must enforce a configurable **response timeout** (read timeout) for receiving the first byte of the response from a downstream service. If the downstream does not begin responding within this period, the request must fail.
| Parameter | Suggested Default |
|---|---|
| Response timeout | 30 seconds |

#### REQ-RS-022: Per-Route Timeout Overrides
Timeout values must support per-route or per-service overrides to accommodate services with inherently different response time profiles (e.g., report generation may need a longer timeout than a health check).

#### REQ-RS-023: Timeout Error Response
When a request times out, the gateway must return:
- HTTP status `504 Gateway Timeout`
- A descriptive error message indicating the downstream service did not respond in time

### 16.4 Bulkhead / Concurrency Limiting

#### REQ-RS-030: Per-Service Concurrency Limits
The gateway must support configurable concurrency limits (bulkhead pattern) per downstream service. This limits the number of concurrent in-flight requests to any single service, preventing one slow or overloaded service from consuming all gateway resources and starving other services.

#### REQ-RS-031: Bulkhead Configuration
| Parameter | Description | Suggested Default |
|---|---|---|
| Max concurrent calls | Maximum parallel requests to a downstream service | 50 |
| Max wait duration | Maximum time a request will wait for a concurrency slot | 500 ms |

#### REQ-RS-032: Bulkhead Rejection Response
When the concurrency limit is reached and the wait duration expires, the gateway must return:
- HTTP status `503 Service Unavailable`
- A descriptive error message indicating the downstream service is at capacity

### 16.5 Graceful Degradation

#### REQ-RS-040: Partial Availability
If one downstream service is unavailable (circuit open), the gateway must continue routing requests to all other healthy services normally. A single service failure must not impact the availability of other services.

#### REQ-RS-041: Startup Resilience
The gateway must start successfully even if one or more downstream services are unavailable at startup. Downstream health is evaluated at request time, not at boot time.

#### REQ-RS-042: Configuration Failure Behavior
If the routing or security configuration file is missing or malformed at startup, the gateway must fail fast with a clear error message and refuse to start, rather than starting in a degraded state with missing security controls.

---

## 17. Non-Functional Requirements

#### REQ-NF-001: Performance
The gateway must add minimal overhead to proxied requests. Specifically:
- URL pattern matching must use precompiled regex patterns loaded at startup; no per-request compilation
- JWT validation must use cached signing keys; no per-request key fetching
- Security and routing configuration must be held in memory after startup; no per-request file I/O
- The gateway must target a p99 added latency of under 10 ms for the gateway's own processing (excluding downstream response time)

#### REQ-NF-002: Scalability
The gateway must be fully stateless — it must hold no per-request or per-session state in memory between requests. All state (rate limit counters) must reside in an external store. This enables horizontal scaling behind a load balancer without sticky sessions.

#### REQ-NF-003: Security — Fail Closed
The gateway must operate on a deny-by-default model at every level:
- A request to an unknown service ID must be denied
- A request to an endpoint not in the whitelist must be denied
- A missing or malformed security configuration at startup must prevent the gateway from starting
- A failed JWT validation must deny the request; any error in the validation pipeline must not result in a pass-through

#### REQ-NF-004: Security — Header Stripping
The gateway must strip all trusted identity headers (`X-Tenant-ID`, `X-User-ID`, `X-User-Name`, `X-User-Phone`, `X-Internal-Request`) from any incoming client request before processing. These headers must only be set by the gateway after successful authentication, preventing clients from injecting false identity claims.

#### REQ-NF-005: Testability
All components (authentication filter, token manager, URL pattern matching, routing logic, circuit breaker, rate limiter) must be independently unit-testable via clear interfaces and constructor-based dependency injection. The following test categories must be supported:
- **Unit tests**: each component in isolation with mocked dependencies
- **Integration tests**: full filter chain with an embedded gateway instance
- **Contract tests**: security configuration and routing configuration loading and validation

#### REQ-NF-006: Configuration Hot-Reload (Future)
Currently, routing and security configurations are loaded once at startup. A future enhancement must support reloading both configurations without restarting the gateway process, to allow endpoint whitelist updates without downtime.

---

## Appendix A: Service Endpoint Summary

### A.1 user-mgmt
**Public endpoints:**
- `GET /crud/health`, `GET /crud/entities`
- `GET /auth/internal-auth/initiate`, `POST /auth/internal-auth/identify`
- `GET /auth/internal-auth/public-key`, `POST /auth/internal-auth/login`
- `POST /auth/internal-auth/refresh-token`, `GET /auth/internal-auth/logged-in-users`
- `GET /applications`, `GET /permission-categories`, `GET /permissions`
- `GET /application/{appCode}/resource-types`, `GET /version`

**Private endpoints (selected):**
- CRUD: `POST /crud`, `POST /crud/relation`, `POST /crud/file-upload-request`, `POST /crud/file-download-request`
- Auth: `POST /auth/internal-auth/logout`, `POST /auth/internal-auth/logout-all`
- Users: `GET /user/list`, `POST /user`, `GET|PUT|DELETE /user/{resourceId}`, `GET /user/me`
- Groups: `GET /org-group/list`, `POST /org-group`, `GET|PUT|DELETE /org-group/{resourceId}`
- Policies: `GET /org-policy/list`, `POST /org-policy`, `GET|DELETE /org-policy/{resourceId}`
- Access control: `GET /user-access-control/app-permissions`

### A.2 org-setting
**Public endpoints:**
- `GET /crud/health`, `GET /crud/entities`
- `POST /email-registration-initiate`, `POST /email-lead-verification`
- `GET /organization/details`, `GET /organization/exists/{tenantId}`, `POST /organization/register`
- `POST /crud/public`
- `GET /public/master/countries`, `GET /public/master/timezones/country/{countryCode}`

**Private endpoints (selected):**
- CRUD operations, Organization location management, Workflow structure
- File upload/download, Organization logo, Master data (countries, regions, timezones, states)

### A.3 document-store
**Public endpoints:** `GET /crud/health`, `GET /crud/entities`

**Private endpoints:**
- CRUD operations
- `GET /document-store/download/{downloadToken}`, `POST /document-store/upload/{uploadToken}`

### A.4 emp-mgmt
**Public endpoints:** `GET /crud/health`, `GET /crud/entities`

**Private endpoints (selected):**
- CRUD operations, File upload/download
- Onboarding: start, submit, cancel
- Approval: process, pending, completed, request details, activity log
- Employee: add, list, profile, next-number, create-user, from-onboarding, me
- Compensation: master components
- ESS: info update submit, my requests, cancel
- Resignation: submit, list, cancel, withdraw, admin operations
- Leave: apply, calculate days, my applications/balances/ledger, team applications, admin operations
- Attendance: check-in, check-out, today, my records, team records, list

### A.5 payroll-mgmt
**Public endpoints:** `GET /crud/health`, `GET /crud/entities`

**Private endpoints (selected):**
- CRUD operations
- Rule engine: expressions, evaluate, cache refresh
- VPF: my opt-in, opt-in, cancel
- Tax: my declaration, regime change, my form16
- Reports: payroll summary, employee status, payroll history
- Payslip: my payslips, payslip details, line items
- Payroll run: initiate, approve, reject
- OAAR: my allocations, my claims, claim, approve, reject
- Investment: my declarations, my proofs, proof verification
- Compensation: assign, revise, my compensation
- Salary structure: preview, onboarding preview, employee salary
