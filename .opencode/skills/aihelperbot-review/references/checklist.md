# Review Checklist Reference

## 1. Hexagonal Architecture Compliance

- Domain purity: `domain/` has ZERO imports from `org.springframework.*`, `javax.persistence.*`, `jakarta.persistence.*`, `adapter.*`
- Port direction: Inbound ports (`port/in/`) do not depend on outbound ports (`port/out/`) and vice versa
- Adapter isolation: `adapter/in/` does not import from `adapter/out/` and vice versa
- Domain models vs infrastructure models: JPA entities are in `adapter/out/persistence/entity/`, NOT in `domain/model/`
- DTOs are adapter-only: Request/response DTOs exist only in `adapter/in/web/dto/`, not in domain or ports
- Port interfaces return domain models: No JPA entities, no DTOs in port signatures
- No `@Service`, `@Component`, `@Repository` on domain classes: Domain beans are wired in `config/BeanConfiguration`
- No HTTP concepts in domain: Domain exceptions carry no HTTP status codes. Status mapping is in `GlobalExceptionHandler`
- MapStruct mappers at boundaries: Domain ↔ JPA and Domain ↔ DTO translation uses MapStruct, not manual mapping in business logic
- Controllers are thin: No business logic — only validate, map, delegate, map back

## 2. TDD Compliance

- Test exists for every change: No production code without a corresponding test
- Domain tests are pure: Tests in `domain/` use JUnit 5 + AssertJ only. No `@SpringBootTest`, no `@MockBean`
- Domain tests use test doubles, not mocks: Stub/fake implementations of port interfaces, not Mockito mocks
- Port contract tests exist: Abstract test classes define expected port behavior
- Adapter tests extend contract tests: Each adapter runs the contract test suite for its port
- Adapter integration tests use real infrastructure: TestContainers for DB, WireMock for HTTP
- ArchUnit tests exist and pass: Hexagonal dependency rules are enforced
- Test names describe behavior: `whenCondition_thenOutcome` format

## 3. Spec Compliance

- Port interfaces match spec: Compare against `specs/03-ports-and-adapters.md`
- API contracts match spec: REST endpoints match `specs/09-api-specification.md` and `specs/api/openapi.yaml`
- Data model matches spec: JPA entities and Liquibase migrations match `specs/10-data-model.md`
- Domain models match spec: Value objects and entities match `specs/02-architecture.md` Section 5
- Spec is updated: If implementation deviates from spec, spec must be updated too

## 4. Code Quality

- SOLID principles followed
- No unnecessary complexity or over-engineering
- No code duplication (DRY)
- Methods have single responsibility
- Java 17+ features used (records, sealed classes, pattern matching, text blocks)
- No magic numbers — named constants
- Proper use of Optional, Stream, functional interfaces

## 5. Spring Boot Patterns

- Constructor injection only (no `@Autowired` on fields)
- `@Transactional` at correct layer (adapter, not domain)
- `@ConfigurationProperties` for config, not scattered `@Value`
- Beans are stateless where possible

## 6. Error Handling

- Domain exceptions are specific and carry context
- Domain exceptions do NOT carry HTTP status codes
- `GlobalExceptionHandler` maps domain exceptions → HTTP status + error JSON
- No swallowed exceptions (empty catch blocks)
- Errors logged at appropriate level

## 7. Security

- No sensitive data in logs
- Input validation at controller boundary (DTO `@Valid`)
- No hardcoded secrets
- All secrets via `${ENV_VAR}` placeholders

## 8. Performance

- No N+1 query risks
- No unnecessary eager fetching
- Collections not iterated multiple times when one pass suffices
- No blocking calls in async/reactive contexts

## 9. Consistency

- Follows project naming conventions in `specs/code-conventions.md`
- Uses existing utilities instead of reinventing
- Consistent error handling style
- Consistent test structure
