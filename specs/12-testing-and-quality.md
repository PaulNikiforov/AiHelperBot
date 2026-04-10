# 12. Testing Strategy & Quality

---

## 20. Testing Strategy

### 20.1 Test Pyramid

```
         /  E2E Tests  \           ~5%   (full Spring context + TestContainers)
        / Adapter Tests  \         ~25%  (real infra via TestContainers/WireMock)
       / Port Contract Tests\      ~20%  (verify adapter conforms to port contract)
      / Domain Unit Tests     \    ~40%  (pure JUnit, no Spring)
     / Architecture Tests       \  ~10%  (ArchUnit dependency enforcement)
```

### 20.2 Domain Unit Tests

**What**: Test domain services, value objects, entities. Pure business logic.

**How**: JUnit 5 + AssertJ. NO Spring context. NO mocks of ports (pass test doubles implementing port interfaces).

**Current inventory (65 tests):**

| Test class | Tests | What |
|-----------|-------|------|
| `QuestionTest` | 3 | Null/blank rejection, valid text |
| `AnswerTest` | 3 | `defaultResponse()`, `unavailable()`, stores text/tokens |
| `DocumentChunkTest` | 2 | Stores all fields, empty metadata |
| `FeedbackIdTest` | 2 | Stores value, equality |
| `FeedbackTest` | 4 | Construction, truncation, null email, exact limit |
| `FeedbackTypeTest` | 1 | Has LIKE and DISLIKE |
| `ValidationResultTest` | 4 | `pass()`, `fail()`, fail with null/blank throws |
| `QueryTypeTest` | 1 | Has 9 expected values |
| `LlmResponseTest` | 1 | Stores answer text, tokens, provider ID |
| `DomainExceptionTest` | 2 | Constructor with message, with cause |
| `FeedbackNotFoundExceptionTest` | 2 | Extends DomainException, message contains ID |
| `LlmUnavailableExceptionTest` | 2 | Extends DomainException, message |
| `InputValidationChainTest` | 4 | All pass, first fail, second fail, empty list |
| `FormatValidatorTest` | 5 | Null, blank, short, no letters, valid |
| `QueryClassifierTest` | 12 | All 9 types, extractTerm (def, time), fallback |
| `DocumentRankerTest` | 4 | Keywords, time relevance, merge with page, dedup |
| `PromptAssemblerTest` | 2 | With context, empty context |
| `FeedbackServiceTest` | 5 | Truncation, exact limit, delegation, found, not-found |
| `RagOrchestratorTest` | 5 | Not loaded, validation fail, no docs, happy path, LLM failure |

**Examples**:
```java
// RagOrchestratorTest.java
@Test
void ask_whenVectorStoreNotLoaded_returnsUnavailable() {
    var vectorSearch = new StubVectorSearchPort(loaded = false);
    var orchestrator = new RagOrchestrator(vectorSearch, ...);

    Answer answer = orchestrator.ask(new Question("How do I..."));

    assertThat(answer).isEqualTo(Answer.unavailable());
}

@Test
void ask_whenNoDocsFound_returnsDefault() { ... }

@Test
void ask_whenDefinitionUnsatisfactory_retriesWithGeneral() { ... }
```

```java
// FeedbackServiceTest.java
@Test
void save_truncatesAnswerTo10000() {
    var persistence = new InMemoryFeedbackPersistence();
    var service = new FeedbackService(persistence);

    Feedback fb = service.save("Q?", "x".repeat(15_000), FeedbackType.LIKE, null);

    assertThat(fb.getAnswer()).hasSize(10_000);
}
```

### 20.3 Port Contract Tests

**What**: Define the behavioral contract for each port. Written once. Run against every adapter.

**How**: Abstract test class parameterized by the port implementation.

**Current inventory (6 contract tests, 15 abstract tests):**

| Contract test | Tests | Verified by |
|--------------|-------|-------------|
| `LlmPortContractTest` | 3 | `OpenRouterLlmAdapterTest` (@Disabled) |
| `VectorSearchPortContractTest` | 3 | `InMemoryVectorStoreAdapterTest` (@Disabled) |
| `VectorIndexPortContractTest` | 2 | (no concrete test yet) |
| `FeedbackPersistencePortContractTest` | 2 | `FeedbackPersistenceAdapterTest` (@Disabled) |
| `DocumentStoragePortContractTest` | 2 | (no concrete test yet) |
| `LanguageDetectionPortContractTest` | 3 | (no concrete test yet) |

