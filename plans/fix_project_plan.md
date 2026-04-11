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

## Phase G: CORS & Reverse Proxy (Caddy)

> Configure CORS for multi-client access (web, mobile, Telegram bot) and add Caddy as a reverse proxy with automatic HTTPS.

### Step G.1 — Configure CORS in Spring Security
- Add CORS configuration to `SecurityConfig`:
  ```java
  @Bean
  CorsConfigurationSource corsConfigurationSource() { ... }
  ```
- Allowed origins from config: `app.cors.allowed-origins` (list)
  - Default: `http://localhost:3000` (dev web frontend)
  - Production: set via `${CORS_ALLOWED_ORIGINS}` env var (comma-separated)
- Allowed methods: `GET, POST, PUT, DELETE, OPTIONS`
- Allowed headers: `Authorization, Content-Type, X-API-Key`
- Expose headers: `X-Request-Id` (for tracing)
- `allowCredentials: true`
- Write test: preflight `OPTIONS` request returns correct CORS headers
- Write test: request from disallowed origin is rejected
- Write test: request from allowed origin passes

### Step G.2 — Add Caddy to production Docker Compose
- Create `infra/caddy/Caddyfile`:
  ```
  {$DOMAIN:localhost} {
      reverse_proxy app:8080
      encode gzip
      log {
          output stdout
          format json
      }
  }
  ```
- Caddy automatically provisions Let's Encrypt certificates when `DOMAIN` is a real domain
- For local dev, falls back to self-signed or plain HTTP
- Add `caddy` service to `docker-compose.prod.yml`:
  - Image: `caddy:2-alpine`
  - Ports: `80:80`, `443:443`
  - Volumes: `caddy_data` (certificates), `caddy_config`, Caddyfile mount
  - Depends on: `app`
- App container no longer exposes port 8080 externally in prod — only Caddy does

### Step G.3 — Add `DOMAIN` and `CORS_ALLOWED_ORIGINS` to `.env.example`
- `DOMAIN=` — production domain (e.g., `api.mybot.com`)
- `CORS_ALLOWED_ORIGINS=` — comma-separated allowed origins
- Document: if `DOMAIN` is empty, Caddy serves on HTTP only (dev mode)

---

## Phase H: Logging & Error Tracking

> Set up dual-format logging (text for dev, JSON for prod) and integrate Sentry for real-time error tracking.

### Step H.1 — Configure Logback for dual-format output
- Create `src/main/resources/logback-spring.xml`:
  ```xml
  <springProfile name="dev,default">
      <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
          <!-- Standard text pattern: timestamp level logger message -->
      </appender>
  </springProfile>
  <springProfile name="prod">
      <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
          <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
      </appender>
  </springProfile>
  ```
- Add `logstash-logback-encoder` dependency to `pom.xml`:
  ```xml
  <dependency>
      <groupId>net.logstash.logback</groupId>
      <artifactId>logstash-logback-encoder</artifactId>
      <version>7.4</version>
  </dependency>
  ```
- JSON log fields: `timestamp`, `level`, `logger`, `message`, `thread`, `traceId`, `spanId`
- Dev profile: human-readable text (current behavior)
- Prod profile: JSON (one line per log event, parseable by Loki/ELK)

### Step H.2 — Add request context to logs (MDC)
- Create `adapter/in/web/filter/RequestIdFilter.java` (servlet filter)
- Generates `X-Request-Id` (UUID) for each request, or reads from incoming header
- Puts `requestId`, `userEmail` (from JWT if present) into MDC
- Returns `X-Request-Id` in response header
- JSON logs automatically include MDC fields
- Write test: response contains `X-Request-Id` header
- Write test: MDC is cleaned up after request

