# Kassandra – Copilot Agent Instructions

Trust these instructions. Only search the codebase if information here is incomplete or appears incorrect.

---

## What This Repository Is

**Kassandra** is an open-source, self-hosted project management server written in Java. It provides effort estimation,
Gantt-chart generation, burn-down charts, sprint tracking, user/team management, OIDC authentication, and an AI
assistant. It is a single-jar Spring Boot + Vaadin application backed by an embedded H2 database.

---

## Technology Stack

| Layer      | Technology                                                                 |
|------------|----------------------------------------------------------------------------|
| Language   | Java 25 (Amazon Corretto 25.0.1)                                           |
| Build      | Apache Maven 3.8.5 (`pom.xml`)                                             |
| Framework  | Spring Boot 4.0.1                                                          |
| UI         | Vaadin 25.0.2 (Vaadin Flow + React frontend)                               |
| Database   | H2 (file-backed in production, in-memory for tests)                        |
| Security   | Spring Security + OIDC (Keycloak)                                          |
| AI         | Spring AI 2.0.0-M2 (OpenAI-compatible endpoint, e.g. LMStudio)             |
| Charts     | Apache Batik (SVG)                                                         |
| Testing    | JUnit 5, Spring Boot Test, Selenium 4, Testcontainers + Keycloak container |
| Code style | Spotless (Eclipse formatter for Java; Prettier 3.3.3 for TypeScript)       |
| Coverage   | JaCoCo 0.8.14                                                              |

---

## Key Source Paths

```
src/main/java/de/bushnaq/abdalla/kassandra/
  Application.java                        # Spring Boot entry point (@SpringBootApplication)
  config/                                 # KassandraProperties, DefaultEntitiesInitializer, CacheConfig
  dao/                                    # JPA entity classes (ProductDAO, SprintDAO, TaskDAO, …)
  dto/                                    # API DTOs (Product, Sprint, Task, User, …)
  repository/                             # Spring Data JPA repositories
  rest/
    controller/                           # REST controllers (@RequestMapping("/api/…"))
    api/                                  # REST client stubs used by tests (AbstractApi, ProductApi, …)
  security/                               # SecurityConfig, OidcSecurityConfig, SecurityUtils
  service/                                # AclSecurityService, ProductAclService, UserRoleService
  ui/
    view/                                 # Vaadin views (MainLayout, ProductListView, SprintListView, …)
    component/                            # Reusable Vaadin components
  report/
    gantt/ burndown/ calendar/            # SVG chart generators
  ai/                                     # Spring AI integration, TTS engines, MCP tools
    mcp/
      AiAssistantService.java             # Central AI chat service; wires all tool beans
      ApiConfiguration.java              # @Configuration – creates *ApiAdapter beans
      AgentThinking.java                 # Record injected by AugmentedToolCallbackProvider for inner reasoning
      QueryResult.java                   # Return type: final text + List<ThinkingStep>
      ThinkingStep.java                  # Records tool name + AgentThinking for one tool call
      SessionToolActivityContext.java    # Buffers + streams activity messages to UI
      ToolActivityContextHolder.java     # ThreadLocal holder for SessionToolActivityContext
      api/
        product/ feature/ version/       # *Tools beans (@Tool methods) + *ApiAdapter (REST calls)
        sprint/ user/ usergroup/
        AuthenticationProvider.java      # Supplies OIDC token for API calls

src/main/resources/
  application.properties                  # Runtime configuration
  META-INF/                               # Spring configuration

src/test/java/de/bushnaq/abdalla/kassandra/
  rest/api/                               # REST API unit tests (ProductApiTest, SprintApiTest, …)
  report/gantt/ burndown/ calendar/       # Chart regression tests
  ui/view/                                # Selenium UI tests (excluded from CI – see below)
  util/                                   # AbstractTestUtil, H2DatabaseStateManager, DTOAsserts
  ui/util/
    AbstractUiTestUtil.java               # Base class for all tests
    AbstractKeycloakUiTestUtil.java       # Base for Keycloak-authenticated UI tests

src/test/resources/
  application.properties                  # Test overrides (in-memory H2, OIDC stubs)
  keycloak/project-hub-realm.json         # Keycloak realm for Testcontainers
  .testcontainers.properties              # testcontainers.reuse.enable=true
```

