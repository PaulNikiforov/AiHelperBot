# 1. Product Overview

**Version:** 2.0  
**Date:** 2026-04-09  
**Status:** Draft  
**Architecture:** Hexagonal (Ports & Adapters)  
**Methodology:** TDD + Spec-Driven Development  

---

## 1.1 Purpose

AiHelperBot is an internal AI-powered helpdesk that answers employee questions about organizational processes by retrieving relevant information from knowledge base documents and generating answers using a large language model (LLM).

## 1.2 Problem Statement

Employees spend significant time searching through PDF user guides to find answers about Performance Management processes (reviews, growth plans, peer selection, etc.). Manual search is slow, imprecise, and creates a burden on HR/People Partner teams who repeatedly answer the same questions.

## 1.3 Users

| Role | Usage |
|------|-------|
| **Employee** | Asks about PM process steps, deadlines, terminology |
| **Manager** | Asks about team-level PM actions (scheduling reviews, selecting peers, growth plans) |
| **People Partner** | Asks about admin-level PM processes |

## 1.4 Design Principles

1. **Hexagonal Architecture** — Business logic is isolated in the domain core, decoupled from all infrastructure via ports and adapters
2. **Test-Driven Development** — Every feature starts with a failing test. Red → Green → Refactor cycle is mandatory
3. **Spec-Driven Development** — This specification is the source of truth. Port interfaces are defined here before implementation begins
4. **Knowledge-source agnostic** — Minimal code changes when switching to a different document or domain
5. **Pluggable infrastructure** — LLM, embedding, and vector store providers are swappable by writing a new adapter (no domain changes)
6. **Zero Trust** — All client communication is authenticated and validated; no implicit trust
7. **Defense in depth** — Multiple layers of input validation, output sanitization, and threat mitigation
8. **Multi-client ready** — Architecture supports multiple consumer applications simultaneously
9. **Observable** — Every pipeline stage produces metrics, logs, and alerts

## 1.5 Non-Goals (Current Scope)

- Multi-turn conversational memory (future)
- User registration and profile management (future)
- Real-time document editing or collaboration
- General-purpose AI assistant beyond the configured knowledge domain

---

## 2. Development Methodology

### 2.1 Spec-Driven Development

This specification defines all contracts before implementation begins:

1. **Port interfaces** ([03-ports-and-adapters.md](03-ports-and-adapters.md)) are the primary contracts — they are defined in this spec and implemented in code exactly as described
2. **API contracts** ([09-api-specification.md](09-api-specification.md), [api/openapi.yaml](api/openapi.yaml)) define external behavior — tests are written against these contracts first
3. **Data model** ([10-data-model.md](10-data-model.md)) defines persistence contracts — Liquibase migrations implement these exactly
4. **Domain models** ([02-architecture.md](02-architecture.md)) define business entities and value objects — they exist in the domain core with no infrastructure dependencies

**Workflow:**

```
Spec defines contract (port interface, API, data model)
    ↓
Write failing test against the contract
    ↓
Implement minimum code to pass
    ↓
Refactor while tests stay green
    ↓
Update spec if contract changes during implementation
```

### 2.2 Test-Driven Development (TDD)

Every feature follows the Red-Green-Refactor cycle:

#### Red Phase
1. Write a test that describes the desired behavior
2. The test MUST fail (compile error or assertion failure)
3. The test is written against a port interface (not an adapter)

#### Green Phase
1. Write the minimum code to make the test pass
2. No optimization, no refactoring, no extra features
3. Domain logic lives in the domain core; adapters implement port interfaces

#### Refactor Phase
1. Improve code structure while all tests remain green
2. Extract value objects, apply patterns, simplify
3. No new behavior — only structural improvements

#### Test Layers

| Layer | What to test | Tools | Speed |
|-------|-------------|-------|-------|
| **Domain Unit Tests** | Domain services, value objects, entities. Pure logic, no Spring context. | JUnit 5, AssertJ | < 1ms each |
| **Port Contract Tests** | Port interface behavior contracts. Written once, run against every adapter. | JUnit 5, Mockito (for dependencies) | < 10ms each |
| **Adapter Integration Tests** | Each adapter against real infrastructure (DB, HTTP, etc.) | TestContainers, WireMock, MockWebServer | < 5s each |
| **Application Tests** | Full flow through ports + adapters, end-to-end. | @SpringBootTest, TestContainers | < 10s each |
| **Architecture Tests** | Enforce hexagonal dependency rules (domain never imports adapter packages) | ArchUnit | < 1s |

#### TDD Rules

1. **No production code without a failing test** — if there is no test, the feature does not exist
2. **Tests are first-class citizens** — test code quality matters as much as production code
3. **Domain tests have zero infrastructure dependencies** — no Spring, no DB, no HTTP
4. **Port contract tests define the behavior** — adapters must pass all contract tests for their port
5. **Adapter tests use real infrastructure** — TestContainers for DB, WireMock for HTTP, no in-memory fakes in integration tests

### 2.3 Definition of Done

A feature is complete when:

- [ ] Spec section exists and is up to date
- [ ] Port interface defined (if applicable)
- [ ] Domain unit tests pass (Red → Green → Refactor completed)
- [ ] Port contract tests pass
- [ ] Adapter integration tests pass
- [ ] ArchUnit tests pass (dependency rules)
- [ ] Code compiles without warnings
- [ ] No new Checkstyle/PMD violations

---

## 3. Current State

### 3.1 Architecture

