# 4. RAG Pipeline Design

---

## 8. RAG Pipeline Design

### 8.1 Current Query Classification Problem

The current `QueryAnalyzer` classifies questions into 9 types using regex patterns. Each type triggers a different retrieval strategy with hardcoded page-number references.

**Problem**: Domain-locked. Switching to a new knowledge base requires rewriting regex patterns, query types, and page mappings.

### 8.2 Strategy Comparison

Four approaches evaluated. See [Appendix A](12-testing-and-quality.md#appendix-a-rag-strategy-comparison) for the full scoring matrix.

### 8.3 Recommended Pipeline (Hybrid)

```
User Question
    |
    v
[InputValidationChain]         <-- domain validation (via ports)
    |
    v
[VectorSearchPort.search()]    <-- semantic search, top-K candidates
    |
    v
[DocumentRanker]               <-- domain service: keyword scoring + time-relevance
    |                               Optional: reranker via outbound port
    v
[Strategy Override] (optional) <-- per-tenant config: merge sections, boost pages
    |
    v
[PromptAssembler]              <-- domain service: builds system + user prompt
    |
    v
[LlmPort.ask()]               <-- via LlmProviderChain adapter
    |
    v
Answer
```

**In hexagonal terms:**
- `InputValidationChain`, `DocumentRanker`, `PromptAssembler` are **domain services**
- `VectorSearchPort`, `LlmPort` are **outbound ports**
- The pipeline orchestration lives in `RagOrchestrator` (domain service implementing `AskQuestionUseCase`)

### 8.4 Migration Path

1. **Phase 1**: Define `VectorSearchPort`, `LlmPort`. Write contract tests. Current implementations become adapters.
2. **Phase 2**: Move `QueryClassifier`, `DocumentRanker`, `PromptAssembler` to domain core. Remove Spring dependencies.
3. **Phase 3**: Implement `SemanticRetrievalStrategy`. A/B test against legacy.
4. **Phase 4**: Configurable strategy overrides per tenant.

### 8.5 Metrics Required for Strategy Evaluation

| Metric | How to Measure |
|--------|---------------|
| **Retrieval Recall@K** | Fraction of gold-standard relevant docs in top-K results |
| **Answer Accuracy** | LLM-as-judge (scores answer vs. gold answer on 1-5) |
| **Off-Topic Rejection Rate** | Fraction of known off-topic queries correctly rejected |
| **False Rejection Rate** | Fraction of legitimate queries incorrectly rejected |
| **Latency p50/p95** | End-to-end response time |
| **Token Usage** | Prompt + completion tokens per query |
