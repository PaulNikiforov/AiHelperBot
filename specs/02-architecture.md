# 2. Architecture — Hexagonal Target & Domain Core

---

## 4. Target Architecture: Hexagonal

### 4.1 Hexagonal Architecture Overview

The hexagonal architecture (Ports & Adapters) separates the application into three layers:

```
+-------------------------------------------------------------------+
|                        ADAPTERS (outer ring)                       |
|                                                                    |
|  +------------------+    +------------------+   +--------------+   |
|  | Web Adapter      |    | Persistence      |   | LLM Adapter  |   |
|  | (REST Controllers)|   | Adapter (JPA)    |   | (OpenRouter) |   |
|  +--------+---------+    +--------+---------+   +------+-------+   |
|           |                       |                     |          |
|   --------+--------       --------+--------     --------+--------  |
|   | Inbound Port  |       | Outbound Port |     | Outbound Port|  |
|   | (Use Cases)   |       | (Repositories)|     | (LLM Client) |  |
|   +-------+-------+       +-------+-------+     +------+-------+  |
|           |                       |                     |          |
|  +--------+-------------------------------------------+---------+  |
|  |                    DOMAIN CORE (inner)                        | |
|  |                                                               | |
|  |  [Domain Services]  [Entities]  [Value Objects]  [Domain      | |
|  |                                                   Events]     | |
|  +---------------------------------------------------------------+ |
+-------------------------------------------------------------------+
```

**Key Rule**: Dependencies point INWARD. The domain core has ZERO dependencies on adapters or frameworks. Adapters depend on ports; ports are part of the domain.

### 4.2 Target Package Structure

