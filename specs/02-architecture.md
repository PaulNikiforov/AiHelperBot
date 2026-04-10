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
│   │   ├── InputValidationChain.java    # Ordered chain of validators
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

1. **Phase 1 — Define Ports**: Create all port interfaces (from this spec). Write port contract tests.
2. **Phase 2 — Extract Domain**: Move business logic out of Spring services into domain services. Create domain models separate from JPA entities.
3. **Phase 3 — Implement Adapters**: Wrap existing infrastructure code into adapters that implement outbound ports. Create inbound web adapters.
4. **Phase 4 — Wire Up**: Configure `BeanConfiguration` to wire ports to adapters. Verify all tests pass.
5. **Phase 5 — Clean Up**: Remove old layered packages. Run ArchUnit to verify dependency rules. Delete dead code.

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

Central domain service that orchestrates the full RAG pipeline. Depends only on outbound ports.

**Responsibilities:**
- Validate input via `InputValidationChain`
- Classify query type via `QueryClassifier`
- Search documents via `VectorSearchPort`
- Rank results via `DocumentRanker`
- Build prompt via `PromptAssembler`
- Send to LLM via `LlmPort`
- Handle DEFINITION fallback

**Does NOT:**
- Know about HTTP, REST, JSON
- Know about JPA, SQL, Blob Storage
- Know about OpenRouter, Ollama, or any specific provider

#### FeedbackService

**Responsibilities:**
- Validate feedback data
- Truncate answer to 10,000 chars
- Delegate persistence to `FeedbackPersistencePort`

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
