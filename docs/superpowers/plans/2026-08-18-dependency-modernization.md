# Dependency Modernization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the application to Spring Boot 4.1.0 and Java 21, preserve its public behavior, repair the incomplete migration, and reduce future Dependabot noise.

**Architecture:** Complete and verify the existing Spring Boot 3.5 migration first, then move to Boot 4.1's focused modules. Keep Flyway as the only schema initializer, use Boot's dependency BOM for compatible versions, and protect request-ID propagation with focused unit tests before changing implementation.

**Tech Stack:** Java 21, Maven, Spring Boot 4.1, Spring Web MVC, Spring Data JPA, Flyway, H2/PostgreSQL/MySQL/MariaDB, Log4j2, JUnit Jupiter, MockMvc, GitHub Actions, Dependabot

---

## File Map

- `pom.xml`: Java baseline, Spring Boot parent, starters, database drivers, logging, Flyway plugin, and obsolete repository cleanup.
- `src/main/java/org/jeduardo/entries/filter/RequestIdFilter.java`: request-ID generation, MDC lifetime, and response propagation.
- `src/main/java/org/jeduardo/entries/model/Entry.java`: portable identity-based primary-key mapping.
- `src/main/resources/application.properties`: current Flyway and Prometheus properties.
- `src/main/resources/log4j2.xml`: JSON console appender and request-ID field.
- `src/test/java/org/jeduardo/entries/filter/RequestIdFilterTest.java`: focused request-ID and MDC regression coverage.
- `src/test/java/org/jeduardo/entries/controller/EntryControllerTest.java`: Boot 4 test-module imports and existing CRUD regression coverage.
- `src/test/resources/application.properties`: H2/Flyway test configuration without removed Hibernate settings.
- `.github/workflows/test.yml`: Java 21 build on current GitHub Actions majors.
- `.github/dependabot.yml`: grouped compatible Maven updates and GitHub Actions updates.
- `README.md`: keep the existing user-owned wording edit; no additional documentation scope is required.

### Task 1: Lock Down Request-ID Behavior

**Files:**
- Create: `src/test/java/org/jeduardo/entries/filter/RequestIdFilterTest.java`
- Modify: `src/main/java/org/jeduardo/entries/filter/RequestIdFilter.java`

- [ ] **Step 1: Write focused regression tests**

Create `RequestIdFilterTest.java` with the complete contents below:

```java
package org.jeduardo.entries.filter;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestIdFilterTest {
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String REQUEST_ID_MDC_KEY = "requestId";

    private final RequestIdFilter filter = new RequestIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void reusesIncomingRequestIdAndReturnsItInTheResponse() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(REQUEST_ID_HEADER, "request-123");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                assertThat(MDC.get(REQUEST_ID_MDC_KEY)).isEqualTo("request-123"));

        assertThat(response.getHeader(REQUEST_ID_HEADER)).isEqualTo("request-123");
        assertThat(MDC.get(REQUEST_ID_MDC_KEY)).isNull();
    }

    @Test
    void generatesRequestIdWhenIncomingHeaderIsBlank() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(REQUEST_ID_HEADER, "  ");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            String requestId = MDC.get(REQUEST_ID_MDC_KEY);
            assertThatCodeIsUuid(requestId);
        });

        assertThatCodeIsUuid(response.getHeader(REQUEST_ID_HEADER));
        assertThat(MDC.get(REQUEST_ID_MDC_KEY)).isNull();
    }

    @Test
    void clearsMdcWhenTheFilterChainFails() {
        var request = new MockHttpServletRequest();
        request.addHeader(REQUEST_ID_HEADER, "request-456");
        var response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertThat(MDC.get(REQUEST_ID_MDC_KEY)).isEqualTo("request-456");
            throw new ServletException("downstream failure");
        }))
                .isInstanceOf(ServletException.class)
                .hasMessage("downstream failure");

        assertThat(response.getHeader(REQUEST_ID_HEADER)).isEqualTo("request-456");
        assertThat(MDC.get(REQUEST_ID_MDC_KEY)).isNull();
    }

    private static void assertThatCodeIsUuid(String value) {
        assertThat(value).isNotBlank();
        assertThat(UUID.fromString(value)).isNotNull();
    }
}
```

- [ ] **Step 2: Run the new tests and verify the response-header assertions fail**

Run:

```bash
mvn -Dtest=RequestIdFilterTest test
```

Expected: `reusesIncomingRequestIdAndReturnsItInTheResponse` and `clearsMdcWhenTheFilterChainFails` fail because the current filter never adds `X-Request-Id` to the response.

- [ ] **Step 3: Implement response propagation without weakening MDC cleanup**

Replace `RequestIdFilter.java` with:

```java
package org.jeduardo.entries.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Retrieve a request ID from the incoming request and make it available
 * for both the logging system and the request response.
 */
@Component
public class RequestIdFilter implements Filter {
    private static final String X_REQUEST_ID = "X-Request-Id";
    private static final String REQUEST_ID_MDC_KEY = "requestId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var httpRequest = (HttpServletRequest) request;
        var httpResponse = (HttpServletResponse) response;
        String requestId = Optional.ofNullable(httpRequest.getHeader(X_REQUEST_ID))
                .filter(value -> !value.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());

        httpResponse.setHeader(X_REQUEST_ID, requestId);
        try (MDC.MDCCloseable ignored = MDC.putCloseable(REQUEST_ID_MDC_KEY, requestId)) {
            chain.doFilter(request, response);
        }
    }
}
```

- [ ] **Step 4: Run the focused tests and verify they pass**

Run:

```bash
mvn -Dtest=RequestIdFilterTest test
```

Expected: 3 tests run with 0 failures and 0 errors.

- [ ] **Step 5: Commit the request-ID regression fix**

```bash
git add src/main/java/org/jeduardo/entries/filter/RequestIdFilter.java src/test/java/org/jeduardo/entries/filter/RequestIdFilterTest.java
git commit -m "fix: preserve request IDs across responses"
```

### Task 2: Complete the Spring Boot 3.5 Checkpoint

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/java/org/jeduardo/entries/model/Entry.java`
- Modify: `src/main/resources/application.properties`
- Modify: `src/main/resources/log4j2.xml`
- Modify: `src/test/resources/application.properties`
- Modify: `src/test/java/org/jeduardo/entries/ApplicationTest.java`
- Modify: `src/test/java/org/jeduardo/entries/controller/EntryControllerTest.java`

- [ ] **Step 1: Reproduce the existing schema-validation failure**

Run:

```bash
mvn -Dtest=ApplicationTest test
```

Expected: failure containing `Schema-validation: missing sequence [entries_id_seq]`.

- [ ] **Step 2: Move the intermediate build to the latest Boot 3.5 release and its managed versions**

In `pom.xml`:

- change the parent version to `3.5.16`;
- change `<java.version>` to `21`;
- remove `postgres.driver.version`, `mysql.driver.version`, and `mariadb.driver.version`;
- stop overriding `flyway.version`;
- remove explicit versions from Commons Lang and all application Flyway dependencies;
- add `org.apache.logging.log4j:log4j-layout-template-json`;
- remove the direct Jackson Databind and JsonPath dependencies because the Boot starters supply the required JSON stack;
- use the inherited `${flyway.version}`, `${postgresql.version}`, `${mysql.version}`, and `${mariadb.version}` properties for dependencies nested in `flyway-maven-plugin`;
- change the plugin migration location to `filesystem:src/main/resources/db/migration/common`;
- remove the obsolete Spring release repositories and Bintray distribution-management blocks.

The intermediate application dependency list must be:

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <exclusions>
            <exclusion>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-logging</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-log4j2</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.logging.log4j</groupId>
        <artifactId>log4j-layout-template-json</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
        <exclusions>
            <exclusion>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-logging</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
        <exclusions>
            <exclusion>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-logging</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-mysql</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.mariadb.jdbc</groupId>
        <artifactId>mariadb-java-client</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-lang3</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
        <exclusions>
            <exclusion>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-logging</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
</dependencies>
```

The Flyway plugin's database-driver dependencies must use these inherited Boot properties:

```xml
<version>${postgresql.version}</version>
<version>${mysql.version}</version>
<version>${mariadb.version}</version>
```

- [ ] **Step 3: Align JPA identity generation with the portable Flyway schema**