### Step H.3 — Add Grafana + Loki to production Docker Compose
- Add to `docker-compose.prod.yml`:
  - **Loki** container (`grafana/loki:2.9.0`):
    - Port: `3100` (internal only)
    - Config: `infra/loki/loki-config.yml` (local filesystem storage, 30-day retention)
  - **Promtail** container (`grafana/promtail:2.9.0`):
    - Reads Docker container logs via Docker socket mount
    - Ships to Loki
    - Config: `infra/promtail/promtail-config.yml`
  - **Grafana** container (`grafana/grafana:10.0`):
    - Port: `3000:3000`
    - Pre-configured datasources: Loki + Prometheus
    - Admin password from `${GRAFANA_ADMIN_PASSWORD:-admin}`
    - Volume: `grafana_data` for persistence
- Create `infra/grafana/provisioning/datasources/datasources.yml` with Loki and Prometheus pre-configured

### Step H.4 — Integrate Sentry for error tracking
- Add Sentry Spring Boot dependency:
  ```xml
  <dependency>
      <groupId>io.sentry</groupId>
      <artifactId>sentry-spring-boot-starter-jakarta</artifactId>
      <version>7.6.0</version>
  </dependency>
  ```
- Add to `application.yml`:
  ```yaml
  sentry:
    dsn: ${SENTRY_DSN:}
    environment: ${SPRING_PROFILES_ACTIVE:dev}
    traces-sample-rate: 0.1
    send-default-pii: false
  ```
- If `SENTRY_DSN` is empty — Sentry is disabled (no-op), no errors on startup
- Add `SENTRY_DSN` to `.env.example`
- `send-default-pii: false` — do not send user emails/IPs to Sentry

### Step H.5 — Enrich Sentry events with context
- Create `adapter/out/observability/SentryUserFilter.java`:
  - Sets Sentry user context from JWT (anonymized — user ID only, no email)
  - Sets `requestId` tag from MDC
- Update `GlobalExceptionHandler`:
  - On 5xx errors, call `Sentry.captureException(ex)`
  - On 4xx errors — do NOT send to Sentry (reduce noise)
- Write test: 500 error triggers Sentry capture (mock Sentry hub)
- Write test: 400 error does NOT trigger Sentry capture

---

## Phase I: Resilience & Timeouts

> Protect the application from cascading failures when external services are slow or unavailable.

### Step I.1 — Configure graceful shutdown
- Add to `application.yml`:
  ```yaml
  server:
    shutdown: graceful
  spring:
    lifecycle:
      timeout-per-shutdown-phase: 30s
  ```
- In-flight requests get up to 30 seconds to complete before the JVM exits
- Update Dockerfile: add `STOPSIGNAL SIGTERM` (Docker sends SIGTERM, Spring catches it)
- Write test: verify `server.shutdown` property is set

### Step I.2 — Add LLM call timeout
- Add config property: `rag.llm.timeout-seconds: ${LLM_TIMEOUT:30}` (default 30s)
- Update `LlmClient` / `OpenRouterLlmAdapter`:
  - Set `WebClient` connect timeout: 5 seconds
  - Set read timeout: `timeout-seconds` from config
  - On timeout, throw `LlmUnavailableException` with clear message
- Write test: LLM call exceeding timeout throws `LlmUnavailableException`
- Write test: LLM call within timeout returns normally

### Step I.3 — Add Ollama embedding timeout
- Update `OllamaEmbeddingAdapter`:
  - Set timeout for embedding calls: 10 seconds (embeddings are fast)
  - On timeout, throw descriptive exception
- Write test: embedding timeout is handled gracefully

### Step I.4 — Implement exponential backoff with alerting for LLM failures
- Create `adapter/out/llm/LlmCircuitBreaker.java`:
  - Wraps `LlmPort` with retry logic
  - Retry strategy: exponential backoff (1s → 2s → 4s → 8s → 16s → 32s → 60s cap)
  - After 10 consecutive failures:
    - Log CRITICAL alert: `"LLM service unavailable after 10 retries"`
    - Send Sentry alert (if configured)
    - Increment Prometheus counter `rag.llm.circuit_breaker.open`
    - Enter "open" state — immediately return 503 for subsequent requests for 60 seconds
  - After cooldown, try one request ("half-open" state)
  - On success, reset failure counter ("closed" state)
