# 11. Configuration Reference

---

## 19. Configuration Reference

### 19.1 Application Configuration

```yaml
server:
  port: 8080

security:
  api-key:
    enabled: true
    header-name: X-API-Key

rag:
  vector-store:
    type: in-memory          # in-memory | pgvector | qdrant
  embedding:
    provider: ollama         # ollama | openai | cohere
    ollama:
      base-url: http://localhost:11434
      model: nomic-embed-text
  llm:
    chain:
      - provider: openrouter
        model: ${LLM_MODEL:anthropic/claude-sonnet-4}
        base-url: ${LLM_BASE_URL:https://openrouter.ai/api/v1}
        api-key: ${OPEN_ROUTER_API_KEY}
        timeout-seconds: 30
    temperature: 0.3
    max-tokens: 1024
    max-documents: 5
    unsatisfactory-phrases:
      - "not explicitly defined"
      - "not specified"
  validation:
    quick-filter:
      min-length: 2
    language-detector:
      accepted-languages: ENGLISH
      min-confidence: 0.5
      short-input-threshold: 10
    domain-checker:
      max-search-results: 1
      min-score: 0.5
    prompt-injection:
      enabled: true
    rate-limit:
      enabled: true
      default-rpm: 100

app:
  adapters:
    inbound:
      enabled: true           # false = Phase 6 adapters dormant, old controllers serve traffic

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

### 19.2 Environment Variables

| Variable | Required | Default | Purpose |
|----------|----------|---------|---------|
| `DB_PASSWORD` | Yes | — | PostgreSQL password |
| `OPEN_ROUTER_API_KEY` | Yes | — | Primary LLM API key |
| `AZURE_BLOB_URL` | No | "" | Azure Blob Storage URL |
| `LLM_BASE_URL` | No | openrouter.ai | LLM API base URL |
| `LLM_MODEL` | No | anthropic/claude-sonnet-4 | Primary LLM model |
