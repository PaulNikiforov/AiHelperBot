# Plan 2: New Features on Hexagonal Architecture

**Prerequisite:** Plan 1 (hexagonal migration) is fully complete. The codebase follows domain/port/adapter structure with ArchUnit enforcement passing.

**Goal:** Implement all target features defined in the spec: multi-tenancy, LLM provider chain with failover, streaming responses, question analytics, rate limiting, prompt injection detection, API key authentication, and observability.

**Principles:**
- Every step follows TDD: Red test first, then Green, then Refactor
- Each step compiles and all tests pass before moving to the next
- New features are added by creating new ports, adapters, and domain services
- Existing domain core is extended, not restructured
- ArchUnit tests continue to pass after every step

---

## Phase 12: API Key Authentication & Security

**Note:** Phase C implemented JWT authentication with Keycloak OAuth2 Resource Server. This phase adds API key authentication as an alternative auth mechanism for service-to-service communication.

### Step 12.1 — Add Spring Security filter chain configuration for API key auth
- Security infrastructure exists at `adapter/in/web/security/SecurityConfig.java`
- Add a custom filter that reads `X-API-Key` header
- For now, accept a single static API key from configuration as a placeholder (TenantConfigPort replaces it in Phase 14)
- Write test: request without API key returns 401
- Write test: request with valid API key passes through
- Write test: request with invalid API key returns 401

### Step 12.2 — Create ApiKeyAuthFilter
- Create `adapter/in/web/security/ApiKeyAuthFilter.java`
- OncePerRequestFilter that extracts `X-API-Key` header
- Validates against configured key (placeholder)
- Sets SecurityContext with authenticated principal if valid
- Returns 401 JSON error if missing or invalid

### Step 12.3 — Create Unauthorized exception response body
- Ensure 401 responses match the spec error format: `{"errorCode": "UNAUTHORIZED", "message": "..."}`
- Write test: verify 401 response body structure

### Step 12.4 — Update OpenAPI spec for security
- Update `specs/api/openapi.yaml` — all endpoints already have `security: [ApiKeyAuth: []]`
- Verify SpringDoc UI shows the lock icon and accepts `X-API-Key`

---

## Phase 13: Multi-Tenancy Foundation

### Step 13.1 — Write tests for TenantId value object (RED)
- Create `domain/model/TenantIdTest.java`
- Test: stores UUID value
- Test: equality based on UUID
- Test: factory method from string

### Step 13.2 — Implement TenantId (GREEN)
- Create `domain/model/TenantId.java` — record wrapping UUID

### Step 13.3 — Write tests for Tenant entity (RED)
- Create `domain/model/TenantTest.java`
- Test: constructs with all fields (id, name, displayName, config, active flag)
- Test: inactive tenant is identifiable

### Step 13.4 — Implement Tenant entity (GREEN)
- Create `domain/model/Tenant.java` — domain entity

### Step 13.5 — Write tests for ApiKeyHash value object (RED)
- Create `domain/model/ApiKeyHashTest.java`
- Test: constructs from raw key (hashes it)
- Test: constructs from pre-computed hash
- Test: equality based on hash value

### Step 13.6 — Implement ApiKeyHash (GREEN)
- Create `domain/model/ApiKeyHash.java` — record with SHA-256 hashing

### Step 13.7 — Write tests for TenantConfigPort
- Define contract: `findByApiKey(String apiKeyHash)` returns `Optional<Tenant>`
- Define contract: `findById(TenantId id)` returns `Optional<Tenant>`

### Step 13.8 — Create TenantConfigPort interface
- Create `port/out/TenantConfigPort.java`
- Methods: `Optional<Tenant> findByApiKeyHash(String hash)`, `Optional<Tenant> findById(TenantId id)`

### Step 13.9 — Create tenant and api_key JPA entities
- Create `adapter/out/persistence/entity/TenantJpaEntity.java`
- Create `adapter/out/persistence/entity/ApiKeyJpaEntity.java`
- Map to existing `tenant` and `api_key` tables from Liquibase migration 002

### Step 13.10 — Create tenant and api_key JPA repositories
- Create `adapter/out/persistence/repository/TenantJpaRepository.java`
- Create `adapter/out/persistence/repository/ApiKeyJpaRepository.java`
- Spring Data JPA interfaces with query methods