---

## Build & Validate

### 1. Compile & package (skip tests)

```bash
mvn -B package --file pom.xml -DskipTests
```

- Produces `target/kassandra.jar` (Spring Boot fat JAR).
- Takes ~20 seconds on a warm Maven cache.
- **Always run this first** after cloning or changing dependencies.

### 2. Run fast unit tests only (recommended default)

```bash
mvn test -Dtest="ProductApiTest,SprintApiTest,CalendarTest"
```

- Pass any comma-separated list of `@Tag("UnitTest")` class names to `-Dtest=` to run only the fast, self-contained
  tests.
- No Docker, no Keycloak, no external services required. Completes in under 2 minutes.
- **Prefer this for validating code changes.**

### 3. Run the full CI-eligible test suite

```bash
mvn test -Dselenium.headless=true
```

- Runs all tests not excluded by Surefire file-pattern exclusions in `pom.xml` (see exclusion table below).
- Includes `UnitTest` and `IntegrationUiTest` classes; excludes `AiUnitTest` and `IntroductionVideo` automatically.
- Keycloak Testcontainer is pulled and started automatically on first run (~1–2 min extra).
- Run this when changing Vaadin views, security config, or anything that affects the rendered UI.
- Takes several minutes due to Keycloak container startup.

### 4. Run a specific test class

```bash
mvn test -Dtest="ProductApiTest,GanttTest"
```

### 5. Production build (Vaadin frontend bundled)

```bash
mvn -B package -P production -DskipTests
```

### 6. Site / coverage report

```bash
mvn jacoco:report site -DskipTests
```

Coverage report lands in `target/site/jacoco/jacoco.xml`.

### 7. Code formatting check

```bash
mvn spotless:check
```

Apply formatting:

```bash
mvn spotless:apply
```

- Java: Eclipse formatter config must be present at `eclipse-formatter.xml` in the repo root (referenced in `pom.xml`).
  If missing, `spotless:apply` will fail.
- TypeScript (`src/main/frontend/**/*.ts|.tsx`): Prettier 3.3.3, config at `.prettierrc.json`.

---

## Continuous Integration (GitHub Actions)

Workflow: `.github/workflows/maven-build.yml` – triggered on push/PR to `main`.

Steps (in order):

1. Checkout + configure Maven settings for GitHub Packages (`settings.xml` written dynamically).
2. Set up JDK 25 (Temurin) with Maven cache.
3. Set up Chrome stable + note that WebDriverManager handles ChromeDriver.
4. Configure Testcontainers (`testcontainers.reuse.enable=true`).
5. **`mvn -B package --file pom.xml -DskipTests`** – build only.
6. **`mvn test -Dselenium.headless=true`** – run tests; `AiUnitTest` and `IntroductionVideo` are excluded automatically
   via Surefire file-pattern exclusions in `pom.xml`.
7. `mvn jacoco:report site -DskipTests` – generate coverage.
8. Upload coverage to Codecov.
9. Publish JUnit XML reports (`**/target/surefire-reports/TEST-*.xml`).
10. Upload test artifacts (recordings, screenshots, reports) with 7-day retention.

**Secrets required:** `PACKAGES_TOKEN` (GitHub Packages), `CODECOV_ORG_TOKEN`.

---

## Test Tags

Tests are categorized using JUnit 5 `@Tag` annotations. Understanding these tags is critical for knowing what can run
where.