- Wire as a decorator around `LlmPort` in `BeanConfiguration`
- Write test: first failure retries with backoff
- Write test: 10 failures opens circuit, returns 503 immediately
- Write test: circuit resets after cooldown + successful probe

### Step I.5 — Add request body size limit
- Add to `application.yml`:
  ```yaml
  spring:
    servlet:
      multipart:
        max-request-size: 1MB
        max-file-size: 1MB
  server:
    tomcat:
      max-http-form-post-size: 1MB
      max-swallow-size: 1MB
  ```
- The `@Size(max=4000)` on question DTO already limits question text — this is an additional guard at the HTTP layer
- Write test: request body exceeding 1MB returns 413 Payload Too Large

---

## Phase J: Data Management

> Implement database backups, data retention policies, and pagination for list endpoints.

### Step J.1 — Create PostgreSQL backup script
- Create `infra/backup/pg-backup.sh`:
  ```bash
  #!/usr/bin/env bash
  TIMESTAMP=$(date +%Y%m%d_%H%M%S)
  pg_dump -h db -U "$DB_USERNAME" "$DB_NAME" | gzip > "/backups/ai_helper_bot_${TIMESTAMP}.gz"
  # Rotate: delete backups older than 30 days
  find /backups -name "*.gz" -mtime +30 -delete
  ```
- Add backup container to `docker-compose.prod.yml`:
  - Uses `postgres:15-alpine` image (has `pg_dump` built in)
  - Runs `pg-backup.sh` via cron (daily at 02:00)
  - Volume: `db_backups:/backups` (named volume, persists on host)
  - Shares network with `db` container
  - Environment: `DB_USERNAME`, `DB_PASSWORD`, `DB_NAME`
- **How it works:** The backup container runs continuously with `crond` as its entrypoint. Each day at 02:00 it executes `pg_dump` against the PostgreSQL container, compresses the output with gzip, and saves it to a mounted volume. Backups older than 30 days are automatically deleted.

### Step J.2 — Document backup and restore procedures
- Add to README:
  - **Backup location:** Docker volume `db_backups` → mapped to host path
  - **Manual backup:** `docker compose exec backup /scripts/pg-backup.sh`
  - **Restore:** `gunzip -c backup.gz | docker compose exec -T db psql -U bot_user ai_helper_bot_db`
  - **Verify:** `docker compose exec db psql -U bot_user -c "SELECT count(*) FROM bot_feedback;"`
  - **Copy to host:** `docker cp <container>:/backups/. ./backups/`

### Step J.3 — Implement data retention service
- Create `domain/service/DataRetentionPolicy.java`:
  - Configurable retention period: `app.retention.feedback-days: ${RETENTION_FEEDBACK_DAYS:365}` (default 1 year)
  - Configurable max table size: `app.retention.feedback-max-rows: ${RETENTION_FEEDBACK_MAX_ROWS:100000}`
- Create `port/out/FeedbackCleanupPort.java`:
  - `int deleteOlderThan(LocalDateTime cutoff)`
  - `long countAll()`
- Add method to `FeedbackPersistenceAdapter` implementing `FeedbackCleanupPort`
- Create `adapter/out/scheduler/DataRetentionScheduler.java` (`@Scheduled`):
  - Runs daily at 03:00 (after backups)
  - Deletes feedback older than retention period
  - If row count exceeds max, deletes oldest rows beyond the limit
  - Logs how many rows were deleted
- Write test: records older than cutoff are deleted
- Write test: records within cutoff are preserved
- Write test: max-rows limit triggers cleanup of oldest excess rows

### Step J.4 — Add pagination to feedback list endpoint
- Create new endpoint: `GET /api/v1/botfeedback?page=0&size=20&sort=createdAt,desc`
- Add `GetFeedbackListUseCase` inbound port:
  - Method: `Page<Feedback> findAll(int page, int size, String sortBy, String direction)`
- Implement in `FeedbackService`
- Add `findAll(Pageable)` to `FeedbackPersistencePort`
- Implement in `FeedbackPersistenceAdapter` via Spring Data `Pageable`
- Create `FeedbackPageResponse` DTO with pagination metadata:
  ```json
  {
    "content": [...],
    "page": 0,
    "size": 20,
    "totalElements": 142,
    "totalPages": 8
  }
  ```