### Step 13.11 — Create TenantPersistenceMapper
- Create `adapter/out/persistence/mapper/TenantPersistenceMapper.java`
- MapStruct mapper: domain `Tenant` maps to JPA `TenantJpaEntity` and back

### Step 13.12 — Create TenantConfigAdapter (GREEN)
- Create `adapter/out/persistence/TenantConfigAdapter.java`
- Implements `TenantConfigPort`
- Looks up api_key by hash, resolves tenant
- Wire in `BeanConfiguration`

### Step 13.13 — Write contract test for TenantConfigPort
- Create abstract `TenantConfigPortContractTest`
- Test: find tenant by valid API key hash
- Test: find tenant by invalid hash returns empty
- Test: find tenant by ID

### Step 13.14 — Write adapter integration test for TenantConfigAdapter
- Extends `TenantConfigPortContractTest`
- Uses TestContainers PostgreSQL
- Pre-inserts test tenant and API key

### Step 13.15 — Update ApiKeyAuthFilter to use TenantConfigPort
- Replace static API key validation with lookup via `TenantConfigPort`
- Extract API key from header, compute SHA-256 hash, look up in database
- Set tenant context on the request (ThreadLocal or SecurityContext)
- Write tests: valid tenant key passes, unknown key returns 401, inactive tenant returns 401

### Step 13.16 — Create TenantContext holder
- Create `domain/service/TenantContext.java` — ThreadLocal-based holder for current tenant
- Set by the security filter, read by domain services
- Includes cleanup on request completion

---

## Phase 14: LLM Provider Chain with Failover

### Step 14.1 — Write tests for LlmProviderChain (RED)
- Create `domain/service/LlmProviderChainTest.java`
- Use stub LlmPort implementations
- Test: primary succeeds — returns result immediately
- Test: primary fails, fallback succeeds — returns fallback result
- Test: all providers fail — throws `LlmUnavailableException`
- Test: provider reports unavailable via `isAvailable()` — skipped
- Test: empty chain — throws immediately

### Step 14.2 — Implement LlmProviderChain (GREEN)
- Create `adapter/out/llm/LlmProviderChain.java`
- Implements `LlmPort`
- Takes ordered list of `LlmPort` instances
- Iterates through providers on failure
- Logs which provider was used, which failed

### Step 14.3 — Refactor OpenRouterLlmAdapter to support multiple models
- The existing adapter talks to one model
- Make it configurable: model name, temperature, max tokens, timeout
- Each instance represents one provider+model combination

### Step 14.4 — Write tests for OllamaLlmAdapter (RED)
- Create `adapter/out/llm/OllamaLlmAdapterTest.java`
- Test: sends prompt to Ollama API
- Test: returns LlmResponse
- Test: reports unavailable when Ollama is down

### Step 14.5 — Create OllamaLlmAdapter (GREEN)
- Create `adapter/out/llm/OllamaLlmAdapter.java`
- Implements `LlmPort`
- Local Ollama as last-resort fallback
- Wire in configuration

### Step 14.6 — Update configuration for provider chain
- Add chain configuration to `application.yml` under `rag.llm.chain`
- Each entry: provider, model, base-url, api-key, timeout-seconds
- Update `LlmProperties` to parse chain list

### Step 14.7 — Wire provider chain in BeanConfiguration
- Construct `OpenRouterLlmAdapter` instances per chain entry
- Optionally construct `OllamaLlmAdapter`
- Wrap all in `LlmProviderChain`
- Register chain as the `LlmPort` bean

### Step 14.8 — Write integration test for provider chain
- Use WireMock to simulate primary failure
- Verify fallback to secondary
- Verify 503 when all fail

---

## Phase 15: Streaming Responses (SSE)

### Step 15.1 — Add WebFlux dependency (or use Spring MVC SSE)
- Evaluate: if WebFlux is too heavy for a single endpoint, use Spring MVC `SseEmitter` or `ResponseBodyEmitter`
- Add dependency if needed

### Step 15.2 — Create AskQuestionStreamUseCase inbound port
- Create `port/in/AskQuestionStreamUseCase.java`
- Method: `Flux<String> askStream(Question question)` (or equivalent reactive type)

### Step 15.3 — Create LlmStreamPort outbound port
- Create `port/out/LlmStreamPort.java`
- Method: `Flux<String> askStreaming(String systemMessage, String userMessage)`

