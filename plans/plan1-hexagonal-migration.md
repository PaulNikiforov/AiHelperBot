# Plan 1: Hexagonal Architecture Migration

**Goal:** Restructure the existing flat-layered codebase into hexagonal architecture (domain/port/adapter) with zero behavior change. All existing features must work identically after migration. New features (multi-tenancy, streaming, LLM chain, rate limiting, analytics) are deferred to Plan 2.

**Principles:**
- Every step follows TDD: Red test first, then Green, then Refactor
- Each step compiles and all tests pass before moving to the next
- Domain core has zero framework dependencies (no Spring, no JPA, no Spring AI)
- Adapters wrap existing infrastructure behind port interfaces
- Spring AI `Document` class is wrapped behind domain `DocumentChunk` in the domain layer
- Lombok stays everywhere; spec will be updated to reflect this

---

## Phase 0: Setup & Architecture Enforcement

### Step 0.1 — Add ArchUnit dependency to pom.xml
- Add `com.tngtech.archunit:archunit-junit5:1.2.1` test dependency
- Run `mvnw compile` to verify

### Step 0.2 — Create ArchUnit test scaffolding (RED)
- Create test class `HexagonalArchitectureTest` in `src/test/java/.../architecture/`
- Write ArchUnit rules for dependency direction: domain must not depend on adapter, Spring, JPA
- These tests must FAIL at this point because the code is still flat-layered
- Keep tests disabled until Phase 9 activates them

### Step 0.3 — Update specs: Lombok in domain
- Update `specs/code-conventions.md` to reflect that Lombok is allowed in the domain layer
- Update domain model examples in `specs/02-architecture.md` to show Lombok annotations where applicable

---

## Phase 1: Domain Models (Value Objects & Entities)

*Create domain model classes in `domain/model/` package. These are pure Java with zero framework imports. Existing classes remain untouched until Phase 10.*

### Step 1.1 — Write tests for Question value object (RED)
- Create test class `QuestionTest` in `src/test/java/.../domain/model/`
- Test: rejects null text
- Test: rejects blank text
- Test: accepts valid text
- These tests fail because `Question` class does not exist yet

### Step 1.2 — Implement Question value object (GREEN)
- Create `domain/model/Question.java` — a record with compact constructor validation
- All tests from Step 1.1 pass

### Step 1.3 — Write tests for Answer value object (RED)
- Create test class `AnswerTest`
- Test: `defaultResponse()` returns expected text and zero tokens
- Test: `unavailable()` returns expected text and zero tokens
- Test: stores text and token counts

### Step 1.4 — Implement Answer value object (GREEN)
- Create `domain/model/Answer.java` — a record with static factory methods
- All tests from Step 1.3 pass

### Step 1.5 — Write tests for DocumentChunk value object (RED)
- Create test class `DocumentChunkTest`
- Test: stores all fields (id, content, pageNumber, source, metadata)
- Test: allows empty metadata map

### Step 1.6 — Implement DocumentChunk value object (GREEN)
- Create `domain/model/DocumentChunk.java` — a record
- All tests from Step 1.5 pass

### Step 1.7 — Write tests for FeedbackId value object (RED)
- Create test class `FeedbackIdTest`
- Test: stores long value
- Test: equality based on value

### Step 1.8 — Implement FeedbackId value object (GREEN)
- Create `domain/model/FeedbackId.java` — a record wrapping a Long
- All tests pass

### Step 1.9 — Write tests for Feedback entity (RED)
- Create test class `FeedbackTest`
- Test: constructs with all fields
- Test: truncates answer to 10,000 characters on creation
- Test: allows null employeeEmail
- Test: answer exactly at limit is not truncated

### Step 1.10 — Implement Feedback entity (GREEN)
- Create `domain/model/Feedback.java` — a class (not record, because it is a mutable entity)
- Include truncation logic in constructor/factory
- All tests pass

### Step 1.11 — Write tests for FeedbackType enum (RED)
- Create test class `FeedbackTypeTest`
- Test: has exactly LIKE and DISLIKE values

### Step 1.12 — Implement FeedbackType enum (GREEN)
- Create `domain/model/FeedbackType.java`
- All tests pass