Replace the identifier annotations in `Entry.java` with:

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private int id;
```

Use explicit imports for `Entity`, `GeneratedValue`, `GenerationType`, `Id`, and `Table` instead of a wildcard import.

- [ ] **Step 4: Bring application and logging properties onto their current names**

Replace `src/main/resources/application.properties` with:

```properties
spring.jpa.hibernate.ddl-auto=${APP_DATABASE_SCHEMA:validate}
spring.datasource.url=${APP_DATABASE_URL:jdbc:h2:mem:entries}
spring.datasource.username=${APP_DATABASE_USER:sa}
spring.datasource.password=${APP_DATABASE_PASSWORD:}
spring.flyway.enabled=${APP_FLYWAY_ENABLED:true}
spring.flyway.locations=classpath:db/migration/common,classpath:db/migration/specific/{vendor}
management.endpoints.web.exposure.include=*
management.endpoint.prometheus.access=unrestricted
management.prometheus.metrics.export.enabled=true
```

Replace `src/test/resources/application.properties` with:

```properties
spring.jpa.hibernate.ddl-auto=${APP_DATABASE_SCHEMA:validate}
spring.datasource.url=${APP_DATABASE_URL:jdbc:h2:mem:test}
spring.datasource.username=${APP_DATABASE_USER:sa}
spring.datasource.password=${APP_DATABASE_PASSWORD:}
spring.flyway.enabled=${APP_FLYWAY_ENABLED:true}
```

In `src/main/resources/log4j2.xml`, change the root reference to match the declared appender:

```xml
<AppenderRef ref="json" />
```

- [ ] **Step 5: Run the complete Boot 3.5 checkpoint**

Run:

```bash
mvn clean test
```

Expected: all application, controller, and request-filter tests pass on Spring Boot 3.5.16; the missing-sequence error is absent.

- [ ] **Step 6: Inspect the intermediate logging graph**

Run:

```bash
mvn dependency:tree -Dincludes=ch.qos.logback:logback-classic,org.springframework.boot:spring-boot-starter-logging,org.apache.logging.log4j:log4j-slf4j-impl,org.apache.logging.log4j:log4j-slf4j2-impl
```

Expected: `log4j-slf4j2-impl` is present; Logback, `spring-boot-starter-logging`, and the old `log4j-slf4j-impl` bridge are absent.

- [ ] **Step 7: Commit the passing Boot 3.5 checkpoint**

```bash
git add pom.xml src/main/java/org/jeduardo/entries/model/Entry.java src/main/resources/application.properties src/main/resources/log4j2.xml src/test/resources/application.properties src/test/java/org/jeduardo/entries/ApplicationTest.java src/test/java/org/jeduardo/entries/controller/EntryControllerTest.java README.md
git commit -m "build: complete Spring Boot 3.5 migration"
```

### Task 3: Upgrade to Spring Boot 4.1 Modules

**Files:**
- Modify: `pom.xml`
- Modify: `src/test/java/org/jeduardo/entries/controller/EntryControllerTest.java`

- [ ] **Step 1: Switch the parent and starters to Boot 4.1**

Change the parent to `4.1.0`. Replace `spring-boot-starter-web` with `spring-boot-starter-webmvc`, replace `spring-boot-starter-test` with the two focused test starters below, and replace direct `flyway-core` with `spring-boot-starter-flyway`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>
    <scope>test</scope>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa-test</artifactId>
    <scope>test</scope>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

Keep `flyway-database-postgresql` and `flyway-mysql` beside the new starter. Add the same logging-starter exclusion to every direct Boot starter other than `spring-boot-starter-log4j2` so no alternate Maven dependency path restores Logback.

The final build must use only Boot 4.1's inherited managed properties for Flyway and the JDBC drivers. Do not override Boot 4.1's tested versions: Flyway 12.4.0, PostgreSQL 42.7.11, MySQL 9.7.0, MariaDB 3.5.8, H2 2.4.240, Commons Lang 3.20.0, and Log4j2 2.25.4.

- [ ] **Step 2: Run test compilation to expose Boot 4 package moves**

Run:

```bash
mvn test -DskipTests
```

Expected: compilation fails on the old `org.springframework.boot.test.autoconfigure.jdbc` and `org.springframework.boot.test.autoconfigure.web.servlet` imports.

- [ ] **Step 3: Update test imports for Boot 4 modularization**

In `EntryControllerTest.java`, replace the two moved imports with:

```java
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
```

Leave the CRUD assertions and application configuration unchanged.

- [ ] **Step 4: Compile and run the complete Boot 4 suite**

Run:

```bash
mvn clean test
```

Expected: all tests pass on Spring Boot 4.1.0.

- [ ] **Step 5: Commit the Boot 4 migration**

```bash
git add pom.xml src/test/java/org/jeduardo/entries/controller/EntryControllerTest.java
git commit -m "build: upgrade to Spring Boot 4.1"
```

### Task 4: Modernize CI and Dependabot

**Files:**
- Modify: `.github/workflows/test.yml`
- Modify: `.github/dependabot.yml`

- [ ] **Step 1: Update the Java 21 workflow**

Replace `.github/workflows/test.yml` with:

```yaml
name: Run tests