### Step 15.4 — Write tests for streaming orchestration in RagOrchestrator
- Extend `RagOrchestrator` to implement `AskQuestionStreamUseCase`
- Validation and retrieval remain synchronous
- Only LLM generation is streamed
- Test with stub `LlmStreamPort`

### Step 15.5 — Create OpenRouter streaming adapter
- Create `adapter/out/llm/OpenRouterLlmStreamAdapter.java`
- Implements `LlmStreamPort`
- Uses OpenRouter streaming API (SSE from provider)
- Parses token-by-token responses

### Step 15.6 — Create streaming endpoint in BotQueryController
- Add `POST /api/v1/ask/stream` mapping
- Accept: `text/event-stream`
- Maps `Flux<String>` to SSE events
- Each event: `{"token": "...", "done": false}`
- Final event: `{"token": "", "done": true, "usage": {...}}`

### Step 15.7 — Write tests for streaming endpoint
- Test: returns SSE content type
- Test: each data line is valid JSON
- Test: final event has `done: true`
- Test: validation errors still return JSON 400

### Step 15.8 — Update OpenAPI spec
- Add `/api/v1/ask/stream` endpoint with SSE response schema
- Add `StreamToken` and `TokenUsage` schemas

---

## Phase 16: Rate Limiting

### Step 16.1 — Write tests for RateLimitExceededException (RED)
- Create `domain/exception/RateLimitExceededExceptionTest.java`
- Test: extends DomainException

### Step 16.2 — Implement RateLimitExceededException (GREEN)
- Create `domain/exception/RateLimitExceededException.java`

### Step 16.3 — Write tests for RateLimitFilter (RED)
- Create `adapter/in/web/security/RateLimitFilterTest.java`
- Test: requests under limit pass through
- Test: requests over limit return 429
- Test: rate limit is per-API-key (isolated counters)
- Test: counter resets after window expires

### Step 16.4 — Implement RateLimitFilter (GREEN)
- Create `adapter/in/web/security/RateLimitFilter.java`
- Sliding window rate limiter per API key
- Reads `rate_limit_rpm` from tenant configuration (via TenantConfigPort)
- Falls back to default RPM if not configured
- Returns 429 with `RATE_LIMIT_EXCEEDED` error code

### Step 16.5 — Add 429 mapping to GlobalExceptionHandler
- Map `RateLimitExceededException` to HTTP 429
- Error body: `{"errorCode": "RATE_LIMIT_EXCEEDED", "message": "..."}`

### Step 16.6 — Wire rate limit filter into security chain
- Add after API key filter, before request processing
- Only active when `rag.validation.rate-limit.enabled: true`

---

## Phase 17: Prompt Injection Detection

### Step 17.1 — Write tests for PromptInjectionDetector domain service (RED)
- Create `domain/validation/PromptInjectionDetectorTest.java`
- Test: detects "ignore previous instructions" pattern
- Test: detects "system:" prefix injection
- Test: detects role-playing attempts
- Test: allows legitimate questions about the system
- Test: returns pass for clean input
- Test: returns fail with reason for injected input

### Step 17.2 — Implement PromptInjectionDetector (GREEN)
- Create `domain/validation/PromptInjectionDetector.java`
- Pattern-based detection of known injection techniques
- Configurable pattern list
- Returns `ValidationResult`

### Step 17.3 — Add PromptInjectionDetector to InputValidationChain
- Insert after language detection, before domain relevance check
- Position: order 5 in the validation pipeline (see spec 10.2)
- Write integration test: injected input is rejected

### Step 17.4 — Add output sanitization
- Create `domain/service/OutputSanitizer.java`
- Strips leaked system prompt fragments from LLM responses
- Write tests: detects and removes system prompt leaks
- Integrate into `RagOrchestrator` after receiving LLM response

---

## Phase 18: Question Analytics

### Step 18.1 — Write tests for QuestionLogEntry value object (RED)
- Create `domain/model/QuestionLogEntryTest.java`
- Test: stores all fields (tenantId, question, answer, queryType, latency, tokens, etc.)

### Step 18.2 — Implement QuestionLogEntry (GREEN)
- Create `domain/model/QuestionLogEntry.java` — a record

### Step 18.3 — Create QuestionLogPort interface
- Already defined in Phase 3 of Plan 1: `port/out/QuestionLogPort.java`
- Method: `void log(QuestionLogEntry entry)`