### Step 1.13 — Write tests for ValidationResult value object (RED)
- Create test class `ValidationResultTest`
- Test: `pass()` returns passed=true, reason=null
- Test: `fail(reason)` returns passed=false, reason is set
- Test: `fail(null)` or `fail(blank)` throws

### Step 1.14 — Implement ValidationResult value object (GREEN)
- Create `domain/model/ValidationResult.java` — a record with factory methods
- All tests pass

### Step 1.15 — Write tests for QueryType enum (RED)
- Create test class `QueryTypeTest`
- Test: has exactly the 9 expected values: GENERAL, DEFINITION, TIME_RELATED, SUPPORT, INBOX, PEERS, REVIEW_PROCESS, REMIND_PEERS, GROWTH_PLAN

### Step 1.16 — Implement QueryType enum (GREEN)
- Create `domain/model/QueryType.java`
- All tests pass

### Step 1.17 — Write tests for LlmResponse value object (RED)
- Create test class `LlmResponseTest`
- Test: stores answer text, promptTokens, completionTokens, providerId

### Step 1.18 — Implement LlmResponse value object (GREEN)
- Create `domain/model/LlmResponse.java` — a record
- All tests pass

---

## Phase 2: Domain Exceptions

### Step 2.1 — Write tests for DomainException (RED)
- Create test class `DomainExceptionTest`
- Test: constructor with message
- Test: constructor with message and cause

### Step 2.2 — Implement DomainException (GREEN)
- Create `domain/exception/DomainException.java`
- All tests pass

### Step 2.3 — Write tests for FeedbackNotFoundException (RED)
- Create test class `FeedbackNotFoundExceptionTest`
- Test: extends DomainException
- Test: constructs with feedback ID in message

### Step 2.4 — Implement FeedbackNotFoundException (GREEN)
- Create `domain/exception/FeedbackNotFoundException.java`
- All tests pass

### Step 2.5 — Write tests for LlmUnavailableException (RED)
- Create test class `LlmUnavailableExceptionTest`
- Test: extends DomainException
- Test: constructs with message

### Step 2.6 — Implement LlmUnavailableException (GREEN)
- Create `domain/exception/LlmUnavailableException.java`
- All tests pass

---

## Phase 3: Port Interfaces ✅ DONE

*Define all port interfaces. These have zero implementation — just contracts.*

### Step 3.1 — Create inbound port: AskQuestionUseCase
- Create `port/in/AskQuestionUseCase.java`
- Single method: `Answer ask(Question question)` throwing `LlmUnavailableException`

### Step 3.2 — Create inbound port: SaveFeedbackUseCase
- Create `port/in/SaveFeedbackUseCase.java`
- Method: `Feedback save(String question, String answer, FeedbackType type, String employeeEmail)`

### Step 3.3 — Create inbound port: GetFeedbackUseCase
- Create `port/in/GetFeedbackUseCase.java`
- Method: `Feedback getById(FeedbackId id)` throwing `FeedbackNotFoundException`

### Step 3.4 — Create inbound port: GetBotIntroUseCase
- Create `port/in/GetBotIntroUseCase.java`
- Method: `String getIntroText()`

### Step 3.5 — Create outbound port: LlmPort
- Create `port/out/LlmPort.java`
- Methods: `String id()`, `LlmResponse ask(String systemMessage, String userMessage)`, `boolean isAvailable()`

### Step 3.6 — Create outbound port: EmbeddingPort
- Create `port/out/EmbeddingPort.java`
- Methods: `List<float[]> embed(List<String> texts)`, `int dimensions()`, `boolean isAvailable()`

### Step 3.7 — Create outbound port: VectorSearchPort
- Create `port/out/VectorSearchPort.java`
- Methods: `List<DocumentChunk> search(String query, int topK)`, `List<DocumentChunk> findByPageNumber(int pageNumber)`, `boolean isLoaded()`

### Step 3.8 — Create outbound port: VectorIndexPort
- Create `port/out/VectorIndexPort.java`
- Methods: `void index(List<DocumentChunk> chunks)`, `void clear()`

### Step 3.9 — Create outbound port: FeedbackPersistencePort
- Create `port/out/FeedbackPersistencePort.java`
- Methods: `Feedback save(Feedback feedback)`, `Optional<Feedback> findById(FeedbackId id)`

