# Plan 3: Project Fix — Security, Infrastructure & Cleanup

**Context:** This project was extracted from a larger SAS web application. It lacks its own authentication, has no containerized database, and carries some dead code. Plan 2 covers new features (streaming, analytics, rate limiting, etc.) — this plan focuses on making the project self-sufficient and secure before those features are built.

**Prerequisite:** Plan 1 (hexagonal migration) is complete.
**Sequencing:** This plan should be executed **before** Plan 2 phases 12–22, as Plan 2's API key auth (Phase 12) and multi-tenancy (Phase 13) build on the security foundation established here.

**Principles:**
- TDD: Red test first, then Green, then Refactor
- Each step compiles and all tests pass before moving to the next
- ArchUnit rules continue to pass after every step
- No secrets in config files — only `${ENV_VAR}` placeholders

---

## Phase A: Code Cleanup ✅ COMPLETE

> Remove dead code and fix inconsistencies left over from the extraction.
>
> **Status:** Completed 2026-04-11
> **Changes:** 6 files deleted, 4 files modified, all tests passing

### Step A.1 — Delete empty `service/rag/` directory
- Delete `src/main/java/com/nikiforov/aichatbot/service/rag/`
- Verify build still passes

### Step A.2 — Address unused QueryType result in RagOrchestrator
- `RagOrchestrator.ask()` calls `queryClassifier.classify()` but discards the result
- Add a `// TODO: use queryType for type-specific retrieval (Plan 2, Phase 20)` comment on the line
- Do NOT remove — this was a key part of the original logic and will be reconnected when `VectorSearchPort` gains page-filtered search
- Keep `QueryClassifier` and all `QueryType` enum values intact

### Step A.3 — Audit and update `pom.xml` description
- Change `<description>` from "SAS AI Helper Bot — extracted RAG/LLM service" to "AI Helper Bot — RAG/LLM chatbot service"
- Remove any remaining references to "SAS" or "Solbeg" in code, comments, or config files

### Step A.4 — Remove BotIntro endpoint and its entire stack
- The `GET /api/v1/bot/intro` endpoint returns a single static string from `application.yml` — this belongs on the frontend, not in a backend API
- **Delete** (8 files):
  - `adapter/in/web/BotIntroControllerAdapter.java` — REST controller
  - `adapter/in/config/BotIntroAdapter.java` — use case implementation
  - `adapter/in/web/dto/IntroResponse.java` — response DTO
  - `port/in/GetBotIntroUseCase.java` — inbound port interface
  - `config/properties/BotProperties.java` — `@ConfigurationProperties` for `bot.*`
  - Test: `adapter/in/web/BotIntroControllerAdapterTest.java`
- **Remove** from config files:
  - `bot.intro-text` property from `application.yml`
  - `bot.intro-text` property from `application-test.yml`
- **Remove** from OpenAPI spec:
  - `/api/v1/bot/intro` path and `IntroResponse` schema from `specs/api/openapi.yaml`
- **Remove** `GetBotIntroUseCase` bean wiring from `BeanConfiguration.java` (if present)
- **Update** `CLAUDE.md`:
  - Remove `GetBotIntroUseCase` from port interfaces list
  - Remove `BotIntroControllerAdapter` from inbound adapters table
  - Remove `BotIntroAdapter` description
  - Remove `GET /api/v1/bot/intro` from REST API table
  - Remove `IntroResponse` from web DTOs list
- Verify build + all tests pass after deletion

### Step A.5 — Verify OpenAPI spec alignment
- `specs/api/openapi.yaml` describes `/api/v1/ask/stream` — this is intentional (Plan 2, Phase 15 will implement it)
- Add a note at the top of the spec: `# Note: /ask/stream endpoint is planned but not yet implemented (see Plan 2, Phase 15)`
- Update contact info if it still references SAS/Solbeg

---

## Phase B: Docker & PostgreSQL Infrastructure ✅ COMPLETE

> Set up containerized PostgreSQL for local development and a Dockerfile for deployment.
>
> **Status:** Completed 2026-04-11
> **Changes:** 4 files created, 2 files modified, Docker infrastructure ready

### Step B.1 — Create `docker-compose.yml` for local development
- PostgreSQL 15 container with:
  - Database: `ai_helper_bot_db`
  - User: `bot_user` / password from `${DB_PASSWORD:-dev_password}` (dev default only in compose)
  - Port: `5432:5432`
  - Named volume for data persistence: `pgdata`
  - Health check: `pg_isready`
- The app is NOT in this compose file — developer runs it from IDE

