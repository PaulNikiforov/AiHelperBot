# 3. Ports & Adapters

---

## 6. Ports (Interfaces)

Ports are the contracts between the domain and the outside world. They are defined here and implemented as adapters.

### 6.1 Inbound Ports (Use Cases)

Inbound ports define what the application **can do**. They are called by inbound adapters (controllers).

#### AskQuestionUseCase

```java
public interface AskQuestionUseCase {
    /**
     * Process a user question through the RAG pipeline.
     * @param question validated user question
     * @return answer from the LLM, default response if no docs found,
     *         or unavailable response if vector store not loaded
     * @throws LlmUnavailableException if all LLM providers fail
     */
    Answer ask(Question question);
}
```

#### AskQuestionStreamUseCase

```java
public interface AskQuestionStreamUseCase {
    /**
     * Process a user question and stream the response.
     * @return Flux of answer tokens
     */
    Flux<String> askStream(Question question);
}
```

#### SaveFeedbackUseCase

```java
public interface SaveFeedbackUseCase {
    Feedback save(String question, String answer, FeedbackType type, String employeeEmail);
}
```

#### GetFeedbackUseCase

```java
public interface GetFeedbackUseCase {
    Feedback getById(FeedbackId id);
}
```

#### GetBotIntroUseCase

```java
public interface GetBotIntroUseCase {
    String getIntroText();
}
```

### 6.2 Outbound Ports

Outbound ports define what the domain **needs** from infrastructure. They are implemented by outbound adapters.

#### LlmPort

```java
public interface LlmPort {
    String id();
    LlmResponse ask(String systemMessage, String userMessage);
    boolean isAvailable();
}
```

#### LlmStreamPort

```java
public interface LlmStreamPort {
    Flux<String> askStreaming(String systemMessage, String userMessage);
}
```

#### EmbeddingPort

```java
public interface EmbeddingPort {
    List<float[]> embed(List<String> texts);
    int dimensions();
    boolean isAvailable();
}
```

#### VectorSearchPort

```java
public interface VectorSearchPort {
    List<DocumentChunk> search(String query, int topK);
    List<DocumentChunk> findByPageNumber(int pageNumber);
    boolean isLoaded();
}
```

#### VectorIndexPort

```java
public interface VectorIndexPort {
    void index(List<DocumentChunk> chunks);
    void clear();
}
```

#### FeedbackPersistencePort

```java
public interface FeedbackPersistencePort {
    Feedback save(Feedback feedback);
    Optional<Feedback> findById(FeedbackId id);
}
```

#### DocumentStoragePort

```java
public interface DocumentStoragePort {
    byte[] download(String path);
    void upload(String path, byte[] data);
    boolean exists(String path);
}
```

#### LanguageDetectionPort

```java
public interface LanguageDetectionPort {
    /** Returns detected language code (e.g. "en") or empty if uncertain */
    Optional<String> detect(String text);
}
```

#### QuestionLogPort

```java
public interface QuestionLogPort {
    void log(QuestionLogEntry entry);
}
```

#### TenantConfigPort

```java
public interface TenantConfigPort {
    Optional<Tenant> findByApiKey(String apiKeyHash);
}
```

#### IdentityProviderPort

```java
public interface IdentityProviderPort {
    String getCurrentUserEmail();
}
```

---

## 7. Adapters

### 7.1 Inbound Adapters

#### Web Adapter (REST)

The web adapter translates HTTP requests into use case calls and domain results back into HTTP responses.

**Responsibilities:**
- Receive HTTP request, validate DTO (`@Valid`)
- Map DTO → domain model (via MapStruct)
- Call inbound port (use case)
- Map domain result → response DTO
- Map domain exceptions → HTTP status codes

**Exception mapping (in GlobalExceptionHandler):**

| Domain Exception | HTTP Status | Error Code |
|-----------------|------------|------------|
| `FeedbackNotFoundException` | 404 | `BOT_FEEDBACK_NOT_FOUND` |
| `LlmUnavailableException` | 503 | `LLM_ERROR` |
| `MethodArgumentNotValidException` | 400 | `VALIDATION_FAILED` |
| `Exception` (generic) | 500 | `INTERNAL_ERROR` |