### Step 18.4 — Create QuestionLog JPA entity and repository
- Create `adapter/out/persistence/entity/QuestionLogJpaEntity.java`
- Create `adapter/out/persistence/repository/QuestionLogJpaRepository.java`
- Maps to `question_log` table from Liquibase migration 006

### Step 18.5 — Create QuestionLogPersistenceMapper
- MapStruct mapper: domain `QuestionLogEntry` maps to JPA `QuestionLogJpaEntity`

### Step 18.6 — Create QuestionLogAdapter (GREEN)
- Create `adapter/out/persistence/QuestionLogAdapter.java`
- Implements `QuestionLogPort`
- Persists analytics via JPA repository
- Wire in `BeanConfiguration`

### Step 18.7 — Integrate analytics into RagOrchestrator
- Add `QuestionLogPort` as constructor dependency
- After each query (success or rejection), create and persist `QuestionLogEntry`
- Record: tenantId, question, queryType, validationResult, latencyMs, tokens
- Write tests with stub QuestionLogPort

### Step 18.8 — Write integration test for question logging
- TestContainers PostgreSQL
- Execute a query, verify `question_log` row is created with correct data

---

## Phase 19: Observability (Metrics & Health Checks)

### Step 19.1 — Add Micrometer Prometheus dependency
- Add `micrometer-registry-prometheus` to pom.xml (may already be transitively available via actuator)

### Step 19.2 — Define metric constants in domain
- Create `domain/service/MetricNames.java` — constants for all metric names from spec section 16.2
- `rag.query.total`, `rag.query.latency`, `rag.llm.tokens`, `rag.llm.errors`, `rag.validation.rejected`, `rag.feedback.total`

### Step 19.3 — Create MetricsPort outbound port
- Create `port/out/MetricsPort.java`
- Methods: `incrementCounter(String name, Map<String,String> tags)`, `recordTimer(String name, long durationMs, Map<String,String> tags)`

### Step 19.4 — Create MicrometerMetricsAdapter (GREEN)
- Create `adapter/out/observability/MicrometerMetricsAdapter.java`
- Implements `MetricsPort`
- Delegates to Micrometer `MeterRegistry`
- Wire in `BeanConfiguration`

### Step 19.5 — Integrate metrics into RagOrchestrator
- Add `MetricsPort` as constructor dependency
- Record query latency, token usage, validation rejections
- Tag by tenant, query type, status
- Write tests with stub MetricsPort

### Step 19.6 — Integrate metrics into FeedbackService
- Record feedback events (LIKE/DISLIKE) with tenant tag

### Step 19.7 — Create health check indicators
- Create `adapter/out/observability/VectorStoreHealthIndicator.java`
  - Checks `VectorSearchPort.isLoaded()`
  - Severity: CRITICAL
- Create `adapter/out/observability/LlmHealthIndicator.java`
  - Checks `LlmPort.isAvailable()`
  - Severity: CRITICAL
- Create `adapter/out/observability/EmbeddingHealthIndicator.java`
  - Checks `EmbeddingPort.isAvailable()`
  - Severity: HIGH
- Create `adapter/out/observability/BlobStorageHealthIndicator.java`
  - Checks `DocumentStoragePort.exists()` for a known path
  - Severity: MEDIUM

### Step 19.8 — Write tests for health indicators
- Test: vector store loaded returns UP
- Test: vector store empty returns DOWN
- Test: LLM available returns UP
- Test: LLM unavailable returns DOWN

### Step 19.9 — Expose actuator endpoints
- Verify `management.endpoints.web.exposure.include: health,metrics,prometheus`
- Verify `/actuator/health` shows all custom indicators
- Verify `/actuator/prometheus` exports custom metrics

---

## Phase 20: Document Source Management

### Step 20.1 — Write tests for DocumentSource entity (RED)
- Create `domain/model/DocumentSourceTest.java`
- Test: constructs with all fields
- Test: status transitions (PENDING to INDEXING to READY)
- Test: status transition to ERROR with reason

### Step 20.2 — Implement DocumentSource entity (GREEN)
- Create `domain/model/DocumentSource.java`

### Step 20.3 — Create DocumentSourcePort outbound port
- Create `port/out/DocumentSourcePort.java`
- Methods: `DocumentSource save(DocumentSource source)`, `Optional<DocumentSource> findById(UUID id)`, `List<DocumentSource> findByTenantId(TenantId tenantId)`