### Step 3.10 — Create outbound port: DocumentStoragePort
- Create `port/out/DocumentStoragePort.java`
- Methods: `byte[] download(String path)`, `void upload(String path, byte[] data)`, `boolean exists(String path)`

### Step 3.11 — Create outbound port: LanguageDetectionPort
- Create `port/out/LanguageDetectionPort.java`
- Method: `Optional<String> detect(String text)`

### Step 3.12 — Verify all ports compile
- Run `mvnw compile`
- Ports should compile cleanly — they only depend on `domain.model` and `domain.exception`

---

## Phase 4: Domain Services (extract business logic) ✅ DONE

*Move business logic from existing `service/` classes into domain services in `domain/service/` and `domain/validation/`. Domain services depend ONLY on ports and domain models — no Spring, no infrastructure.*

### Step 4.1 — Write tests for InputValidationChain (RED)
- Create `domain/validation/InputValidationChainTest.java`
- Use a hand-written stub implementing a simplified validator interface
- Test: all validators pass — returns pass
- Test: first validator fails — short-circuits, returns fail with reason
- Test: second validator fails, first passes — returns fail with reason from second
- Test: empty validator list — returns pass
- These tests fail because `InputValidationChain` does not exist yet

### Step 4.2 — Implement InputValidationChain (GREEN)
- Create `domain/validation/InputValidationChain.java`
- Takes a list of validator functions/strategy interface
- Sequential execution, short-circuit on first failure
- All tests pass

### Step 4.3 — Write tests for FormatValidator domain service (RED)
- Create `domain/validation/FormatValidatorTest.java`
- Test: rejects null
- Test: rejects blank
- Test: rejects input shorter than min-length
- Test: rejects input with no Unicode letters
- Test: accepts valid input

### Step 4.4 — Implement FormatValidator (GREEN)
- Create `domain/validation/FormatValidator.java`
- Pure domain logic, takes `minLength` as constructor parameter
- No Spring, no `@Component`, no `RagValidationProperties` — just an int parameter
- All tests pass

### Step 4.5 — Write tests for QueryClassifier domain service (RED)
- Create `domain/service/QueryClassifierTest.java`
- Test: "What is a review?" produces DEFINITION with extracted term "review"
- Test: "How do I schedule a review?" produces REVIEW_PROCESS
- Test: "Who are my peers?" produces PEERS
- Test: "What is the deadline?" produces TIME_RELATED with extracted term
- Test: "Tell me about growth plan" produces GROWTH_PLAN
- Test: "Random question" produces GENERAL
- Test: all 9 query types covered
- Tests fail because `QueryClassifier` does not exist

### Step 4.6 — Implement QueryClassifier (GREEN)
- Create `domain/service/QueryClassifier.java`
- Move regex-based classification logic from existing `QueryAnalyzer`
- Also move `extractTerm` logic
- Takes no infrastructure dependencies — pure regex matching
- All tests pass

### Step 4.7 — Write tests for DocumentRanker domain service (RED)
- Create `domain/service/DocumentRankerTest.java`
- Use stub `VectorSearchPort` for page lookups
- Test: `rankByKeywords` scores documents by query word overlap
- Test: `rankByTimeRelevance` boosts documents with date patterns
- Test: `mergeWithPage` prepends page-specific docs, deduplicates, respects limit

### Step 4.8 — Implement DocumentRanker (GREEN)
- Create `domain/service/DocumentRanker.java`
- Takes `VectorSearchPort` as constructor dependency (for page lookups)
- Move ranking logic from existing `DocumentRanker`
- Operates on domain `DocumentChunk` instead of Spring AI `Document`
- All tests pass

### Step 4.9 — Write tests for PromptAssembler domain service (RED)
- Create `domain/service/PromptAssemblerTest.java`
- Test: builds system message with behavioral rules
- Test: builds user message with question + page-labeled context
- Test: handles empty context gracefully

### Step 4.10 — Implement PromptAssembler (GREEN)
- Create `domain/service/PromptAssembler.java`
- Move prompt-building logic from existing `PromptBuilder`
- Operates on domain `DocumentChunk` instead of Spring AI `Document`
- Returns a record/DTO holding `systemMessage` and `userMessage` strings
- All tests pass

