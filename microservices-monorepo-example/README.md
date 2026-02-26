# Microservices Maven Monorepo

This workspace is structured for multiple microservices and shared libraries using a centralized Maven parent and BOM.

Detailed docs:

- `docs/build-runbook.md` (how to build, test, release, and troubleshoot)
- `docs/build-versioning.md` (version and dependency governance)

## Module layout

- `pom.xml` (root): parent of all modules + aggregator for reactor builds
- `bom`: organization BOM (dependency versions, Spring Boot BOM import)
- `libraries/lib-domain`: foundational library layer
- `libraries/lib-core`: middle library layer, depends on `lib-domain`
- `libraries/lib-infra`: top library layer, depends on `lib-core` and `lib-domain`
- `services/*`: Spring Boot microservices that consume libraries

## Spring Boot parent location

This setup intentionally does **not** use `spring-boot-starter-parent`.

Instead, Spring dependency versions are managed by importing:

- `org.springframework.boot:spring-boot-dependencies`

inside `bom/pom.xml`.

Benefits:

- one root parent for all modules (services and libraries)
- consistent plugin management and company-wide policies
- Spring Boot version controlled centrally without forcing Boot parent inheritance
- BOM import is declared once in root parent and inherited by all child modules

## Versioning model

CI-friendly versions are enabled at root `pom.xml`:

- `revision` (for release numbers)
- `changelist` (usually `-SNAPSHOT` for development)

Example release build:

`mvn -Drevision=1.2.0 -Dchangelist= clean deploy`

## Dependency direction and cycle prevention

Library direction is one-way:

- `lib-domain` -> no internal library dependency
- `lib-core` -> may depend on `lib-domain`
- `lib-infra` -> may depend on `lib-core`, `lib-domain`

Maven Enforcer rules in each library block upward dependencies.
This prevents architectural cycles from being introduced.

## Build

Prerequisites:

- JDK 17 to 21 (project compiles with Java release 17)
- Maven 3.9+

Quick commands from repository root:

- Validate and package all modules: `mvn clean verify`
- Faster local compile-only check: `mvn -DskipTests clean verify`
- Run strict dependency checks: `mvn enforcer:enforce`

Build only one module and required dependencies:

- `mvn -pl services/order-service -am clean verify`

Release-style local build:

- `mvn "-Drevision=1.2.0" "-Dchangelist=" clean verify`

From repository root:

- `mvn clean verify`

Optional strict validation:

- `mvn enforcer:enforce`

## Sample release workflow

A sample GitHub Actions workflow is available at:

- `.github/workflows/release.yml`

How it versions releases:

- For manual runs (`workflow_dispatch`), provide `revision` input (for example `1.2.0`).
- For tag runs, tag as `vX.Y.Z` and workflow uses `X.Y.Z` as `revision`.
- `changelist` is set to empty during release (`-Dchangelist=`).

Required repository secrets for `mvn deploy`:

- `MAVEN_USERNAME`
- `MAVEN_PASSWORD`
