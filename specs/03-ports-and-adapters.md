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

### 7.2 Outbound Adapters

#### Persistence Adapter (JPA)

- JPA entities (`FeedbackJpaEntity`) are **separate** from domain models (`Feedback`)
- MapStruct mappers translate between domain and JPA
- `FeedbackPersistenceAdapter` implements `FeedbackPersistencePort`
- JPA annotations exist ONLY in the adapter layer

#### LLM Adapter

- `OpenRouterLlmAdapter` implements `LlmPort` and `LlmStreamPort`
- `LlmProviderChain` wraps multiple `LlmPort` instances with failover logic
- Each adapter handles its own HTTP communication, serialization, error handling

#### Embedding Adapter

- `OllamaEmbeddingAdapter` implements `EmbeddingPort`
- Adapter handles Ollama HTTP API specifics
- Switching providers = adding a new adapter + configuration change

#### Vector Store Adapter

- `InMemoryVectorStoreAdapter` implements `VectorSearchPort` + `VectorIndexPort`
- Keeps the current `CustomSimpleVectorStore` internally
- `PgVectorStoreAdapter` will use pgvector SQL operations

#### Blob Storage Adapter

- `AzureBlobStorageAdapter` implements `DocumentStoragePort`
- Handles Azure SDK specifics, SAS token management
- Can be replaced with S3 adapter, local filesystem adapter, etc.
