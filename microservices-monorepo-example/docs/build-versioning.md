# Build and Versioning Guidelines

For command-level build and release steps, see `docs/build-runbook.md`.

## 1. Parent and BOM strategy

- All modules inherit from root `pom.xml`.
- Root `pom.xml` imports `org-bom` in `dependencyManagement`, and children inherit it.
- The org BOM imports Spring Boot BOM and defines internal library versions.

## 2. Where to put `spring-boot-starter-parent`

Do not use it in this repository.

Use:

- root `pom.xml` for shared Maven build policy
- `org-bom` for Spring Boot + internal version alignment

This avoids splitting inheritance between company parent and Boot parent.

## 3. Managing versions

Use CI-friendly version properties in root POM:

- `${revision}`
- `${changelist}`

Rules:

1. Keep `changelist=-SNAPSHOT` on development branches.
2. For release pipelines, pass `-Drevision=<x.y.z> -Dchangelist=`.
3. Avoid hardcoding internal module versions in child POMs.
4. Add external dependency version pins only in `bom/pom.xml`.

## 4. Preventing cyclic dependencies

Artifact-level cycle prevention:

- Keep strict one-way dependency layers.
- Use `maven-enforcer-plugin` + `bannedDependencies` in lower layers.

Code-level cycle prevention (recommended next step):

- Add ArchUnit tests in each module to enforce package boundaries.
- Fail CI if architectural tests fail.

## 5. CI checks

Recommended pipeline commands:

1. `mvn -B -DskipTests clean verify`
2. `mvn -B enforcer:enforce`
3. `mvn -B test` (with architecture tests)