- Update `BotFeedbackControllerAdapter` with new mapping
- Write test: returns paginated results with correct metadata
- Write test: default page size is 20
- Write test: sort by `createdAt` desc works
- Update OpenAPI spec with new endpoint

---

## Phase K: Environment Profiles & Secret Management

> Set up dev/prod Spring profiles and migrate from `.env` files to Docker Secrets.

### Step K.1 — Create Spring profiles
- Create `application-dev.yml`:
  ```yaml
  spring.jpa.show-sql: true
  logging.level.com.nikiforov.aichatbot: DEBUG
  sentry.dsn: ""  # disabled in dev
  ```
- Create `application-prod.yml`:
  ```yaml
  spring.jpa.show-sql: false
  logging.level.com.nikiforov.aichatbot: INFO
  logging.level.root: WARN
  server.shutdown: graceful
  ```
- Set default profile in `application.yml`: `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:dev}`

### Step K.2 — Migrate to Docker Secrets for sensitive values
- **What are Docker Secrets:** Files mounted into the container at `/run/secrets/<name>`. More secure than env vars — not visible in `docker inspect`, not leaked in process listings or logs.
- Update `docker-compose.prod.yml`:
  ```yaml
  services:
    app:
      secrets:
        - db_password
        - openrouter_api_key
        - sentry_dsn
  secrets:
    db_password:
      file: ./secrets/db_password.txt
    openrouter_api_key:
      file: ./secrets/openrouter_api_key.txt
    sentry_dsn:
      file: ./secrets/sentry_dsn.txt
  ```
- Each secret is a plain text file with just the value (no `KEY=value`, no newline)
- Create `secrets/` directory, add to `.gitignore`
- Create `secrets/.example/` with empty placeholder files

### Step K.3 — Read Docker Secrets in Spring Boot
- Spring Boot can read secrets as files. Add to `application-prod.yml`:
  ```yaml
  spring:
    config:
      import: optional:configtree:/run/secrets/
  ```
- Or use `spring.datasource.password: ${DB_PASSWORD:}` and set `DB_PASSWORD_FILE=/run/secrets/db_password` with a config processor
- **Simpler approach:** Use Spring Boot's built-in `configtree` import — file name becomes property name:
  - `/run/secrets/db_password` → `db-password` property
  - Map in `application-prod.yml`: `spring.datasource.password: ${db-password}`
- Write test: app starts with secrets from file (integration test with temp files)

### Step K.4 — Update local dev to still use `.env`
- `docker-compose.yml` (dev) keeps using `.env` — no secrets infrastructure needed for local dev
- `docker-compose.prod.yml` (prod) uses Docker Secrets
- Document the difference in README

---

## Phase L: CI/CD with GitHub Actions

> Set up automated testing and Docker image build on push.

### Step L.1 — Create test workflow
- Create `.github/workflows/test.yml`:
  ```yaml
  name: Test
  on: [push, pull_request]
  jobs:
    test:
      runs-on: ubuntu-latest
      services:
        postgres:
          image: postgres:15
          env:
            POSTGRES_DB: ai_helper_bot_db
            POSTGRES_USER: bot_user
            POSTGRES_PASSWORD: test
          ports: ["5432:5432"]
          options: --health-cmd pg_isready --health-interval 10s --health-timeout 5s --health-retries 5
      steps:
        - uses: actions/checkout@v4
        - uses: actions/setup-java@v4
          with:
            distribution: temurin
            java-version: 17
            cache: maven
        - run: ./mvnw verify
          env:
            DB_PASSWORD: test
            OPEN_ROUTER_API_KEY: fake-key-for-tests
  ```
- Runs all unit + integration + architecture tests on every push and PR