Single Spring Boot 3.2.5 / Java 17 service migrating from flat layered to hexagonal architecture:

- **REST API** (4 endpoints: ask, feedback CRUD, intro text)
- **RAG pipeline** (query analysis → document retrieval → prompt building → LLM call)
- **Input validation** (3-stage filter chain: format, language, domain relevance)
- **In-memory vector store** (Spring AI `SimpleVectorStore` with custom page-index extension)
- **Azure Blob Storage** for PDF/vector store persistence
- **PostgreSQL** for feedback storage
- **Ollama** (local) for embeddings (`nomic-embed-text`)
- **OpenRouter** for LLM generation

**Migration status:** Phases 1–5 complete. Domain models, exceptions, port interfaces, domain services, and all 6 outbound adapters exist alongside the original flat-layered packages. Adapters wrap existing infrastructure behind port interfaces. No behavior change yet — existing code is still active. 65 domain unit tests pass, Spring context loads with all adapters wired.

### 3.2 Current Package Structure (In Migration)

```
com.nikiforov.aichatbot/
├── domain/                              # NEW — hexagonal domain core
│   ├── model/                           # Domain models (9 files: value objects, entities, enums)
│   ├── exception/                       # Domain exceptions (3 files)
│   ├── service/                         # Domain services (5 files: QueryClassifier, DocumentRanker,
│   │                                    #   PromptAssembler, FeedbackService, RagOrchestrator)
│   └── validation/                      # Domain validation (3 files: InputValidatorFn, InputValidationChain,
│                                        #   FormatValidator)
├── port/                                # NEW — port interfaces (contracts only)
│   ├── in/                              # Inbound ports (4 use cases)
│   └── out/                             # Outbound ports (7 infrastructure needs)
├── adapter/                             # NEW — outbound adapters (Phase 5)
│   └── out/
│       ├── llm/                         # OpenRouterLlmAdapter (wraps LlmClient)
│       ├── vectorstore/                 # InMemoryVectorStoreAdapter (wraps EmbeddingIndexer)
│       ├── persistence/                 # FeedbackPersistenceAdapter + MapStruct mapper
│       ├── storage/                     # AzureBlobStorageAdapter (wraps AzureBlobStorageService)
│       ├── language/                    # LinguaLanguageAdapter (wraps Lingua LanguageDetector)
│       └── embedding/                   # OllamaEmbeddingAdapter (wraps Spring AI EmbeddingModel)
├── config/                              # Spring configuration
│   └── properties/                      # @ConfigurationProperties beans
├── controller/                          # REST controllers (OLD — to be replaced in Phase 6)
├── dto/
│   ├── request/                         # Request DTOs (OLD)
│   └── response/                        # Response DTOs (OLD)
├── exceptionhandler/                    # @RestControllerAdvice (OLD — to be replaced in Phase 6)
│   └── exception/                       # Custom exceptions (OLD — to be removed in Phase 10)
├── model/                               # JPA entities + enums (OLD — to be moved to adapter/)
├── repository/                          # Spring Data repositories (OLD — to be moved to adapter/)
└── service/
    └── rag/                             # RAG pipeline classes (OLD — to be wrapped in adapters)
        └── validation/                  # Input validation filters (OLD)
```

### 3.3 RAG Pipeline Flow

```
User Question
    |
    v
[QuickInputFilter] -> reject if blank / too short / no letters
    |
    v
[LanguageDetectorFilter] -> reject if non-English (Lingua, confidence > 0.5)
    |
    v
[DomainRelevanceChecker] -> reject if vector similarity < 0.5
    |
    v
[QueryAnalyzer] -> classify into QueryType via regex patterns
    |                (9 types: GENERAL, DEFINITION, TIME_RELATED, SUPPORT,
    |                 INBOX, PEERS, REVIEW_PROCESS, REMIND_PEERS, GROWTH_PLAN)
    v
[DocumentRetriever] -> type-specific strategy (glossary filter, page merge, keyword search)
    |
    v
[DocumentRanker] -> keyword scoring, time-relevance scoring, page merging
    |
    v
[PromptBuilder] -> system prompt (behavioral rules) + user message (question + context)
    |
    v
[LlmClient] -> POST to OpenRouter /chat/completions
    |
    v
[Fallback] -> if DEFINITION answer unsatisfactory, retry with GENERAL strategy
    |
    v
Answer
```

### 3.4 Known Limitations

| Area | Limitation |
|------|-----------|
| **Architecture** | Migration in progress (Phases 1–5 done). Flat-layered code still active; hexagonal domain + ports + outbound adapters coexist. 6 adapters wrap existing infrastructure behind port interfaces. Domain services exist but type-specific retrieval and DEFINITION fallback deferred to Phase 7. |
| **Query classification** | Regex-based, hardcoded to 9 domain-specific types. Not portable. |
| **Document retrieval** | Strategies reference hardcoded page numbers. |
| **Vector store** | In-memory only. Single-tenant. |
| **Embeddings** | Tightly coupled to Ollama. No fallback. |
| **LLM** | Single provider (OpenRouter). No failover. No streaming. |
| **Security** | No authentication. No rate limiting. |
| **Observability** | Log-only. No metrics, tracing, or alerting. |
| **Testing** | Domain unit tests in place (65 tests: 21 model + 7 exception + 31 service/validation + 6 misc). 6 port contract tests (15 abstract tests). Adapter integration tests disabled (need live infra). Port interfaces defined; adapter tests need TestContainers/WireMock. |
| **Multi-client** | No client identification. No API key management. |