```
com.nikiforov.aichatbot/
│
├── domain/                              # DOMAIN CORE — zero framework dependencies
│   ├── model/                           # Entities and Value Objects
│   │   ├── Question.java                # Value object: validated user question
│   │   ├── Answer.java                  # Value object: LLM-generated answer
│   │   ├── QueryType.java               # Enum: GENERAL, DEFINITION, etc.
│   │   ├── DocumentChunk.java           # Value object: a chunk of knowledge base
│   │   ├── RetrievalResult.java         # Value object: ranked list of chunks
│   │   ├── FeedbackId.java              # Value object: type-safe ID
│   │   ├── Feedback.java                # Entity: user feedback
│   │   ├── FeedbackType.java            # Enum: LIKE, DISLIKE
│   │   ├── TenantId.java                # Value object: type-safe tenant ID
│   │   ├── Tenant.java                  # Entity: tenant configuration
│   │   ├── ApiKeyHash.java              # Value object: hashed API key
│   │   ├── ValidationResult.java        # Value object: pass/fail + reason
│   │   └── LlmResponse.java            # Value object: answer + token usage
│   │
│   ├── service/                         # Domain services — pure business logic
│   │   ├── RagOrchestrator.java         # Orchestrates the RAG pipeline
│   │   ├── QueryClassifier.java         # Classifies query type
│   │   ├── DocumentRanker.java          # Ranks/reranks retrieved documents
│   │   ├── PromptAssembler.java         # Builds system + user prompt
│   │   └── FeedbackService.java         # Feedback business logic
│   │
│   ├── validation/                      # Domain validation rules
│   │   ├── InputValidatorFn.java        # @FunctionalInterface for validator functions
│   │   ├── InputValidationChain.java    # Ordered chain of validators (short-circuit on fail)
│   │   ├── FormatValidator.java         # Blank/short/no-letters check
│   │   └── DomainTerms.java             # Glossary terms set
│   │
│   └── exception/                       # Domain exceptions
│       ├── DomainException.java         # Base domain exception
│       ├── FeedbackNotFoundException.java
│       └── LlmUnavailableException.java
│
├── port/                                # PORT INTERFACES
│   ├── in/                              # Inbound ports (use cases) — driven by adapters
│   │   ├── AskQuestionUseCase.java      # Ask a question, get answer
│   │   ├── AskQuestionStreamUseCase.java# Ask with SSE streaming
│   │   ├── SaveFeedbackUseCase.java     # Save feedback
│   │   ├── GetFeedbackUseCase.java      # Retrieve feedback by ID
│   │   └── GetBotIntroUseCase.java      # Get intro text
│   │
│   └── out/                             # Outbound ports — driven by domain
│       ├── LlmPort.java                 # Send prompt, get answer
│       ├── LlmStreamPort.java           # Send prompt, get token stream
│       ├── EmbeddingPort.java           # Embed text into vectors
│       ├── VectorSearchPort.java        # Search vector store
│       ├── VectorIndexPort.java         # Index documents into vector store
│       ├── FeedbackPersistencePort.java # Save/load feedback
│       ├── DocumentStoragePort.java     # Read/write documents from blob
│       ├── TenantConfigPort.java        # Load tenant configuration
│       ├── LanguageDetectionPort.java   # Detect input language
│       └── QuestionLogPort.java         # Log question analytics
│
├── adapter/                             # ADAPTERS — infrastructure implementations
│   ├── in/                              # Inbound adapters (driving)
│   │   └── web/                         # REST API
│   │       ├── BotQueryController.java
│   │       ├── BotFeedbackController.java
│   │       ├── BotIntroController.java
│   │       ├── GlobalExceptionHandler.java
│   │       ├── dto/
│   │       │   ├── AskRequest.java
│   │       │   ├── AskResponse.java
│   │       │   ├── FeedbackRequest.java
│   │       │   ├── FeedbackResponse.java
│   │       │   └── IntroResponse.java
│   │       └── mapper/
│   │           ├── FeedbackWebMapper.java     # MapStruct: DTO ↔ domain
│   │           └── QuestionWebMapper.java
│   │
│   └── out/                             # Outbound adapters (driven)
│       ├── persistence/                 # Database adapter
│       │   ├── entity/
│       │   │   ├── FeedbackJpaEntity.java
│       │   │   └── QuestionLogJpaEntity.java
│       │   ├── repository/
│       │   │   ├── FeedbackJpaRepository.java
│       │   │   └── QuestionLogJpaRepository.java
│       │   ├── mapper/
│       │   │   └── FeedbackPersistenceMapper.java  # MapStruct: entity ↔ domain
│       │   ├── FeedbackPersistenceAdapter.java     # Implements FeedbackPersistencePort
│       │   └── QuestionLogAdapter.java             # Implements QuestionLogPort
│       │
│       ├── llm/                         # LLM adapter
│       │   ├── OpenRouterLlmAdapter.java           # Implements LlmPort + LlmStreamPort
│       │   ├── OllamaLlmAdapter.java               # Local fallback
│       │   ├── LlmProviderChain.java               # Chain of LlmPort with failover
│       │   └── dto/
│       │       ├── OpenRouterRequest.java
│       │       └── OpenRouterResponse.java
│       │
│       ├── embedding/                   # Embedding adapter
│       │   ├── OllamaEmbeddingAdapter.java         # Implements EmbeddingPort
│       │   ├── OpenAiEmbeddingAdapter.java
│       │   └── CohereEmbeddingAdapter.java
│       │
│       ├── vectorstore/                 # Vector store adapter
│       │   ├── InMemoryVectorStoreAdapter.java     # Implements VectorSearchPort + VectorIndexPort
│       │   ├── PgVectorStoreAdapter.java
│       │   └── QdrantVectorStoreAdapter.java
│       │
│       ├── storage/                     # Blob storage adapter
│       │   └── AzureBlobStorageAdapter.java        # Implements DocumentStoragePort
│       │
│       └── language/                    # Language detection adapter
│           └── LinguaLanguageAdapter.java          # Implements LanguageDetectionPort
│
└── config/                              # Spring Boot configuration
    ├── BeanConfiguration.java           # Wire ports to adapters
    ├── SecurityConfiguration.java
    ├── WebConfiguration.java
    └── properties/
        ├── RagProperties.java
        └── BotProperties.java
```

### 4.3 Dependency Rules (enforced by ArchUnit)