| Tag                 |  Can run in CI   | External dependency                       | Description                                                                                                                                                                                                                                                                                       |
|---------------------|:----------------:|-------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `UnitTest`          |      ✅ Yes       | None (in-memory H2, `@WithMockUser`)      | Self-contained tests. All `rest/api/*ApiTest` classes, SVG chart tests (`GanttTest`, `BurndownTest`, `CalendarTest`), date-util tests, profiler tests.                                                                                                                                            |
| `IntegrationUiTest` | ✅ Yes (headless) | Keycloak (auto-started via Testcontainer) | Full-stack Selenium UI tests against a live Spring Boot instance. Run headless in CI via `-Dselenium.headless=true`. All `ui/view/*ViewTest` classes.                                                                                                                                             |
| `AiUnitTest`        |       ❌ No       | LM Studio + AI containers (see below)     | Requires local AI services. Cannot run in GitHub Actions. All tests under `ai/` package, including `AiAssistantServiceBasicTest`, `AiAssistantServiceProductTest`, `AiAssistantServiceVersionTest`, `AiAssistantServiceFeatureTest`, `AiAssistantServiceUserTest`, `AiAssistantServiceMixedTest`. |
| `IntroductionVideo` |       ❌ No       | Real desktop browser + TTS containers     | Records introduction videos. Requires a visible browser and running TTS services. All `ui/introduction/*Video` classes.                                                                                                                                                                           |

### External Services Required by `AiUnitTest`

| Service                      | Default URL             | Docker compose location    | Purpose                                                        |
|------------------------------|-------------------------|----------------------------|----------------------------------------------------------------|
| LM Studio (LLM on local GPU) | `http://localhost:1234` | N/A – run manually         | Chat/completion AI for `AiAssistantService*` tests             |
| Chatterbox TTS container     | `http://localhost:4123` | `docker/chatterbox/`       | Text-to-speech for `TestChatterboxTTS` and introduction videos |
| Index TTS container          | (configured separately) | `docker/index-tts/`        | Alternative TTS engine for `TestIndexTTS`                      |
| Stable Diffusion container   | `http://localhost:7860` | `docker/stable-diffusion/` | Image generation for `StableDiffusionServiceTest`              |

Start Chatterbox: `cd docker/chatterbox && chatterbox-helper.bat start`
Start Stable Diffusion: `cd docker/stable-diffusion && docker-compose up -d`

### Keycloak in `IntegrationUiTest`

All `IntegrationUiTest` classes extend `AbstractKeycloakUiTestUtil`, which automatically:

- Starts a Keycloak Testcontainer (`quay.io/keycloak/keycloak:24.0.1`) before the Spring context.
- Loads the realm from `src/test/resources/keycloak/project-hub-realm.json`.
- Allocates a random server port stored in system property `test.server.port`.

No manual Keycloak setup is needed for `IntegrationUiTest`.

---

## Tests – What Is Excluded from Standard `mvn test`

The following are explicitly excluded from Surefire in `pom.xml` and **must not** be run in CI:

| Exclusion pattern                                                                               | Reason                                                     |
|-------------------------------------------------------------------------------------------------|------------------------------------------------------------|
| `**/LegacyGanttTest.java`                                                                       | Missing reference files not available in CI                |
| `**/ai/**/Test*.java`, `**/ai/**/*Test.java`, `**/ai/**/*Tests.java`, `**/ai/**/*TestCase.java` | Tagged `AiUnitTest` – requires LLM / GPU / AI containers   |
| `**/introduction/*Video.java`                                                                   | Tagged `IntroductionVideo` – requires real desktop browser |
| `**/ui/view/TaskListViewTest.java`                                                              | Waits for user to close browser                            |
| `**/ui/view/SprintQualityBoardTest.java`                                                        | Waits for user to close browser                            |
| `**/ui/Demo.java`                                                                               | Interactive demo                                           |

---

## Architecture Notes

- **Data model hierarchy:** `Product → Version → Feature → Sprint → Task/Story → Worklog`
- **REST API base path:** `/api` — each resource has its own controller and client stub.
- **Vaadin UI base path:** `/ui/*` (configured via `vaadin.url-mapping`).
- **Security:** Spring Security with OIDC. `SecurityConfig` configures the filter chain; `OidcSecurityConfig` /
  `OidcApiSecurityConfig` handle UI vs API paths. Tests use `@WithMockUser` and in-memory security.
- **Test database:** In-memory H2 (`jdbc:h2:mem:testdb`) with `create-drop` DDL. `AbstractTestUtil.beforeEach()`
  truncates all tables before each test using `SET REFERENTIAL_INTEGRITY FALSE`.