### Step 20.4 — Create DocumentSource JPA entity, repository, mapper
- Create `adapter/out/persistence/entity/DocumentSourceJpaEntity.java`
- Create `adapter/out/persistence/repository/DocumentSourceJpaRepository.java`
- Create `adapter/out/persistence/mapper/DocumentSourcePersistenceMapper.java`
- Maps to `document_source` table from Liquibase migration 004

### Step 20.5 — Create DocumentSourceAdapter (GREEN)
- Create `adapter/out/persistence/DocumentSourceAdapter.java`
- Implements `DocumentSourcePort`

### Step 20.6 — Integrate document source tracking into BootstrapIndexRunner
- After indexing, update `document_source` row status to READY and set `chunk_count`
- On error, set status to ERROR
- Record content hash, chunk_size, overlap

---

## Phase 21: Payload Sanitization (Input Filter)

### Step 21.1 — Write tests for PayloadSanitizer (RED)
- Create `adapter/in/web/security/PayloadSanitizerTest.java`
- Test: strips control characters
- Test: normalizes Unicode (NFC)
- Test: strips null bytes
- Test: preserves valid UTF-8

### Step 21.2 — Implement PayloadSanitizer (GREEN)
- Create `adapter/in/web/security/PayloadSanitizer.java`
- Servlet filter that sanitizes request body before controller receives it
- Operates at the adapter level (HTTP concern, not domain)

### Step 21.3 — Wire PayloadSanitizer into filter chain
- Position: after rate limiter, before controller
- Only sanitizes request bodies for `/api/v1/ask` and `/api/v1/ask/stream`

---

## Phase 22: Final Integration & Validation

### Step 22.1 — End-to-end test: authenticated ask flow
- Test: POST /api/v1/ask with valid API key returns 200
- Test: POST /api/v1/ask without API key returns 401
- Test: POST /api/v1/ask over rate limit returns 429

### Step 22.2 — End-to-end test: provider failover
- Primary LLM returns 500
- Fallback LLM returns 200
- Client receives 200 with answer
- Metrics record primary failure

### Step 22.3 — End-to-end test: streaming
- POST /api/v1/ask/stream with valid API key returns SSE
- Tokens stream one by one
- Final event has done=true

### Step 22.4 — End-to-end test: analytics
- Execute a query
- Verify `question_log` row is created
- Verify metrics counters are incremented

### Step 22.5 — End-to-end test: prompt injection rejection
- POST /api/v1/ask with injection attempt returns rejection
- Verify `validation.rejected` metric incremented
- Verify `question_log` records rejection

### Step 22.6 — Full ArchUnit validation
- Run all ArchUnit tests
- All dependency rules still pass
- New adapters, ports, domain classes all comply

### Step 22.7 — Full test suite
- Run `mvnw test`
- All unit, contract, integration, architecture tests pass

### Step 22.8 — Update CLAUDE.md
- Add new endpoints to REST API table
- Add streaming endpoint
- Add API key authentication documentation
- Add rate limiting documentation
- Add observability endpoints documentation
- Update configuration reference with new properties

### Step 22.9 — Update specs if needed
- Verify all spec files still match implementation
- Update `specs/09-api-specification.md` with new endpoints
- Update `specs/11-configuration.md` with new properties
- Update `specs/08-observability.md` if implementation differs

---

## Summary Statistics

| Phase | Steps | What |
|-------|-------|------|
| 12 | 4 | API key authentication |
| 13 | 16 | Multi-tenancy (entities, ports, adapters, integration) |
| 14 | 8 | LLM provider chain with failover |
| 15 | 8 | Streaming SSE responses |
| 16 | 6 | Rate limiting |
| 17 | 4 | Prompt injection detection + output sanitization |
| 18 | 8 | Question analytics |
| 19 | 9 | Observability (metrics + health checks) |
| 20 | 6 | Document source management |
| 21 | 3 | Payload sanitization |
| 22 | 9 | Final integration & validation |
| **Total** | **81** | |

---

## Combined Plan Statistics

| Plan | Phases | Steps | Focus |
|------|--------|-------|-------|
| Plan 1 | 0–11 | 96 | Hexagonal architecture migration |
| Plan 2 | 12–22 | 81 | New features on hexagonal foundation |
| **Total** | **0–22** | **177** | Full spec implementation |