### Step 4.11 — Write tests for FeedbackService domain service (RED)
- Create `domain/service/FeedbackServiceTest.java`
- Use stub `FeedbackPersistencePort`
- Test: truncates answer to 10,000 chars on save
- Test: answer at exact limit is not truncated
- Test: delegates to persistence port after validation
- Test: retrieves by ID, throws `FeedbackNotFoundException` when not found

### Step 4.12 — Implement FeedbackService (GREEN)
- Create `domain/service/FeedbackService.java`
- Implements `SaveFeedbackUseCase` and `GetFeedbackUseCase`
- Takes `FeedbackPersistencePort` as constructor dependency
- Contains truncation logic
- All tests pass

### Step 4.13 — Write tests for RagOrchestrator domain service (RED)
- Create `domain/service/RagOrchestratorTest.java`
- Use stub implementations for all outbound ports
- Test: vector store not loaded — returns `Answer.unavailable()`
- Test: input validation fails — returns rejection message in answer
- Test: no documents found — returns `Answer.defaultResponse()`
- Test: happy path — returns LLM answer
- Test: DEFINITION unsatisfactory — retries with GENERAL strategy
- Test: LLM failure — throws `LlmUnavailableException`

### Step 4.14 — Implement RagOrchestrator (GREEN)
- Create `domain/service/RagOrchestrator.java`
- Implements `AskQuestionUseCase`
- Takes constructor dependencies: `InputValidationChain`, `QueryClassifier`, `VectorSearchPort`, `DocumentRanker`, `PromptAssembler`, `LlmPort`
- Orchestrates: validate, classify, retrieve, rank, build prompt, ask LLM, fallback
- All tests pass

---

## Phase 5: Outbound Adapters (wrap existing infrastructure)

*Each adapter implements an outbound port and wraps existing infrastructure code. Existing classes are repurposed, not deleted yet.*

### Step 5.1 — Write contract test for LlmPort (RED)
- Create `port/out/LlmPortContractTest.java` — abstract test class
- Test: `ask()` returns non-null `LlmResponse`
- Test: `id()` returns non-null string
- Test: `isAvailable()` returns boolean

### Step 5.2 — Create OpenRouterLlmAdapter (GREEN)
- Create `adapter/out/llm/OpenRouterLlmAdapter.java`
- Implements `LlmPort`
- Wraps existing `LlmClient` internally (delegates to it)
- Maps between domain `LlmResponse` and existing `ChatCompletionResponse`
- Wire in `BeanConfiguration`

### Step 5.3 — Write contract test for VectorSearchPort + VectorIndexPort (RED)
- Create `port/out/VectorSearchPortContractTest.java` — abstract
- Test: `search()` returns list of `DocumentChunk`
- Test: `findByPageNumber()` returns list
- Test: `isLoaded()` returns boolean
- Create `port/out/VectorIndexPortContractTest.java` — abstract
- Test: `index()` then `search()` returns indexed data
- Test: `clear()` removes all data

### Step 5.4 — Create InMemoryVectorStoreAdapter (GREEN)
- Create `adapter/out/vectorstore/InMemoryVectorStoreAdapter.java`
- Implements both `VectorSearchPort` and `VectorIndexPort`
- Wraps existing `CustomSimpleVectorStore` and `EmbeddingIndexer`
- Maps between domain `DocumentChunk` and Spring AI `Document`
- Wire in `BeanConfiguration`

### Step 5.5 — Write contract test for FeedbackPersistencePort (RED)
- Create `port/out/FeedbackPersistencePortContractTest.java` — abstract
- Test: `save()` and `findById()` round-trip
- Test: `findById()` with non-existent ID returns empty

### Step 5.6 — Create FeedbackPersistenceAdapter (GREEN)
- Create `adapter/out/persistence/FeedbackPersistenceAdapter.java`
- Implements `FeedbackPersistencePort`
- Wraps existing `BotFeedbackRepository`
- Maps between domain `Feedback`/`FeedbackId` and JPA `BotFeedback`
- Wire in `BeanConfiguration`

