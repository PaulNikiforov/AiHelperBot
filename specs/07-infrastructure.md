# 7. Infrastructure — LLM Chain, Embeddings & Streaming

---

## 12. LLM Provider Chain

### 12.1 Chain Design

```
LlmProviderChain (implements LlmPort)
    |
    +-- [Adapter 1: OpenRouter / Claude]    <-- primary
    |       |
    |       v (on failure)
    +-- [Adapter 2: OpenRouter / GPT-4o]    <-- fallback
    |       |
    |       v (on failure)
    +-- [Adapter 3: Local Ollama / Llama]   <-- last resort
    |       |
    |       v (all failed)
    +-- LlmUnavailableException
```

`LlmProviderChain` is an **adapter** that implements `LlmPort`. It wraps multiple `LlmPort` adapters with failover logic. The domain sees only a single `LlmPort` — it does not know about chains or failover.

### 12.2 Chain Behavior

| Scenario | Behavior |
|----------|----------|
| Primary succeeds | Return result, log metrics |
| Primary fails (timeout, 5xx, rate limit) | Try next provider |
| All providers fail | Throw `LlmUnavailableException` |

### 12.3 Per-Tenant Provider Override

Each tenant can configure its own provider chain or use the global default via `TenantConfigPort`.

---

## 13. Embedding & Vector Store Abstraction

### 13.1 Embedding Provider

Defined by `EmbeddingPort` (see [03-ports-and-adapters.md](03-ports-and-adapters.md)). Adapters:
- `OllamaEmbeddingAdapter` — Local (current)
- `OpenAiEmbeddingAdapter` — OpenAI text-embedding-3-small
- `CohereEmbeddingAdapter` — Cohere embed-v3

**Constraint**: Changing embedding model requires full reindex.

### 13.2 Vector Store

Defined by `VectorSearchPort` + `VectorIndexPort` (see [03-ports-and-adapters.md](03-ports-and-adapters.md)). Adapters:
- `InMemoryVectorStoreAdapter` — Dev/small deployments
- `PgVectorStoreAdapter` — Production with existing PG
- `QdrantVectorStoreAdapter` — High-volume deployments

Selection via configuration: `rag.vector-store.type: in-memory | pgvector | qdrant`

---

## 14. Streaming Responses

### 14.1 Protocol

**Server-Sent Events (SSE)** over HTTP:

```
POST /api/v1/ask/stream
Accept: text/event-stream
X-API-Key: ahb_...

Request body: { "question": "How do I schedule a review?" }
```

Response:
```
data: {"token": "To schedule"}
data: {"token": " a review"}
data: {"token": ".", "done": true, "usage": {"promptTokens": 340, "completionTokens": 85}}
```

### 14.2 Implementation in Hexagonal Terms

- `AskQuestionStreamUseCase` (inbound port) returns `Flux<String>`
- `LlmStreamPort` (outbound port) provides streaming from LLM
- Web adapter maps `Flux<String>` → `Flux<ServerSentEvent>`
- Validation and retrieval remain synchronous; only LLM generation is streamed