### Step L.2 — Create Docker build & push workflow
- Create `.github/workflows/docker.yml`:
  ```yaml
  name: Docker Build
  on:
    push:
      branches: [main]
      tags: ["v*"]
  jobs:
    build:
      runs-on: ubuntu-latest
      steps:
        - uses: actions/checkout@v4
        - uses: docker/setup-buildx-action@v3
        - uses: docker/login-action@v3
          with:
            registry: ghcr.io
            username: ${{ github.actor }}
            password: ${{ secrets.GITHUB_TOKEN }}
        - uses: docker/build-push-action@v5
          with:
            push: true
            tags: ghcr.io/${{ github.repository }}:${{ github.sha }}
            cache-from: type=gha
            cache-to: type=gha,mode=max
  ```
- Builds Docker image and pushes to GitHub Container Registry (ghcr.io)
- Only on `main` branch and version tags
- Uses GitHub Actions cache for Docker layers

### Step L.3 — Add build status badge to README
- Add badge at the top of README: `![Tests](https://github.com/<owner>/<repo>/actions/workflows/test.yml/badge.svg)`

---

## Phase M: Documentation

> Create comprehensive README in Russian and English for non-technical stakeholders (PM, BA).

### Step M.1 — Create `README.md` (English)
- Structure:
  1. **Project Overview** — what the bot does, architecture diagram (text-based)
  2. **Quick Start (for developers)**
     - Prerequisites: Java 17, Docker, Git
     - Clone, `docker compose up -d`, `./mvnw spring-boot:run`
     - Verify: `curl http://localhost:8080/actuator/health`
  3. **API Reference** — all endpoints with curl examples
     - How to get a JWT token from Keycloak
     - Example: ask a question, save feedback, get feedback
  4. **Configuration** — all env variables with descriptions and defaults
  5. **Docker Deployment**
     - Dev: `docker compose up -d` (PostgreSQL + Keycloak only)
     - Prod: `docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d`
     - Secret management with Docker Secrets
  6. **Monitoring**
     - Health check URL
     - Prometheus scraping endpoint
     - Grafana dashboards
     - Sentry error tracking
  7. **Backup & Restore** — step-by-step with commands
  8. **Troubleshooting** — common issues and solutions
     - App can't connect to DB
     - Keycloak realm not imported
     - Ollama not running
     - LLM API key invalid
  9. **Architecture** — hexagonal architecture overview, package map
  10. **Contributing** — how to run tests, code style, PR process

### Step M.2 — Create `README.ru.md` (Russian)
- Full translation of `README.md`
- Same structure, same examples
- Link from `README.md`: "🇷🇺 [Русская версия](README.ru.md)"
- Link from `README.ru.md`: "🇬🇧 [English version](README.md)"

### Step M.3 — Create architecture diagram
- Create `docs/architecture.md` with text-based diagram:
  ```
  Client → Caddy (HTTPS) → Spring Boot App → PostgreSQL
                                ├── Keycloak (JWT validation)
                                ├── OpenRouter (LLM)
                                ├── Ollama (embeddings)
                                └── Azure Blob / Local FS
  ```
- Include hexagonal architecture diagram (ports & adapters)
- Reference from both READMEs

---

## Phase N: Final Verification

### Step N.1 — Run full test suite
- `./mvnw verify` — all unit, contract, integration, architecture tests pass
- Verify no test relies on `permitAll()` security (all updated for JWT mock)

### Step N.2 — Verify local dev workflow end-to-end
1. `docker compose up -d` — starts PostgreSQL + Keycloak
2. Keycloak auto-imports realm
3. `./mvnw spring-boot:run -Dspring.profiles.active=dev` — app starts, Liquibase runs
4. Obtain JWT: `POST http://localhost:8180/realms/ai-helper-bot/protocol/openid-connect/token`
5. `POST /api/v1/ask` with `Authorization: Bearer <jwt>` — returns answer
6. `POST /api/v1/ask` without token — returns 401
7. `GET /actuator/health` — returns UP
8. `GET /actuator/info` — returns build info