| Source package | CAN depend on | MUST NOT depend on |
|---------------|--------------|-------------------|
| `domain.*` | Only `java.*`, `jakarta.validation.*` | `adapter.*`, `port.*`, `config.*`, `org.springframework.*`, `javax.persistence.*` |
| `port.in.*` | `domain.model.*` | `adapter.*`, `port.out.*` |
| `port.out.*` | `domain.model.*` | `adapter.*`, `port.in.*` |
| `adapter.in.*` | `port.in.*`, `domain.model.*` | `adapter.out.*`, `port.out.*` |
| `adapter.out.*` | `port.out.*`, `domain.model.*` | `adapter.in.*`, `port.in.*` |
| `config.*` | Everything | — |

### 4.4 Key Architectural Changes from Current State

| Current | Target |
|---------|--------|
| Flat layered (`controller/service/repository`) | Hexagonal (`domain/port/adapter`) |
| Domain logic mixed with Spring annotations | Pure domain core, zero framework dependencies |
| JPA entities used across all layers | Separate domain models and JPA entities with MapStruct mapping |
| Services directly call repositories | Domain services call outbound ports; adapters implement ports |
| Controllers directly call services | Controllers call inbound port (use case) interfaces |
| No architecture enforcement | ArchUnit tests enforce dependency rules |
| Tests coupled to Spring context | Domain tests are pure JUnit, no Spring |

### 4.5 Migration Path

1. **Phase 1 — Domain Models**: Create value objects, entities, enums in `domain/model/`. Done.
2. **Phase 2 — Domain Exceptions**: Create `DomainException`, `FeedbackNotFoundException`, `LlmUnavailableException`. Done.
3. **Phase 3 — Port Interfaces**: Define all 11 port interfaces (4 inbound, 7 outbound). Done.
4. **Phase 4 — Domain Services**: Extract business logic into `domain/service/` and `domain/validation/`. Done.
5. **Phase 5 — Outbound Adapters**: Wrap existing infrastructure into adapters. Done.
6. **Phase 6 — Inbound Adapters**: Create REST controllers calling inbound ports. Upcoming.
7. **Phase 7 — Wiring**: `BeanConfiguration` wires ports to adapters. Upcoming.
8. **Phase 8 — Integration Tests**: Port and verify integration tests. Upcoming.
9. **Phase 9 — Activate ArchUnit**: Enable architecture enforcement tests. Upcoming.
10. **Phase 10 — Delete Old Code**: Remove flat-layered packages. Upcoming.
11. **Phase 11 — Final Validation**: Full test suite, application startup. Upcoming.

---

## 5. Domain Core

The domain core contains **all business logic** and has **zero infrastructure dependencies**.

### 5.1 Domain Models (Value Objects & Entities)

#### Question (Value Object)

```java
public record Question(String text) {
    public Question {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Question text must not be blank");
        }
    }
}
```

#### Answer (Value Object)

```java
public record Answer(String text, int promptTokens, int completionTokens) {
    public static Answer defaultResponse() {
        return new Answer("Not specified in the User Guide", 0, 0);
    }

    public static Answer unavailable() {
        return new Answer("AI Helper Bot temporarily unavailable. Check application logs for details.", 0, 0);
    }
}
```

#### DocumentChunk (Value Object)

```java
public record DocumentChunk(
    String id,
    String content,
    int pageNumber,
    String source,
    Map<String, Object> metadata
) {}
```

#### Feedback (Entity)

```java
public class Feedback {
    private static final int MAX_ANSWER_LENGTH = 10_000;

    private final FeedbackId id;
    private final String question;
    private final String answer;         // truncated to 10_000 chars in constructor
    private final FeedbackType type;
    private final String employeeEmail;  // nullable
    private final Instant createdAt;

    public Feedback(FeedbackId id, String question, String answer,
                    FeedbackType type, String employeeEmail, Instant createdAt) {
        this.id = id;
        this.question = question;
        this.answer = truncate(answer);
        this.type = type;
        this.employeeEmail = employeeEmail;
        this.createdAt = createdAt;
    }

    // getters ...

    private static String truncate(String value) {
        if (value != null && value.length() > MAX_ANSWER_LENGTH) {
            return value.substring(0, MAX_ANSWER_LENGTH);
        }
        return value;
    }
}
```

#### ValidationResult (Value Object)