### Step B.2 — Create `docker-compose.prod.yml` (override for deployment)
- Extends `docker-compose.yml`
- Adds the application service:
  - Builds from `Dockerfile`
  - Depends on `db` (healthy)
  - Environment variables: `DB_USERNAME`, `DB_PASSWORD`, `OPEN_ROUTER_API_KEY`, `AZURE_BLOB_URL`, `GUIDE_URL`, `LLM_BASE_URL`
  - Port: `8080:8080`
  - Connects to PostgreSQL via Docker network (service name `db`)
- Overrides `spring.datasource.url` to `jdbc:postgresql://db:5432/ai_helper_bot_db`

### Step B.3 — Create multi-stage `Dockerfile`
- **Stage 1 (build):** Maven build with `eclipse-temurin:17-jdk` base
  - Copy `pom.xml` + `mvnw` + `.mvn/` first (layer caching for dependencies)
  - `./mvnw dependency:go-offline`
  - Copy `src/` and build: `./mvnw clean package -DskipTests`
- **Stage 2 (runtime):** `eclipse-temurin:17-jre` base
  - Copy JAR from build stage
  - Non-root user (`appuser`)
  - Expose port 8080
  - Entry point: `java -jar app.jar`

### Step B.4 — Create `.env.example` file
- Template with all required environment variables (no values):
  ```
  DB_PASSWORD=
  OPEN_ROUTER_API_KEY=
  AZURE_BLOB_URL=
  GUIDE_URL=
  LLM_BASE_URL=
  LLM_MODEL=
  ```
- Add `.env` to `.gitignore` if not already present

### Step B.5 — Update `application.yml` datasource URL for flexibility
- Change datasource URL to: `jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/ai_helper_bot_db`
- This allows Docker Compose to override host to `db` without a separate config file
- Verify existing tests still pass (they use H2/TestContainers, unaffected)

### Step B.6 — Verify local dev workflow
- `docker compose up -d` starts PostgreSQL
- `./mvnw spring-boot:run` connects to containerized DB
- Liquibase migrations run, schema is created
- `POST /api/v1/ask` returns a response (may need Ollama running for full RAG, but app starts)

### Step B.7 — Verify deployment workflow
- `docker compose -f docker-compose.yml -f docker-compose.prod.yml up --build`
- App container starts, waits for DB health check
- Liquibase runs, all endpoints respond

---

## Phase C: JWT Authentication with Keycloak

> Add OAuth2/JWT authentication for user-facing access. API key auth for service-to-service is covered in Plan 2, Phase 12.

### Step C.1 — Add Keycloak to `docker-compose.yml`
- Keycloak 24+ container (`quay.io/keycloak/keycloak:24.0`):
  - Dev mode: `start-dev`
  - Port: `8180:8080` (avoid conflict with app on 8080)
  - Admin credentials from env vars: `${KEYCLOAK_ADMIN:-admin}` / `${KEYCLOAK_ADMIN_PASSWORD:-admin}`
  - PostgreSQL as Keycloak's DB (separate database `keycloak_db` in same PostgreSQL instance, or dedicated container — keep it simple with same PG instance)
- Add Keycloak env vars to `.env.example`

### Step C.2 — Create Keycloak realm configuration
- Create `infra/keycloak/realm-export.json` with:
  - Realm: `ai-helper-bot`
  - Client: `ai-helper-bot-api` (confidential, service accounts enabled)
  - Client: `ai-helper-bot-web` (public, for future frontend)
  - Default roles: `user`, `admin`
  - Test user: `testuser` / `test` (dev only, in realm export)
- Mount into Keycloak container via volume for auto-import on startup

### Step C.3 — Add OAuth2 Resource Server dependency
- Add to `pom.xml`:
  ```xml
  <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
  </dependency>
  ```

### Step C.4 — Configure JWT validation in `application.yml`
- Add:
  ```yaml
  spring:
    security:
      oauth2:
        resourceserver:
          jwt:
            issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8180/realms/ai-helper-bot}
            jwk-set-uri: ${KEYCLOAK_JWK_URI:http://localhost:8180/realms/ai-helper-bot/protocol/openid-connect/certs}
  ```
- Add `KEYCLOAK_ISSUER_URI` and `KEYCLOAK_JWK_URI` to `.env.example`

### Step C.5 — Write tests for JWT security configuration (RED)
- Create `src/test/java/.../config/JwtSecurityConfigTest.java`
- Test: request without token to `/api/v1/ask` returns 401
- Test: request with valid JWT to `/api/v1/ask` returns 200
- Test: request with expired JWT returns 401
- Test: request with JWT from wrong issuer returns 401
- Test: `/actuator/health` is accessible without token (public endpoint)
- Test: Swagger UI (`/swagger-ui/**`, `/v3/api-docs/**`) is accessible without token
- Use `spring-security-test` with `@WithMockUser` and mock JWT for unit tests

