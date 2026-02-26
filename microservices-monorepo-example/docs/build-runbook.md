# Build Runbook

This runbook explains how to build, test, and release this monorepo.

## 1) Prerequisites

- JDK 17, 18, 19, 20, or 21 available on `JAVA_HOME`
- Maven 3.9+
- Network access to dependency repositories

Project settings:

- Code is compiled with `maven.compiler.release=17`
- Maven Enforcer requires Java range `[17,22)`

## 2) Standard local build

From repository root:

1. Clean + full verification:
   - `mvn clean verify`
2. Faster inner-loop build (skip tests):
   - `mvn -DskipTests clean verify`
3. Run enforcer rules explicitly:
   - `mvn enforcer:enforce`

## 3) Building a single service or library

Use `-pl <module> -am` to build selected module + required upstream modules.

Examples:

- Build one service and dependencies:
  - `mvn -pl services/order-service -am clean verify`
- Build one library and dependencies:
  - `mvn -pl libraries/lib-core -am clean verify`

## 4) Running tests

- Unit tests in all modules:
  - `mvn test`
- Full verification phase (includes tests):
  - `mvn verify`
- Skip tests temporarily (local only):
  - `mvn -DskipTests verify`

## 5) Versioning and release builds

The root POM uses CI-friendly versioning:

- `revision` (e.g., `1.2.0`)
- `changelist` (usually `-SNAPSHOT` for dev)

Common commands:

- Snapshot/local dev build:
  - `mvn "-Drevision=0.1.0" "-Dchangelist=-SNAPSHOT" clean verify`
- Release-style verification:
  - `mvn "-Drevision=1.2.0" "-Dchangelist=" clean verify`
- Release-style deployment:
  - `mvn "-Drevision=1.2.0" "-Dchangelist=" clean deploy`

## 6) CI workflow

Sample GitHub Actions release workflow:

- `.github/workflows/release.yml`

It supports:

- `workflow_dispatch` with `revision` input
- Git tag trigger `vX.Y.Z`

Required GitHub repository secrets:

- `MAVEN_USERNAME`
- `MAVEN_PASSWORD`

## 7) Dependency management rules

- All modules inherit from root parent `pom.xml`
- Root parent `pom.xml` imports `org-bom`; all modules inherit it
- Internal module versions are centralized in BOM
- Avoid hardcoded dependency versions in child modules where BOM manages them

## 8) Preventing cyclic dependencies

Library direction is one-way:

- `lib-domain` -> no internal library dependencies
- `lib-core` -> depends on `lib-domain`
- `lib-infra` -> depends on `lib-core` and `lib-domain`

Enforcer `bannedDependencies` rules in library POMs block upward dependency edges.

## 9) Troubleshooting

### Java version mismatch

Symptom:

- Enforcer failure for `RequireJavaVersion`

Fix:

- Check Java version: `java -version`
- Ensure `JAVA_HOME` points to JDK 17..21

### Dependency convergence errors

Symptom:

- `dependencyConvergence` or `requireUpperBoundDeps` failure

Fix:

- Inspect dependency tree:
  - `mvn -Dverbose dependency:tree`
- Pin versions in `bom/pom.xml`

### Module resolution issues

Symptom:

- Maven cannot find parent/BOM/modules

Fix:

- Run commands from repository root
- Verify module paths under root `pom.xml`