### Step 5.7 — Create FeedbackPersistenceMapper
- Create `adapter/out/persistence/mapper/FeedbackPersistenceMapper.java`
- MapStruct mapper: domain `Feedback` maps to JPA `BotFeedback` and back
- Uses existing mapping logic from `BotFeedbackMapper`

### Step 5.8 — Write contract test for DocumentStoragePort (RED)
- Create `port/out/DocumentStoragePortContractTest.java` — abstract
- Test: `download()`, `upload()`, `exists()` round-trip

### Step 5.9 — Create AzureBlobStorageAdapter (GREEN)
- Create `adapter/out/storage/AzureBlobStorageAdapter.java`
- Implements `DocumentStoragePort`
- Wraps existing `AzureBlobStorageService`
- Wire in `BeanConfiguration`

### Step 5.10 — Write contract test for LanguageDetectionPort (RED)
- Create `port/out/LanguageDetectionPortContractTest.java` — abstract
- Test: detects English text — returns "en"
- Test: detects non-English — returns different code
- Test: uncertain input — returns empty

### Step 5.11 — Create LinguaLanguageAdapter (GREEN)
- Create `adapter/out/language/LinguaLanguageAdapter.java`
- Implements `LanguageDetectionPort`
- Wraps existing Lingua `LanguageDetector` bean
- Wire in `BeanConfiguration`

### Step 5.12 — Create EmbeddingAdapter
- Create `adapter/out/embedding/OllamaEmbeddingAdapter.java`
- Implements `EmbeddingPort`
- Wraps Spring AI `EmbeddingModel` bean
- Wire in `BeanConfiguration`

---

## Phase 6: Inbound Adapters (REST controllers)

*Rewire controllers to call inbound ports instead of directly calling service implementations.*

### Step 6.1 — Create web DTOs in adapter layer
- Create `adapter/in/web/dto/AskRequest.java` — record with `question` field, validation annotations
- Create `adapter/in/web/dto/AskResponse.java` — record with `answer` field
- Create `adapter/in/web/dto/FeedbackRequest.java` — record with `question`, `answer`, `botFeedbackType`
- Create `adapter/in/web/dto/FeedbackResponse.java` — record with all feedback fields
- Create `adapter/in/web/dto/IntroResponse.java` — record with `introText`

### Step 6.2 — Create web mappers
- Create `adapter/in/web/mapper/QuestionWebMapper.java` — maps `AskRequest` to domain `Question`
- Create `adapter/in/web/mapper/FeedbackWebMapper.java` — maps between web DTOs and domain models

### Step 6.3 — Write tests for BotQueryController adapter (RED)
- Create `adapter/in/web/BotQueryControllerTest.java`
- Mock `AskQuestionUseCase` port
- Test: POST /api/v1/ask with valid question returns 200 with answer
- Test: POST with blank question returns 400
- Test: LLM unavailable returns 503

### Step 6.4 — Create BotQueryController adapter (GREEN)
- Create `adapter/in/web/BotQueryController.java`
- Injects `AskQuestionUseCase` (port interface, not `RagService`)
- Maps request to domain `Question`, calls `ask()`, maps `Answer` to response

### Step 6.5 — Write tests for BotFeedbackController adapter (RED)
- Create `adapter/in/web/BotFeedbackControllerTest.java`
- Mock `SaveFeedbackUseCase` and `GetFeedbackUseCase` ports
- Test: POST /api/v1/botfeedback returns 201
- Test: GET /api/v1/botfeedback/{id} returns 200
- Test: GET non-existent returns 404

### Step 6.6 — Create BotFeedbackController adapter (GREEN)
- Create `adapter/in/web/BotFeedbackController.java`
- Injects `SaveFeedbackUseCase` and `GetFeedbackUseCase`
- Maps between web DTOs and domain models

### Step 6.7 — Write tests for BotIntroController adapter (RED)
- Create `adapter/in/web/BotIntroControllerTest.java`
- Mock `GetBotIntroUseCase`
- Test: GET /api/v1/bot/intro returns 200 with intro text

### Step 6.8 — Create BotIntroController adapter (GREEN)
- Create `adapter/in/web/BotIntroController.java`
- Injects `GetBotIntroUseCase`
- Maps to `IntroResponse`