All adapter contract tests are `@Disabled` — they require live infrastructure (Ollama, OpenRouter, database, Azure Blob). Database contract is now covered by `FeedbackServiceIT` integration test. Other contracts will be activated with TestContainers/WireMock in Phase 11 (final validation).

```java
// FeedbackPersistencePortContract.java
abstract class FeedbackPersistencePortContract {

    abstract FeedbackPersistencePort createPort();

    @Test
    void save_andFindById_returnsEqual() {
        var port = createPort();
        var feedback = new Feedback(...);

        Feedback saved = port.save(feedback);
        Optional<Feedback> found = port.findById(saved.getId());

        assertThat(found).isPresent().get().isEqualTo(saved);
    }

    @Test
    void findById_nonExistent_returnsEmpty() {
        var port = createPort();
        assertThat(port.findById(new FeedbackId(999L))).isEmpty();
    }
}
```

Each adapter test extends the contract:

```java
// FeedbackPersistenceAdapterTest.java (with TestContainers)
@SpringBootTest
@Testcontainers
class FeedbackPersistenceAdapterTest extends FeedbackPersistencePortContract {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:15");

    @Autowired FeedbackPersistenceAdapter adapter;

    @Override
    FeedbackPersistencePort createPort() { return adapter; }
}
```

### 20.4 Adapter Integration Tests

**What**: Test adapters against real infrastructure.

**Tools**:
- **Database**: TestContainers PostgreSQL
- **HTTP services**: WireMock (for OpenRouter, Ollama)
- **Blob storage**: TestContainers Azurite or mock

**Current inventory (22 tests, Phase 8-10):**

| Test class | Tests | What |
|-----------|-------|------|
| `integration/AskQuestionIT` | 6 | Valid question, blank/null, short, no letters, too long validation |
| `integration/BotFeedbackControllerAdapterIT` | 7 | Valid save, existing GET, 404 GET, blank question, null type, answer too long |
| `integration/FeedbackServiceIT` | 6 | Truncation at limit, long answer truncation, short answer, getById found, getById not found, null email |
| `BotIntroControllerAdapterTest` | 2 | GET returns intro text, null returns empty |
| `BotQueryControllerAdapterTest` | 4 | Valid question, validation failures, LLM unavailable |

All integration tests use:
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@Transactional
@TestPropertySource(properties = {
    "spring.liquibase.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.ai.ollama.embedding.enabled=false",
    "rag.storage.blob.url=",
    "app.adapters.inbound.enabled=true"
})
```

**PostgreSQL TestContainers**:
- PostgreSQL 15 with `@ServiceConnection` for auto-configuration
- `@Transactional` for test isolation
- `@MockBean` for external dependencies (EmbeddingModel, AzureBlobStorageService)

**Old integration tests** (to be deleted in Phase 10):
- `BotFeedbackControllerIT` — Tests old controller (dormant)
- `BotFeedbackServiceIT` — Tests old service (dormant)

### 20.5 Architecture Tests (ArchUnit) — Phase 9 COMPLETE

**What**: Enforce hexagonal architecture dependency rules at compile/test time.

**Current inventory (12 rules, Phase 9):**

| Rule | Enforces |
|------|----------|
| `domain_mustNotDependOn_adapters` | Domain → No adapter imports |
| `domain_mustNotDependOn_spring` | Domain → No Spring framework |
| `domain_mustNotDependOn_persistence` | Domain → No JPA/Hibernate |
| `portIn_mustNotDependOn_portOut` | Inbound ports → No outbound ports |
| `portIn_shouldOnlyDependOn_domainModel` | Inbound ports → Only domain model |
| `portOut_shouldOnlyDependOn_domainModel` | Outbound ports → Only domain model |
| `adapterIn_mustNotDependOn_adapterOut` | Inbound adapters → No outbound adapter implementations |
| `adapterIn_shouldOnlyDependOn_portInAndDomain` | Inbound adapters → Only allowed dependencies |
| `adapterOut_shouldOnlyDependOn_portOutAndDomain` | Outbound adapters → Only allowed dependencies |
| `domainServices_mustNotHave_springAnnotations` | Domain services → No @Service/@Component |
| `domainModels_mustResideIn_domainModel` | Domain models → Correct package |
| `noCyclicDependencies_betweenPackages` | Package slices → No cycles |

**Implementation**: `src/test/java/.../architecture/HexagonalArchitectureTest.java`

```java
            .should().dependOnClassesThat().resideInAPackage("..port.out..");
}
```

### 20.6 TDD Workflow Example (End-to-End)

Adding a new feature: **Rate limiting per API key**.

**Step 1 — Update spec** (this document):
- Add rate limit to `TenantConfigPort` → already there
- Define `RateLimitExceededException` in domain exceptions
- Add HTTP 429 mapping in web adapter

**Step 2 — Red (write failing tests)**:
```java
// Domain test
@Test
void ask_whenRateLimitExceeded_throwsException() {
    // RateLimitExceededException is not yet created → compile error → RED
}

