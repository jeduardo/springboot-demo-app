# Dependency Modernization Design

## Goal

Bring the application from Spring Boot 2.7 and its backlog of unmerged
Dependabot updates to Spring Boot 4.1.0 on Java 21 while preserving its REST,
database, request-tracing, logging, and monitoring behavior.

## Current State

The checkout contains an unfinished migration to Spring Boot 3.5.4. It already
moves persistence and servlet APIs from `javax.*` to `jakarta.*`, converts the
tests to JUnit Jupiter, and starts simplifying dependency management. The
checkout is three commits behind `origin/main`; those commits update PostgreSQL,
H2, and Commons Lang. Five open Dependabot pull requests cover Commons Lang,
MariaDB Connector/J, Spring Boot 4, Flyway, and PostgreSQL.

The existing `mvn test` baseline compiles but reports five test errors because
Flyway creates an identity-style `serial` column while the JPA entity requires
an `entries_id_seq` sequence.

## Migration Strategy

Use a staged migration rather than replaying the Dependabot pull requests
individually:

1. Reconcile the existing worktree with the three upstream dependency commits.
2. Upgrade to Spring Boot 3.5.16 and make the full test suite pass.
3. Upgrade to Spring Boot 4.1.0 and replace deprecated or broad starters with
   Boot 4's focused starters.
4. Resolve all remaining stable dependency updates and verify the resolved
   graph.

The intermediate Spring Boot 3.5 checkpoint follows Spring's recommended path
for applications upgrading from an earlier release to Spring Boot 4. The final
state, not the intermediate checkpoint, is the deliverable.

## Build and Dependencies

- Set Java 21 as the Maven compiler release and GitHub Actions runtime.
- Use Spring Boot 4.1.0 as the parent.
- Use Boot 4's focused Web MVC, Web MVC test, Flyway, persistence, actuator,
  Prometheus, and Log4j2 starters where applicable.
- Allow Spring Boot's dependency-management BOM to control application
  dependency versions unless a version must intentionally differ.
- Keep explicit versions for dependencies nested inside the Flyway Maven plugin,
  because Maven plugin dependencies do not inherit application dependency
  management in the same way.
- Remove obsolete JUnit 4, Vintage, `javax.*`, logging bridge, and duplicate
  library declarations.
- Resolve current stable Flyway and database-driver releases from Maven Central
  during implementation, checking their compatibility with Spring Boot 4.1.
- Keep application artifact coordinates and version unchanged.

## Application Compatibility

Retain the existing Jakarta Persistence and Jakarta Servlet migrations. Limit
application changes to compatibility fixes required by Spring Boot 4, Spring
Framework 7, Hibernate, Flyway, and Jackson 3. Do not refactor controller or
repository structure unless a framework API makes a focused change necessary.

The external REST contract remains unchanged:

- CRUD endpoints retain their paths, methods, payloads, and status codes.
- Missing entries retain the existing `ResourceNotFoundException` behavior.
- Actuator health and Prometheus endpoints remain available.

## Database Migration

Flyway remains the only schema-initialization mechanism. Align the `Entry`
identifier mapping with the migration's identity-style `serial` column rather
than adding a sequence that is not portable across PostgreSQL, MySQL/MariaDB,
and H2. Do not mask migration defects with Hibernate schema generation in tests.
Validate application startup and CRUD operations against the migrated H2
schema.

The Flyway Maven plugin keeps support for PostgreSQL, MySQL, and MariaDB through
its explicit database modules and JDBC-driver dependencies. Correct the existing
password-property typo as part of the migration.

## Request IDs and Logging

`RequestIdFilter` must:

- reuse a nonblank incoming `X-Request-Id` value;
- generate a UUID when the header is absent or blank;
- place the value in the logging MDC for the duration of the request;
- return the value in the `X-Request-Id` response header; and
- clear the MDC after the filter chain completes, including exceptional paths.

Log4j2 continues to emit JSON to standard output with the request ID included.
The logging configuration and dependencies must agree on the selected appender
name and include the JSON template layout implementation when required.

## Continuous Integration

Run the test workflow for pull requests targeting `main` and for pushes to
`main`. Do not run the workflow for ordinary feature-branch pushes, because an
open pull request already emits a `pull_request` `synchronize` event for the
same commit. This preserves validation for pull requests, direct pushes, and
merged commits without executing the same feature-branch revision twice.

## Dependabot Maintenance

Update `.github/dependabot.yml` to group compatible Maven production dependency
updates. This reduces overlapping single-dependency pull requests while still
allowing Dependabot to surface incompatible major upgrades separately.

Existing Dependabot pull requests are treated as upgrade inputs, not branches
to merge mechanically. Closing or otherwise mutating those pull requests is
outside the local implementation scope.

## Verification

The migration is accepted when all of the following are true on Java 21:

1. The Spring Boot 3.5.16 checkpoint passes the full test suite before moving to
   Spring Boot 4.
2. Application-context startup and Flyway migration tests pass on Spring Boot
   4.1.0.
3. CRUD integration tests preserve the current API behavior.
4. Tests cover supplied and generated request IDs, response propagation, and
   MDC cleanup.
5. `mvn clean verify` completes successfully.
6. The resolved dependency tree contains no unintended Spring Boot 3,
   `javax.*`, JUnit 4/Vintage, or duplicate SLF4J/Log4j bridge artifacts.
7. Maven's dependency and plugin update reports show that all intentionally
   pinned dependencies are current stable releases, with any deliberate
   exceptions documented.

## References

- [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- [Spring Boot 4.1 System Requirements](https://docs.spring.io/spring-boot/system-requirements.html)