### Step N.3 — Verify production deployment end-to-end
1. Create secret files in `secrets/`
2. `docker compose -f docker-compose.yml -f docker-compose.prod.yml up --build -d`
3. Verify all containers start: PostgreSQL, Keycloak, app, Caddy, Loki, Promtail, Grafana, backup
4. HTTPS works via Caddy
5. Grafana shows logs from Loki and metrics from Prometheus
6. Sentry test: trigger a 500 error, verify it appears in Sentry dashboard

### Step N.4 — Update CLAUDE.md
- Add Docker section (commands to start/stop containers)
- Add Keycloak section (realm, clients, test user)
- Add auth section (JWT + API key strategy, public vs protected endpoints)
- Add Actuator section (exposed endpoints, security, Prometheus scraping)
- Add storage section (local vs Azure modes)
- Add CORS section (configuration, allowed origins)
- Add logging section (dev text vs prod JSON, Loki, Sentry)
- Add resilience section (timeouts, circuit breaker, graceful shutdown)
- Add data management section (backups, retention, pagination)
- Update REST API table with auth requirements and new endpoints
- Remove any stale references

### Step N.5 — Update ArchUnit tests
- Add rule: security adapters (`adapter/in/web/security/`) don't depend on domain services
- Add rule: `KeycloakJwtConverter` only depends on Spring Security types
- Add rule: scheduler classes are in `adapter/out/scheduler/`
- Add rule: filter classes are in `adapter/in/web/filter/`
- Verify all existing 13 rules still pass

---

## Relationship to Plan 2

This plan establishes the foundation that Plan 2 builds on:

| This Plan (Plan 3) | Enables in Plan 2 |
|---|---|
| Phase C: JWT auth | Phase 12: API key auth plugs into the same filter chain |
| Phase D: Dual auth architecture | Phase 13: Multi-tenancy resolves tenant from API key |
| Phase B: Docker + PostgreSQL | Phase 13: Tenant/API key entities need a real database |
| Phase E: Actuator health + Prometheus | Phase 19: Custom metrics and health indicators build on top |
| Phase F: Storage abstraction | Phase 20: Document source management per tenant |
| Phase G: CORS | Phase 15: Streaming SSE needs CORS for EventSource clients |
| Phase H: Structured logging + Sentry | Phase 18: Question analytics logs integrate with Loki |
| Phase I: LLM circuit breaker | Phase 14: LLM provider chain uses same resilience patterns |
| Phase J: Data retention | Phase 18: Question log needs same retention policies |
| Phase L: GitHub Actions CI | All phases: automated test validation on every change |

**Execution order:**
1. Plan 3, Phase A (cleanup) — immediate ✅
2. Plan 3, Phase B (Docker) — immediate ✅
3. Plan 3, Phase C (JWT auth) — before Plan 2
4. Plan 3, Phase D (dual auth bridge) — before Plan 2, Phase 12
5. Plan 3, Phase E (actuator) — before Plan 2, Phase 19
6. Plan 3, Phase F (storage abstraction) — before Plan 2, Phase 20
7. Plan 3, Phase G (CORS & Caddy) — before Plan 2, Phase 15
8. Plan 3, Phase H (logging & Sentry) — before Plan 2, Phase 18
9. Plan 3, Phase I (resilience) — before Plan 2, Phase 14
10. Plan 3, Phase J (data management) — before Plan 2, Phase 18
11. Plan 3, Phase K (profiles & secrets) — before prod deployment
12. Plan 3, Phase L (CI/CD) — as early as possible (amplifies all other phases)
13. Plan 3, Phase M (documentation) — after all features
14. Plan 3, Phase N (final verification) — last
15. Plan 2, Phases 12–22 (new features)

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
| G | 3 | CORS & Caddy reverse proxy with HTTPS |
| H | 5 | Logging (text + JSON), Grafana Loki, Sentry |
| I | 5 | Resilience — graceful shutdown, timeouts, circuit breaker |
| J | 4 | Data management — backups, retention, pagination |
| K | 4 | Environment profiles & Docker Secrets |
| L | 3 | CI/CD with GitHub Actions |
| M | 3 | Documentation (README ru/en, architecture diagram) |
| N | 5 | Final verification |
| **Total** | **69** | |