### Step 6.9 — Update GlobalExceptionHandler
- Move to `adapter/in/web/GlobalExceptionHandler.java`
- Add mapping for `FeedbackNotFoundException` to 404
- Add mapping for `LlmUnavailableException` to 503
- Keep existing mappings for validation errors and generic exceptions
- Ensure exception to HTTP status mapping is consistent with spec error codes

---

## Phase 7: Configuration & Wiring

### Step 7.1 — Create BeanConfiguration
- Create `config/BeanConfiguration.java`
- Wires domain services to their adapter implementations
- Explicit Spring `@Configuration` that constructs domain services with port implementations

### Step 7.2 — Create GetBotIntroUseCase implementation
- Create a simple domain service or adapter that implements `GetBotIntroUseCase`
- Reads intro text from `BotProperties` configuration
- This is the only case where a domain use case reads directly from config — wire through a simple adapter

### Step 7.3 — Update existing controller endpoint paths
- The existing `BotTopicsController` served `/api/v1/bot/topics`
- The new `BotIntroController` serves `/api/v1/bot/intro` per spec
- Ensure path matches spec; decide on backward compatibility

### Step 7.4 — Verify compilation
- Run `mvnw compile`
- Both old and new packages coexist temporarily
- No old code is deleted yet

---

## Phase 8: Integration Tests

### Step 8.1 — Port existing integration tests
- Update `BotFeedbackControllerIT` to use new controller path and DTOs
- Update `BotFeedbackServiceIT` to test through domain `FeedbackService` instead of `BotFeedbackServiceImpl`
- Ensure TestContainers configuration still works

### Step 8.2 — Write end-to-end integration test
- Create `AskQuestionIT.java` — full pipeline test with mocked LLM and vector store
- Verify POST /api/v1/ask returns expected answer

### Step 8.3 — Run all tests
- Run `mvnw test`
- All unit tests (domain), contract tests, integration tests must pass

---

## Phase 9: Activate Architecture Tests

### Step 9.1 — Enable ArchUnit tests from Step 0.2
- Remove disabled/skip annotations from `HexagonalArchitectureTest`
- Run ArchUnit tests — they should now PASS because:
  - `domain.*` has zero Spring/JPA/adapter imports
  - `port.in.*` depends only on `domain.model`
  - `port.out.*` depends only on `domain.model`
  - `adapter.in.*` depends on `port.in` and `domain.model`
  - `adapter.out.*` depends on `port.out` and `domain.model`

### Step 9.2 — Run ArchUnit and fix any violations
- If any violations found, fix the offending import
- Re-run until all pass

---

## Phase 10: Delete Old Code

*Only after ALL tests pass (unit + contract + integration + ArchUnit).*

### Step 10.1 — Delete old controllers
- Delete `controller/BotQueryController.java`
- Delete `controller/BotFeedbackController.java`
- Delete `controller/BotTopicsController.java`

### Step 10.2 — Delete old service interfaces and implementations
- Delete `service/BotFeedbackService.java`
- Delete `service/BotFeedbackServiceImpl.java`
- Delete `service/BotTopicsService.java`
- Delete `service/BotTopicsServiceImpl.java`
- Delete `service/security/SecurityUtils.java`

### Step 10.3 — Delete old RAG service classes
- Delete `service/rag/RagService.java`
- Delete `service/rag/QueryAnalyzer.java` (replaced by `domain/service/QueryClassifier`)
- Delete `service/rag/QueryType.java` (replaced by `domain/model/QueryType`)
- Delete `service/rag/DocumentRetriever.java` (logic moved to `RagOrchestrator`)
- Delete `service/rag/DocumentRanker.java` (replaced by `domain/service/DocumentRanker`)
- Delete `service/rag/PromptBuilder.java` (replaced by `domain/service/PromptAssembler`)
- Delete `service/rag/LlmClient.java` (wrapped in `OpenRouterLlmAdapter`)
- Delete `service/rag/validation/InputValidator.java` (replaced by `domain/validation/InputValidationChain`)
- Delete `service/rag/validation/InputFilter.java` (replaced by domain validator interface)
- Delete `service/rag/validation/FilterResult.java` (replaced by `domain/model/ValidationResult`)
- Delete `service/rag/validation/QuickInputFilter.java` (logic in `domain/validation/FormatValidator`)
- Delete `service/rag/validation/LanguageDetectorFilter.java` (wrapped in adapter)
- Delete `service/rag/validation/DomainRelevanceChecker.java` (logic in `RagOrchestrator`)