### Step C.6 — Update `SecurityConfig.java` (GREEN)
- Configure `SecurityFilterChain`:
  - Public endpoints (no auth required): `/actuator/health`, `/actuator/health/**`, `/actuator/info`, `/swagger-ui/**`, `/v3/api-docs/**`
  - Protected endpoints (JWT required): `/api/v1/ask`, `/api/v1/botfeedback/**`
  - OAuth2 Resource Server with JWT decoder
  - CSRF remains disabled (stateless API)
  - Session management: STATELESS
- Extract Keycloak roles from JWT claims (`realm_access.roles`)

### Step C.7 — Create `KeycloakJwtConverter` for role extraction
- Create `adapter/in/web/security/KeycloakJwtConverter.java`
- Implements `Converter<Jwt, AbstractAuthenticationToken>`
- Extracts `realm_access.roles` from Keycloak JWT
- Maps to Spring Security `GrantedAuthority` (prefixed with `ROLE_`)
- Write tests: valid JWT with roles maps correctly

### Step C.8 — Update `SpringSecurityIdentityAdapter`
- Currently returns `null` for unauthenticated users
- Update to extract email from JWT claims (`email` or `preferred_username`)
- Write test: with JWT containing email claim, `getCurrentUserEmail()` returns the email
- Write test: without JWT, returns `null` (for public endpoints)

### Step C.9 — Update integration tests
- Add test profile Keycloak configuration:
  - Option A: Use `spring-security-test` mock JWT (no real Keycloak in tests)
  - Option B: Use Keycloak TestContainer for integration tests
  - **Recommended: Option A** for speed — mock JWT in tests, real Keycloak only for manual/E2E testing
- Update existing `@WebMvcTest` tests to include JWT mock where needed
- Update `AskQuestionIT` and `BotFeedbackControllerAdapterIT` to send mock JWT

### Step C.10 — Update `application-test.yml`
- Add test JWT configuration:
  ```yaml
  spring:
    security:
      oauth2:
        resourceserver:
          jwt:
            issuer-uri: https://test-issuer.example.com
  ```
- Or disable JWT validation entirely in tests with a test `SecurityConfig` that uses `permitAll()`

---

## Phase D: Dual Auth Strategy — JWT + API Key (Bridge to Plan 2)

> Prepare the security chain to support both JWT (users) and API key (services). Plan 2, Phase 12 implements the full API key flow — this phase sets up the architecture.

### Step D.1 — Design the dual filter chain
- Document the intended auth flow:
  1. Request arrives
  2. If `Authorization: Bearer <jwt>` header present → JWT validation (Phase C)
  3. Else if `X-API-Key: <key>` header present → API key validation (Plan 2, Phase 12)
  4. Else → 401 Unauthorized
- No code yet for API key path — just the `SecurityFilterChain` structure that allows extension

### Step D.2 — Refactor SecurityConfig for extensibility (GREEN)
- Split into two `SecurityFilterChain` beans:
  - `jwtSecurityFilterChain` — handles requests with `Authorization` header, order 1
  - `apiKeySecurityFilterChain` — placeholder, order 2 (currently permits nothing, returns 401)
- This prepares the hook for Plan 2, Phase 12 to plug in `ApiKeyAuthFilter`
- Write test: JWT auth still works after refactor
- Write test: requests without any auth header return 401

### Step D.3 — Update GlobalExceptionHandler
- Add `401 UNAUTHORIZED` error response: `{"errorCode": "UNAUTHORIZED", "message": "..."}`
- Add `403 FORBIDDEN` error response: `{"errorCode": "FORBIDDEN", "message": "..."}`
- Write tests for new error responses

### Step D.4 — Update OpenAPI spec with security schemes
- Add both security schemes to `openapi.yaml`:
  ```yaml
  securitySchemes:
    BearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
    ApiKeyAuth:
      type: apiKey
      in: header
      name: X-API-Key
  ```
- Mark endpoints with `security: [BearerAuth: [], ApiKeyAuth: []]`

---

## Phase E: Actuator — Health & Prometheus Metrics

> Configure Spring Boot Actuator to expose only essential endpoints: health check and Prometheus metrics scraping. Custom application metrics and health indicators are deferred to Plan 2, Phase 19.

