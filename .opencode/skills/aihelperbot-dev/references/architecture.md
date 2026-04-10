# Architecture Reference

## Dependency Rules (ENFORCED BY ArchUnit)

| Package | CAN depend on | MUST NOT depend on |
|---------|--------------|-------------------|
| `domain.*` | `java.*`, `jakarta.validation.*` | `adapter.*`, `org.springframework.*`, `javax.persistence.*` |
| `port.in.*` | `domain.model.*` | `adapter.*`, `port.out.*` |
| `port.out.*` | `domain.model.*` | `adapter.*`, `port.in.*` |
| `adapter.in.*` | `port.in.*`, `domain.model.*` | `adapter.out.*`, `port.out.*` |
| `adapter.out.*` | `port.out.*`, `domain.model.*` | `adapter.in.*`, `port.in.*` |
| `config.*` | Everything | — |

## What Goes Where

| Concern | Location | Example |
|---------|----------|---------|
| Business rules / use cases | `domain/service/` | Answer truncation, fallback strategy, validation chain |
| Use case interface | `port/in/` | `AskQuestionUseCase`, `SaveFeedbackUseCase` |
| Infrastructure interface | `port/out/` | `LlmPort`, `VectorSearchPort`, `FeedbackPersistencePort` |
| Entities / value objects | `domain/model/` | `Question`, `Answer`, `Feedback`, `FeedbackId` |
| Domain exceptions | `domain/exception/` | `LlmUnavailableException` (no HTTP status!) |
| Domain validation | `domain/validation/` | `InputValidationChain`, `FormatValidator` |
| HTTP handling | `adapter/in/web/` | Controllers, DTOs, exception-to-HTTP mapping |
| Web DTO ↔ domain mapping | `adapter/in/web/mapper/` | MapStruct mappers |
| JPA entities + repositories | `adapter/out/persistence/` | `FeedbackJpaEntity`, `FeedbackJpaRepository` |
| LLM client implementations | `adapter/out/llm/` | `OpenRouterLlmAdapter` |
| Embedding implementations | `adapter/out/embedding/` | `OllamaEmbeddingAdapter` |
| Vector store implementations | `adapter/out/vectorstore/` | `InMemoryVectorStoreAdapter` |
| Blob storage implementations | `adapter/out/storage/` | `AzureBlobStorageAdapter` |
| Language detection | `adapter/out/language/` | `LinguaLanguageAdapter` |
| Spring config + wiring | `config/` | `BeanConfiguration` connects ports to adapters |
| HTTP status mapping | `adapter/in/web/` | `GlobalExceptionHandler` maps domain exceptions → HTTP |

## Domain Layer Rules

- Value Objects: Java records. Validate in compact constructor.
- Entities: Classes with private fields. ID is a typed value object (e.g., `FeedbackId`).
- Domain Services: Constructor injection of outbound ports. No `@Service` — wired in `BeanConfiguration`.
- Exceptions: Extend `DomainException`. No HTTP concepts.
- No `public` modifier on interface methods (redundant)
- `Optional` as return type only, never as parameter or field type
- Constructor injection over field injection

## Adapter Layer Rules

- Controllers MUST NOT contain business logic — only validate, map, delegate, map back
- JPA entities are separate from domain models — MapStruct for mapping
- Each adapter implements exactly one port interface
- Exception → HTTP status mapping lives in `GlobalExceptionHandler` only
- DTOs exist only in `adapter/in/web/dto/`
- JPA annotations exist ONLY in `adapter/out/persistence/`

## Spec Index

| File | Content |
|------|---------|
| `specs/01-product-overview.md` | Purpose, methodology, current state |
| `specs/02-architecture.md` | Hexagonal target, domain core |
| `specs/03-ports-and-adapters.md` | All port interfaces + adapter responsibilities |
| `specs/04-rag-pipeline.md` | RAG strategy, hybrid pipeline |
| `specs/05-security.md` | Threat model, zero trust, input validation |
| `specs/06-multi-tenancy.md` | Tenant model, data isolation |
| `specs/07-infrastructure.md` | LLM chain, embeddings, streaming |
| `specs/08-observability.md` | Analytics, health checks, metrics |
| `specs/09-api-specification.md` | REST endpoint contracts |
| `specs/10-data-model.md` | All table schemas |
| `specs/11-configuration.md` | YAML config, env vars |
| `specs/12-testing-and-quality.md` | Test pyramid, ArchUnit, quality |
| `specs/api/openapi.yaml` | OpenAPI 3.1 contract |
| `specs/code-conventions.md` | Naming, dependency rules, code style |