// Web adapter test
@Test
void ask_rateLimited_returns429() {
    // 429 mapping not configured → fails → RED
}
```

**Step 3 — Green**: Create exception, throw it from domain, map in adapter.

**Step 4 — Refactor**: Extract rate limit check into its own domain service if complex.

---

## 21. Quality & Evaluation

### 21.1 Current Quality

Answer accuracy estimated at 70-90% based on manual testing. No systematic evaluation exists.

### 21.2 Evaluation Framework

**Components**:

1. **Evaluation Dataset**: 100+ question-answer pairs covering each topic, edge cases, off-topic, injection attempts
2. **Evaluation Metrics**: Answer accuracy (LLM-as-judge), retrieval recall@K, rejection precision/recall, latency
3. **CI Integration**: Run evaluation on PR merge to detect regressions

### 21.3 Quality Improvement Roadmap

| Phase | Action | Expected Impact |
|-------|--------|----------------|
| 1 | Build evaluation dataset | Enables measurement |
| 2 | Add reranker to pipeline | +5-10% retrieval precision |
| 3 | Upgrade to larger LLM model | +5-15% answer accuracy |
| 4 | Fine-tune system prompt | +3-5% on problematic categories |
| 5 | Multi-document support | Broader coverage |
| 6 | Hybrid retrieval | Best of both worlds |

---

## Appendix A: RAG Strategy Comparison

### Scoring Matrix (1-5 scale, 5 = best)

| Dimension | A: Pure Semantic | B: LLM Classification | C: Configurable | D: Hybrid |
|-----------|:---:|:---:|:---:|:---:|
| **Portability** | 5 | 3 | 4 | 5 |
| **Accuracy (small models)** | 2 | 4 | 4 | 4 |
| **Accuracy (large models)** | 4 | 5 | 4 | 5 |
| **Per-query cost** | 4 | 2 | 5 | 4 |
| **Latency** | 4 | 2 | 5 | 4 |
| **Implementation effort** | 5 | 3 | 2 | 3 |
| **Maintainability** | 5 | 3 | 2 | 3 |
| **Multi-tenant suitability** | 5 | 3 | 4 | 5 |
| **Graceful degradation** | 3 | 2 | 4 | 4 |
| **Observability** | 4 | 3 | 4 | 4 |
| **Total** | **41** | **30** | **38** | **41** |

### Recommendation

**Approach D (Hybrid)** is recommended. See [04-rag-pipeline.md](04-rag-pipeline.md) section 8.3.

---

## Appendix B: Hexagonal Architecture Decision Record

### Context

The current flat layered architecture (`controller → service → repository`) mixes domain logic with infrastructure concerns. Spring annotations, JPA entities, and HTTP-specific code are intertwined with business rules.

### Decision

Adopt hexagonal architecture (Ports & Adapters) to:

1. **Isolate domain logic** — Business rules are testable without Spring, DB, or HTTP
2. **Enable pluggable infrastructure** — Swap LLM providers, vector stores, and persistence without touching domain code
3. **Enforce boundaries** — ArchUnit tests prevent dependency violations
4. **Simplify TDD** — Domain tests are pure JUnit, fast, and deterministic
5. **Support multi-tenancy** — Port interfaces naturally support tenant-aware operations

### Consequences

- **More files** — Each concept has domain model + JPA entity + MapStruct mapper
- **More explicit wiring** — `BeanConfiguration` must wire all ports to adapters
- **Steeper onboarding** — New developers must understand the port/adapter pattern
- **Better testability** — Domain tests run in milliseconds with zero infrastructure
- **Better changeability** — Infrastructure changes do not affect domain code

### Alternatives Considered

- **Clean Architecture (Uncle Bob)** — Similar but introduces extra layers (Entities, Use Cases, Interface Adapters, Frameworks). Hexagonal is simpler for this project's size.
- **Onion Architecture** — Similar to hexagonal with concentric layers. Hexagonal's port/adapter terminology is more explicit.
- **Keep flat layered** — Rejected because it cannot enforce isolation or support pluggable infrastructure.