```java
public record ValidationResult(boolean passed, String reason) {
    public ValidationResult {
        if (passed && reason != null) {
            throw new IllegalArgumentException("A passing result must not carry a reason");
        }
        if (!passed && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("A failing result must have a non-blank reason");
        }
    }

    public static ValidationResult pass() { return new ValidationResult(true, null); }
    public static ValidationResult fail(String reason) { return new ValidationResult(false, reason); }
}
```

#### FeedbackId (Value Object)

```java
public record FeedbackId(Long value) {}
```

#### LlmResponse (Value Object)

```java
public record LlmResponse(String answerText, int promptTokens, int completionTokens, String providerId) {}
```

### 5.2 Domain Services

#### RagOrchestrator

Central domain service that orchestrates the full RAG pipeline. Implements `AskQuestionUseCase`.

**Constructor dependencies:** `InputValidationChain`, `QueryClassifier`, `VectorSearchPort`, `DocumentRanker`, `PromptAssembler`, `LlmPort`

**Current flow:**
1. Check if vector store is loaded → return `Answer.unavailable()` if not
2. Validate input via `InputValidationChain` → return rejection message if failed
3. Classify query type via `QueryClassifier` (computed but not yet used for type-specific retrieval)
4. Search documents via `VectorSearchPort.search()`
5. Rank results via `DocumentRanker.rankByKeywords()`
6. Build prompt via `PromptAssembler.build()`
7. Send to LLM via `LlmPort.ask()`
8. Return answer with token counts

**Deferred to Phase 5/7:**
- Type-specific retrieval strategies (requires `VectorSearchPort.findByPageNumber()` + config-based page offsets)
- DEFINITION fallback retry (retry with GENERAL strategy if answer contains "not explicitly defined")

**Does NOT:**
- Know about HTTP, REST, JSON
- Know about JPA, SQL, Blob Storage
- Know about OpenRouter, Ollama, or any specific provider

#### QueryClassifier

Pure regex-based query classification. No dependencies.

- Maps input text to one of 9 `QueryType` values
- Priority order: `SUPPORT > REVIEW_PROCESS > PEERS > REMIND_PEERS > INBOX > GROWTH_PLAN > TIME_RELATED > DEFINITION > GENERAL`
- `extractTerm()` extracts the focus term from DEFINITION/TIME_RELATED queries
- `classify()` validates that definition terms have ≤ 3 words (otherwise falls back to GENERAL)

#### DocumentRanker

Scores and ranks document chunks. No dependencies.

- `rankByKeywords()` — word-count overlap scoring between document content and query
- `rankByTimeRelevance()` — keyword score + time-pattern bonus (dates, deadlines, schedules)
- `mergeWithPage()` — prepends page-specific docs, deduplicates, respects max limit

#### PromptAssembler

Builds system + user prompt from context documents. No dependencies.

- Returns a `ChatMessages` record with `systemMessage` and `userMessage`
- System prompt contains behavioral rules for the LLM
- User message includes page-labeled context excerpts

#### FeedbackService

Implements `SaveFeedbackUseCase` and `GetFeedbackUseCase`.

**Constructor dependencies:** `FeedbackPersistencePort`

- `save()` delegates to persistence (ID is assigned by the adapter, domain passes `null`)
- `getById()` throws `FeedbackNotFoundException` if not found
- Answer truncation to 10,000 chars happens in the `Feedback` entity constructor

### 5.3 Domain Validation

#### InputValidatorFn

`@FunctionalInterface` that takes a `String` input and returns `ValidationResult`.

#### InputValidationChain

Takes a `List<InputValidatorFn>` and validates sequentially, short-circuiting on the first failure.

#### FormatValidator

Implements `InputValidatorFn`. Checks:
- Null or blank input → fail
- Input shorter than `minLength` → fail
- Input with no Unicode letters → fail

### 5.3 Domain Exceptions

Domain exceptions do not carry HTTP status codes. Status code mapping is the adapter's responsibility.

```java
public class DomainException extends RuntimeException {
    public DomainException(String message) { ... }
    public DomainException(String message, Throwable cause) { ... }
}

public class FeedbackNotFoundException extends DomainException {
    public FeedbackNotFoundException(FeedbackId id) { ... }
}

public class LlmUnavailableException extends DomainException {
    public LlmUnavailableException(String message) { ... }
}
```