### Step E.1 — Add Micrometer Prometheus registry dependency
- `spring-boot-starter-actuator` is already in `pom.xml`
- Add Prometheus registry:
  ```xml
  <dependency>
      <groupId>io.micrometer</groupId>
      <artifactId>micrometer-registry-prometheus</artifactId>
  </dependency>
  ```

### Step E.2 — Configure Actuator in `application.yml`
- Expose `health`, `prometheus`, and `info` endpoints:
  ```yaml
  management:
    endpoints:
      web:
        exposure:
          include: health,prometheus,info
    endpoint:
      health:
        show-details: when_authorized
        show-components: when_authorized
        probes:
          enabled: true
    prometheus:
      metrics:
        export:
          enabled: true
    info:
      env:
        enabled: true
  ```
- `show-details: when_authorized` — anonymous users see UP/DOWN only, authenticated users see component details
- `probes.enabled: true` — exposes `/actuator/health/liveness` and `/actuator/health/readiness` groups

### Step E.3 — Configure liveness and readiness probe groups
- Add health group configuration to `application.yml`:
  ```yaml
  management:
    endpoint:
      health:
        group:
          liveness:
            include: livenessState
          readiness:
            include: readinessState,db
  ```
- **Liveness** — only checks if the JVM is alive (no external deps). If this fails, Docker should restart the container
- **Readiness** — checks if the app can serve traffic (DB must be UP). If this fails, load balancer should stop routing
- Update `Dockerfile` with `HEALTHCHECK`:
  ```dockerfile
  HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health/liveness || exit 1
  ```
- Write test: `/actuator/health/liveness` returns 200 without auth
- Write test: `/actuator/health/readiness` returns 200 when DB is up

### Step E.4 — Configure info endpoint with build and git metadata
- Add `build-info` goal to `spring-boot-maven-plugin` in `pom.xml`:
  ```xml
  <plugin>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-maven-plugin</artifactId>
      <executions>
          <execution>
              <goals>
                  <goal>build-info</goal>
              </goals>
          </execution>
      </executions>
  </plugin>
  ```
- Add `git-commit-id-maven-plugin`:
  ```xml
  <plugin>
      <groupId>io.github.git-commit-id</groupId>
      <artifactId>git-commit-id-maven-plugin</artifactId>
      <version>7.0.0</version>
      <executions>
          <execution>
              <goals><goal>revision</goal></goals>
          </execution>
      </executions>
      <configuration>
          <generateGitPropertiesFile>true</generateGitPropertiesFile>
          <failOnNoGitDirectory>false</failOnNoGitDirectory>
      </configuration>
  </plugin>
  ```
- `/actuator/info` will show: app name, version, build time, git branch, commit hash, commit time
- Write test: `/actuator/info` response contains `build.version` and `git.commit.id`

### Step E.5 — Secure Actuator endpoints in SecurityConfig
- `/actuator/health` — public (no auth), needed for Docker health checks and load balancers
- `/actuator/health/liveness` and `/actuator/health/readiness` — public (no auth), needed for container orchestration
- `/actuator/info` — public (no auth), contains only build metadata (no secrets)
- `/actuator/prometheus` — require `ROLE_admin` or a dedicated `ROLE_monitoring` role (prevent metric data leakage)
- All other `/actuator/**` endpoints — deny
- Write test: `/actuator/health` returns 200 without token
- Write test: `/actuator/health/liveness` returns 200 without token
- Write test: `/actuator/info` returns 200 without token
- Write test: `/actuator/prometheus` returns 200 with admin JWT
- Write test: `/actuator/prometheus` returns 401 without token
- Write test: `/actuator/env` returns 404 (not exposed)

### Step E.6 — Add database health indicator configuration
- Spring Boot auto-configures `DataSourceHealthIndicator` when JPA is present — verify it's active
- Health check should show: `db: UP` with PostgreSQL connection status
- Write test: `/actuator/health` response includes `status: UP` when DB is available

### Step E.7 — Verify Prometheus metrics output
- Start the app, hit `/actuator/prometheus`
- Confirm default JVM/HTTP metrics are present:
  - `jvm_memory_used_bytes`
  - `http_server_requests_seconds_count` / `http_server_requests_seconds_sum`
  - `hikaricp_connections_active` (connection pool)
  - `process_uptime_seconds`
- No custom application metrics yet — those come in Plan 2, Phase 19

---

## Phase F: Storage Abstraction (Future-Proofing)


> Prepare AzureBlobStorageAdapter for future replacement with any cloud or local storage.