- **Keycloak UI tests:** `AbstractKeycloakUiTestUtil` starts a Keycloak Testcontainer (
  `quay.io/keycloak/keycloak:24.0.1`) with the realm from `src/test/resources/keycloak/project-hub-realm.json` and
  allocates a random port before the Spring context starts.
- **AI features:** `AiAssistantService` (in `ai/mcp/`) is the central AI service. It uses Spring AI's native `@Tool`
  annotation on dedicated tool beans (`ProductTools`, `ProductAclTools`, `UserTools`, `UserGroupTools`, `VersionTools`,
  `FeatureTools`, `SprintTools`) wired together via `AugmentedToolCallbackProvider` for per-call inner-thought
  reasoning. Each tool bean delegates to a corresponding `*ApiAdapter` (also in `ai/mcp/api/`) which calls the REST API
  using the current user's OIDC token via `AuthenticationProvider`. `ApiConfiguration` is a `@Configuration` class that
  wires up the `*ApiAdapter` beans. `AgentThinking` is a record injected as an extra argument by
  `AugmentedToolCallbackProvider` to capture the model's inner reasoning. `QueryResult` carries the final text response
  together with a list of `ThinkingStep` records for UI display. `SessionToolActivityContext` /
  `ToolActivityContextHolder` stream per-step activity messages to the UI. Spring AI BOM `2.0.0-M2` is resolved from
  the Spring Milestones repository.
- **Lombok:** Used extensively (`@Getter`, `@Setter`, `@Slf4j`, etc.). The compiler plugin configures it as an
  annotation processor path – do not add it as a regular dependency.
- **SVG Charts:** Generated server-side with Apache Batik 1.18 and compared against reference files in
  `test-reference-results/` during chart regression tests.

---

## Configuration Files

| File                                            | Purpose                                                                |
|-------------------------------------------------|------------------------------------------------------------------------|
| `pom.xml`                                       | All dependencies, plugins, profiles (`production`, `integration-test`) |
| `src/main/resources/application.properties`     | Runtime settings (H2 file DB, OIDC, AI endpoint, Vaadin)               |
| `src/test/resources/application.properties`     | Test overrides (in-memory H2, headless Vaadin, OIDC stubs)             |
| `src/test/resources/.testcontainers.properties` | `testcontainers.reuse.enable=true`                                     |
| `.prettierrc.json`                              | Prettier config: `singleQuote`, `printWidth=120`, `bracketSameLine`    |
| `eclipse-formatter.xml`                         | Eclipse Java formatter rules (referenced by Spotless)                  |
| `vite.config.ts` / `vite.generated.ts`          | Vaadin Vite bundler config                                             |
| `tsconfig.json`                                 | TypeScript compiler config for frontend code                           |
| `.github/workflows/maven-build.yml`             | CI pipeline definition                                                 |

---

## Common Pitfalls

- **`eclipse-formatter.xml` must exist** at repo root for `spotless:check` / `spotless:apply` to succeed on Java files.
- **Spring Milestones repository** is required to resolve `spring-ai-bom:2.0.0-M2` and related artifacts. It is declared
  in `pom.xml` under `<repositories>`.
- **`-Xshare:off`** is passed to Surefire via `argLine` to avoid JVM class sharing issues with Java 25.
- **Test isolation:** Each test extends `AbstractTestUtil` (or a subclass), which truncates all H2 tables in
  `@BeforeEach`. If you add a new test that needs data, do not rely on data left by a previous test.
- **Port allocation in Keycloak tests:** The server port is allocated before the Spring context starts using a
  `ServerSocket` in a static initializer in `AbstractKeycloakUiTestUtil`. The port is stored as system property
  `test.server.port`.
- **Vaadin dev mode:** `vaadin.devmode.devTools.enabled=false` is set in test `application.properties` to prevent the
  Vaadin dev server from starting during tests.
- **Running the application locally:** Default port `8080`. H2 file DB at `./db`. Vaadin UI at
  `http://localhost:8080/ui/`. Requires a running OIDC provider or a stub.