**Key constraint:** Controllers MUST NOT contain business logic. They only translate, delegate, and map.

**Phased activation:** All inbound adapters use `@ConditionalOnProperty(name = "app.adapters.inbound.enabled", havingValue = "true")`. They remain dormant until Phase 7 wires the port implementations as Spring beans. Old controllers continue serving traffic until Phase 10 cutover.

**Implemented inbound adapters (Phase 6):**

| Adapter | Path | Port Dependencies | Endpoint |
|---------|------|-------------------|----------|
| `BotQueryControllerAdapter` | `adapter/in/web/` | `AskQuestionUseCase`, `QuestionWebMapper` | POST `/api/v1/ask` |
| `BotFeedbackControllerAdapter` | `adapter/in/web/` | `SaveFeedbackUseCase`, `GetFeedbackUseCase`, `FeedbackWebMapper`, `IdentityProviderPort` | POST/GET `/api/v1/botfeedback` |
| `BotIntroControllerAdapter` | `adapter/in/web/` | `GetBotIntroUseCase` | GET `/api/v1/bot/intro` |

### 7.2 Outbound Adapters

All 7 outbound adapters are implemented. Each wraps an existing infrastructure service and implements its corresponding port interface. All are wired as `@Component` beans.

#### Persistence Adapter (JPA)

- `FeedbackPersistenceAdapter` implements `FeedbackPersistencePort`
- JPA entities (`BotFeedback`) currently in old `model/` package — will move to `adapter/out/persistence/entity/` in Phase 10
- `FeedbackPersistenceMapper` (MapStruct) translates between domain `Feedback`/`FeedbackId`/`FeedbackType` and JPA `BotFeedback`/`BotFeedbackType`
- Handles `Instant` ↔ `LocalDateTime` conversion for `createdAt`
- Wraps `BotFeedbackRepository` (Spring Data JPA)

#### LLM Adapter

- `OpenRouterLlmAdapter` implements `LlmPort`
- Wraps existing `LlmClient` (delegates `ask()` calls)
- Catches all exceptions → throws `LlmUnavailableException`
- Token usage currently zeroed (deferred — `LlmClient.ask()` returns `String` only)

#### Embedding Adapter

- `OllamaEmbeddingAdapter` implements `EmbeddingPort`
- Wraps Spring AI `EmbeddingModel` bean (Ollama `nomic-embed-text`)
- `dimensions()` returns 768 (hardcoded to match `nomic-embed-text`)

#### Vector Store Adapter

- `InMemoryVectorStoreAdapter` implements both `VectorSearchPort` and `VectorIndexPort`
- Wraps existing `EmbeddingIndexer` + `CustomSimpleVectorStore`
- Maps between domain `DocumentChunk` and Spring AI `Document`:
  - `toChunk()`: extracts `page` and `source` from metadata, uses `doc.getText()` for content
  - `toDocument()`: passes chunk metadata as-is to Spring AI `Document` constructor

#### Blob Storage Adapter

- `AzureBlobStorageAdapter` implements `DocumentStoragePort`
- Wraps existing `AzureBlobStorageService`
- Maps path strings to specific blob operations:
  - `"vectorstore"` → vector store download/upload/exists
  - `"metadata"` → metadata download/upload/exists
  - default → PDF download/exists
- Can be replaced with S3 adapter, local filesystem adapter, etc.

#### Language Detection Adapter

- `LinguaLanguageAdapter` implements `LanguageDetectionPort`
- Wraps Lingua `LanguageDetector` bean (configured in `LinguaConfig`)
- Returns `Optional.empty()` for inputs shorter than 10 characters
- Maps Lingua `Language` enum → ISO 639-1 code string (lowercase)
- Returns `Optional.empty()` for `Language.UNKNOWN`

#### Identity Adapter

- `SpringSecurityIdentityAdapter` implements `IdentityProviderPort`
- Wraps existing `SecurityUtils` (reads email from Spring Security `SecurityContextHolder`)
- Decouples inbound adapters from the old `service.security` package