### Step F.1 — Review `DocumentStoragePort` interface
- Verify the port interface is storage-agnostic (no Azure-specific types leak through)
- Current methods should be: `upload(path, data)`, `download(path)`, `exists(path)`, `delete(path)`
- If Azure types leak into the port, refactor to use domain types only

### Step F.2 — Create `LocalFileStorageAdapter` as a dev/test alternative
- Create `adapter/out/storage/LocalFileStorageAdapter.java`
- Implements `DocumentStoragePort`
- Stores files in a configurable local directory (`rag.storage.local.base-path`)
- Write tests: upload/download/exists/delete against temp directory

### Step F.3 — Make storage adapter selection config-driven
- Add config property: `rag.storage.type: ${STORAGE_TYPE:local}` (values: `local`, `azure`)
- In `BeanConfiguration`, conditionally create either `AzureBlobStorageAdapter` or `LocalFileStorageAdapter`
- Use `@ConditionalOnProperty` or explicit `if` in `@Bean` method
- Default to `local` — Azure only when `AZURE_BLOB_URL` is provided and `type=azure`

### Step F.4 — Update `.env.example` and docs
- Add `STORAGE_TYPE=` to `.env.example`
- Document the two storage modes

---

## Phase G: Final Verification & Documentation

### Step G.1 — Run full test suite
- `./mvnw test` — all unit, contract, integration, architecture tests pass
- Verify no test relies on `permitAll()` security (all updated for JWT mock)

### Step G.2 — Verify local dev workflow end-to-end
1. `docker compose up -d` — starts PostgreSQL + Keycloak
2. Keycloak auto-imports realm
3. `./mvnw spring-boot:run` — app starts, Liquibase runs
4. Obtain JWT: `POST http://localhost:8180/realms/ai-helper-bot/protocol/openid-connect/token` with client credentials
5. `POST /api/v1/ask` with `Authorization: Bearer <jwt>` — returns answer
6. `POST /api/v1/ask` without token — returns 401

### Step G.3 — Verify deployment workflow end-to-end
1. `docker compose -f docker-compose.yml -f docker-compose.prod.yml up --build`
2. All three containers start (PostgreSQL, Keycloak, app)
3. Same token flow works against `http://localhost:8080`

### Step G.4 — Update CLAUDE.md
- Add Docker section (commands to start/stop containers)
- Add Keycloak section (realm, clients, test user)
- Add auth section (JWT + API key strategy, public vs protected endpoints)
- Add Actuator section (exposed endpoints, security, Prometheus scraping)
- Add storage section (local vs Azure modes)
- Update REST API table with auth requirements
- Remove any stale references

### Step G.5 — Update ArchUnit tests
- Add rule: security adapters (`adapter/in/web/security/`) don't depend on domain services
- Add rule: `KeycloakJwtConverter` only depends on Spring Security types
- Verify all existing 13 rules still pass

---

## Relationship to Plan 2

This plan establishes the foundation that Plan 2 builds on:

| This Plan (Plan 3) | Enables in Plan 2 |
|---|---|
| Phase C: JWT auth | Phase 12: API key auth plugs into the same filter chain |
| Phase D: Dual auth architecture | Phase 13: Multi-tenancy resolves tenant from API key |
| Phase B: Docker + PostgreSQL | Phase 13: Tenant/API key entities need a real database |
| Phase B: Dockerfile | Phase 19: Observability needs containerized deployment |
| Phase E: Actuator health + Prometheus | Phase 19: Custom metrics and health indicators build on top |
| Phase F: Storage abstraction | Phase 20: Document source management per tenant |

**Execution order:**
1. Plan 3, Phase A (cleanup) — immediate
2. Plan 3, Phase B (Docker) — immediate
3. Plan 3, Phase C (JWT auth) — before Plan 2
4. Plan 3, Phase D (dual auth bridge) — before Plan 2, Phase 12
5. Plan 3, Phase E (actuator) — before Plan 2, Phase 19
6. Plan 3, Phase F (storage abstraction) — before Plan 2, Phase 20
7. Plan 3, Phase G (verification) — after all above
8. Plan 2, Phases 12–22 (new features)

---

## Summary Statistics

| Phase | Steps | What |
|-------|-------|------|
| A | 5 | Code cleanup (incl. BotIntro removal) |
| B | 7 | Docker & PostgreSQL infrastructure |
| C | 10 | JWT authentication with Keycloak |
| D | 4 | Dual auth strategy (JWT + API key bridge) |
| E | 7 | Actuator — health, probes, info & Prometheus metrics |
| F | 4 | Storage abstraction |
| G | 5 | Final verification & documentation |
| **Total** | **42** | |