on:
  push:
  pull_request:

permissions:
  contents: read

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7
      - uses: actions/setup-java@v5
        with:
          distribution: "temurin"
          java-version: "21"
          cache: "maven"
      - name: Verify with Maven
        run: mvn --batch-mode clean verify
```

- [ ] **Step 2: Group compatible Maven updates and monitor Actions**

Replace `.github/dependabot.yml` with:

```yaml
version: 2
updates:
  - package-ecosystem: "maven"
    directory: "/"
    schedule:
      interval: "weekly"
    groups:
      production-minor-and-patch:
        dependency-type: "production"
        update-types:
          - "minor"
          - "patch"
  - package-ecosystem: "github-actions"
    directory: "/"
    schedule:
      interval: "weekly"
    groups:
      actions-minor-and-patch:
        update-types:
          - "minor"
          - "patch"
```

- [ ] **Step 3: Validate both YAML files structurally**

Run:

```bash
ruby -e 'require "yaml"; ARGV.each { |file| YAML.safe_load_file(file, aliases: true) }' .github/workflows/test.yml .github/dependabot.yml
```

Expected: exit code 0 with no parser errors.

- [ ] **Step 4: Commit automation updates**

```bash
git add .github/workflows/test.yml .github/dependabot.yml
git commit -m "ci: modernize dependency maintenance"
```

### Task 5: Audit and Verify the Finished Upgrade

**Files:**
- Modify only if verification exposes a scoped migration defect.

- [ ] **Step 1: Run the full local verification gate**

Run:

```bash
mvn clean verify
```

Expected: `BUILD SUCCESS`, with every test passing.

- [ ] **Step 2: Verify under an actual Java 21 runtime**

Run:

```bash
docker run --rm --volume "$PWD:/workspace" --workdir /workspace maven:3.9.11-eclipse-temurin-21 mvn --batch-mode clean verify
```

Expected: `BUILD SUCCESS` under Java 21. If the Docker daemon is unavailable, record that constraint and rely on the GitHub Actions Java 21 run before merging.

- [ ] **Step 3: Audit the resolved dependency graph**

Run:

```bash
mvn dependency:tree
```

Inspect the output and confirm:

- every `org.springframework.boot` artifact is version 4.1.0;
- no `javax.servlet`, `javax.persistence`, `junit-vintage-engine`, `logback-classic`, `spring-boot-starter-logging`, or `log4j-slf4j-impl` artifact is present;
- `log4j-slf4j2-impl` and `log4j-layout-template-json` are present;
- database and Flyway artifacts resolve to the Spring Boot 4.1 BOM versions documented in Task 3.

- [ ] **Step 4: Review stable dependency and plugin updates**

Run:

```bash
mvn org.codehaus.mojo:versions-maven-plugin:display-dependency-updates org.codehaus.mojo:versions-maven-plugin:display-plugin-updates -DallowSnapshots=false
```

Expected: Spring Boot 4.1.0 is the current stable parent. Newer individual libraries outside Boot's BOM may be reported; retain Boot 4.1's managed versions as deliberate compatibility choices rather than overriding the tested dependency set.

- [ ] **Step 5: Check formatting and final scope**

Run:

```bash
git diff --check
git status --short
git diff --stat HEAD~4..HEAD
```

Expected: no whitespace errors; only the dependency migration, compatibility fixes, tests, CI, Dependabot, README wording, and design/plan documentation are in scope.

- [ ] **Step 6: Commit any verification-driven correction**

If a scoped correction was necessary, stage only its exact files and commit it with:

```bash
git commit -m "fix: resolve Spring Boot 4 verification issue"
```

If verification required no correction, do not create an empty commit.