### Step 10.4 — Delete old DTOs and mappers
- Delete `dto/request/BotFeedbackRequest.java`
- Delete `dto/request/LlmRequest.java`
- Delete `dto/response/BotFeedbackResponse.java`
- Delete `dto/response/BotTopicsResponse.java`
- Delete `dto/response/LlmResponse.java`
- Delete `dto/llm/ChatCompletionRequest.java`
- Delete `dto/llm/ChatCompletionResponse.java`
- Delete `mapper/BotFeedbackMapper.java`

### Step 10.5 — Delete old model classes
- Delete `model/BotFeedback.java` (replaced by domain `Feedback` + JPA entity in adapter)
- Delete `model/BotFeedbackType.java` (replaced by domain `FeedbackType`)

### Step 10.6 — Repurpose infrastructure classes into adapter packages
- `EmbeddingIndexer` — move to `adapter/out/vectorstore/` (internal to adapter)
- `CustomSimpleVectorStore` — move to `adapter/out/vectorstore/` (internal to adapter)
- `PdfChunker` — move to `adapter/out/vectorstore/` (internal to adapter)
- `BootstrapIndexRunner` — move to `adapter/out/vectorstore/` (internal to adapter)
- `AzureBlobStorageService` — fold into `AzureBlobStorageAdapter`
- `GlossaryTerms` — move to `domain/validation/` or `domain/service/` (pure static data)

### Step 10.7 — Delete old exception classes
- Delete `exceptionhandler/exception/CodedException.java`
- Delete `exceptionhandler/exception/RestException.java`
- Delete `exceptionhandler/exception/LlmException.java`
- Delete `exceptionhandler/ErrorCodes.java`
- Delete `exceptionhandler/GlobalExceptionHandler.java` (already replaced)

### Step 10.8 — Delete empty old packages
- Remove `controller/`, `dto/`, `exceptionhandler/`, `mapper/`, `model/`, `repository/`, `service/` packages
- Only `domain/`, `port/`, `adapter/`, `config/` remain at the top level

### Step 10.9 — Update SecurityConfig
- Ensure `SecurityConfig` allows all endpoints (current behavior — no auth yet)
- Auth/API keys deferred to Plan 2

### Step 10.10 — Update test configuration
- Update `application-test.yml` if needed for new package structure
- Ensure `@MockBean` references point to correct adapter classes

---

## Phase 11: Final Validation

### Step 11.1 — Full test suite
- Run `mvnw test`
- All unit tests (domain) pass
- All contract tests pass
- All integration tests pass
- ArchUnit tests pass

### Step 11.2 — Application starts successfully
- Run `mvnw spring-boot:run` (with mocked Ollama and Azure)
- Verify all 4 endpoints respond:
  - POST /api/v1/ask
  - POST /api/v1/botfeedback
  - GET /api/v1/botfeedback/{id}
  - GET /api/v1/bot/intro

### Step 11.3 — Update CLAUDE.md
- Update package structure section to reflect hexagonal layout
- Update REST API table (endpoint `/bot/topics` to `/bot/intro`)
- Update RAG pipeline flow to reference domain services by new names
- Update configuration prefix references if changed
- Remove references to deleted classes

---

## Summary Statistics

| Phase | Steps | What |
|-------|-------|------|
| 0 | 3 | Setup, ArchUnit scaffolding, spec update |
| 1 | 18 | Domain value objects and entities (9 pairs of RED/GREEN) |
| 2 | 6 | Domain exceptions (3 pairs) |
| 3 | 12 | Port interfaces (12 ports) |
| 4 | 14 | Domain services with TDD (7 pairs) |
| 5 | 12 | Outbound adapters with contract tests (6 pairs) |
| 6 | 9 | Inbound adapters (controllers + DTOs + tests) |
| 7 | 4 | Configuration and wiring |
| 8 | 3 | Integration tests |
| 9 | 2 | Activate ArchUnit |
| 10 | 10 | Delete old code |
| 11 | 3 | Final validation |
| **Total** | **96** | |
