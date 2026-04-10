# 8. Observability — Analytics, Monitoring & Alerting

---

## 15. Question Analytics

### 15.1 Data Collected

| Field | Source | Purpose |
|-------|--------|---------|
| `tenant_id` | API key lookup | Tenant attribution |
| `question` | Request | Content analysis |
| `query_type` | QueryClassifier | Strategy distribution |
| `documents_retrieved` | DocumentRanker output | Retrieval quality |
| `answer` | LLM response | Quality analysis |
| `llm_provider` | LlmProviderChain | Provider performance |
| `latency_ms` | Timer | Performance monitoring |
| `tokens_prompt` / `tokens_completion` | LlmResponse | Cost tracking |
| `feedback` | User feedback | Quality signal |
| `validation_result` | InputValidationChain | Rejection analysis |

### 15.2 Storage

Analytics events written via `QuestionLogPort` (outbound port). Adapter writes to `question_log` table. For high-volume: offload to Kafka → ClickHouse.

---

## 16. Monitoring & Alerting

### 16.1 Health Checks

| Indicator | Checks | Severity |
|-----------|--------|----------|
| `vectorStore` | Documents loaded (via `VectorSearchPort.isLoaded()`) | CRITICAL |
| `llmProviderChain` | At least one provider available | CRITICAL |
| `embeddingProvider` | Embedding port responds | HIGH |
| `blobStorage` | Blob accessible via `DocumentStoragePort` | MEDIUM |
| `database` | PostgreSQL connection pool healthy | CRITICAL |

### 16.2 Metrics (Micrometer / Prometheus)

| Metric | Type | Tags |
|--------|------|------|
| `rag.query.total` | Counter | `tenant`, `query_type`, `status` |
| `rag.query.latency` | Timer | `tenant`, `phase` |
| `rag.llm.tokens` | Counter | `tenant`, `provider`, `type` |
| `rag.llm.errors` | Counter | `tenant`, `provider`, `error_type` |
| `rag.validation.rejected` | Counter | `tenant`, `filter`, `reason` |
| `rag.feedback.total` | Counter | `tenant`, `type` |

### 16.3 Alerts

| Alert | Condition | Action |
|-------|-----------|--------|
| **All LLM providers down** | Health DOWN >1min | Page on-call |
| **High error rate** | Errors >10% in 5min | Notify team |
| **Vector store empty** | Health DOWN | Notify, check bootstrap |
| **Latency spike** | p95 >10s for 5min | Check LLM provider |
| **Token budget breach** | Daily sum > limit | Notify |
