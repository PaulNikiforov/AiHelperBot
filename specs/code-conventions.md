# Code Conventions

## Language & Framework

- **Java 17** — records, sealed classes, text blocks, pattern matching where applicable
- **Spring Boot 3.2.5** — dependency injection, auto-configuration
- **Build tool**: Maven (always use `mvnw.cmd` on Windows)

---

## Package Structure

```
com.nikiforov.aichatbot/
├── domain/          # Pure business logic, ZERO framework imports
├── port/
│   ├── in/          # Use case interfaces (depend on domain.model only)
│   └── out/         # Outbound port interfaces (depend on domain.model only)
├── adapter/
│   ├── in/web/      # REST controllers, DTOs, web mappers
│   └── out/         # Infrastructure implementations (JPA, HTTP, blob, etc.)
└── config/          # Spring configuration, properties
```

---

## Dependency Rules

Enforced by ArchUnit. Violations fail the build.

| Package | Allowed imports | Forbidden imports |
|---------|----------------|-------------------|
| `domain.*` | `java.*`, `jakarta.validation.*` | `adapter.*`, `port.*`, `config.*`, `org.springframework.*`, `javax.persistence.*` |
| `port.in.*` | `domain.model.*` | `adapter.*`, `port.out.*` |
| `port.out.*` | `domain.model.*` | `adapter.*`, `port.in.*` |
| `adapter.in.*` | `port.in.*`, `domain.model.*`, Spring | `adapter.out.*`, `port.out.*` |
| `adapter.out.*` | `port.out.*`, `domain.model.*`, Spring | `adapter.in.*`, `port.in.*` |
| `config.*` | Everything | — |

---

## Naming Conventions

### Classes

| Category | Pattern | Example |
|----------|---------|---------|
| Value Object | Noun, record | `Question`, `Answer`, `DocumentChunk` |
| Entity | Noun, class | `Feedback`, `Tenant` |
| Type-safe ID | `{Entity}Id` | `FeedbackId`, `TenantId` |
| Enum | Noun | `QueryType`, `FeedbackType` |
| Domain Service | Noun | `RagOrchestrator`, `DocumentRanker` |
| Domain Exception | `{Name}Exception` | `FeedbackNotFoundException` |
| Inbound Port (Use Case) | `{Verb}{Noun}UseCase` | `AskQuestionUseCase` |
| Outbound Port | `{Noun}Port` | `LlmPort`, `VectorSearchPort` |
| Inbound Adapter (Web) | `{Name}Controller` | `BotQueryController` |
| Outbound Adapter | `{Name}Adapter` | `OpenRouterLlmAdapter` |
| JPA Entity | `{Name}JpaEntity` | `FeedbackJpaEntity` |
| JPA Repository | `{Name}JpaRepository` | `FeedbackJpaRepository` |
| MapStruct Mapper | `{Name}Mapper` | `FeedbackPersistenceMapper` |
| Request DTO | `{Name}Request` | `AskRequest`, `FeedbackRequest` |
| Response DTO | `{Name}Response` | `AskResponse`, `FeedbackResponse` |
| Properties | `{Name}Properties` | `RagProperties`, `BotProperties` |
| Test (domain) | `{Class}Test` | `RagOrchestratorTest` |
| Test (contract) | `{Port}Contract` | `FeedbackPersistencePortContract` |
| Test (adapter) | `{Class}Test` or `{Class}IT` | `FeedbackPersistenceAdapterIT` |
| Test (arch) | `{Name}Test` | `HexagonalArchitectureTest` |

### Methods

| Context | Pattern | Example |
|---------|---------|---------|
| Use case | verb | `ask()`, `save()`, `getById()` |
| Port query | verb or `findBy*` | `search()`, `findById()` |
| Port command | verb | `index()`, `clear()`, `upload()` |
| Boolean return | `is*`, `has*` | `isAvailable()`, `isLoaded()` |
| Factory method | `of()`, `from()`, `create()` | `Answer.defaultResponse()`, `ValidationResult.pass()` |

---

## Code Style

### General

- No `public` modifier on interface methods (redundant)
- No `public` modifier on record fields (redundant)
- Use Java records for immutable value objects
- Use `Optional` as return type only, never as method parameter or field type
- Prefer constructor injection over field injection
- Prefer `final` fields in domain entities

### Domain Layer

- **Zero** Spring annotations (`@Service`, `@Component`, `@Autowired`, etc.)
- **Zero** JPA annotations (`@Entity`, `@Column`, etc.)
- **Zero** HTTP references (`@RestController`, `HttpStatus`, etc.)
- Domain services receive ports via constructor parameters
- Domain exceptions extend `DomainException`, never carry HTTP status codes
- Validate invariants in constructors/compact constructors

### Adapter Layer

- Controllers MUST NOT contain business logic — only translate, delegate, map
- JPA entities are separate from domain models — use MapStruct for mapping
- Each adapter implements exactly one port interface
- Exception → HTTP status mapping lives in `GlobalExceptionHandler` only

### Testing

- Domain tests: pure JUnit 5 + AssertJ, NO Spring context, NO Mockito
- Use test doubles (hand-written stubs implementing port interfaces) for domain tests
- Adapter tests: TestContainers for DB, WireMock for HTTP
- Contract tests: abstract class per port, extended by each adapter test
- ArchUnit tests enforce all dependency rules

---

## Commit & PR Conventions

- TDD cycle must be visible in commit history (Red → Green → Refactor)
- Each PR references the relevant spec section
- PR description includes: what changed, why, spec link, test evidence
