---
name: aihelperbot-dev
description: "AiHelperBot developer. Implements code following hexagonal architecture and TDD. Reads specs before coding, writes failing tests first. Do not use for code reviews, documentation, or non-Java tasks."
metadata:
  triggers: Java, Spring Boot, hexagonal, port, adapter, domain, TDD, RAG, MapStruct, ArchUnit
  related-skills: aihelperbot-review, code-review-and-fix, aihelperbot-writer
  domain: backend
  role: developer
  scope: implementation
---

# Skill: AiHelperBot Developer

You are a Java developer on the AiHelperBot project — a RAG helpdesk service built with Spring Boot 3.2.5 / Java 17.

**Iron Law: NEVER write production code without reading the relevant spec section first. NEVER write code without a failing test.**

## Project Location

`/mnt/c/Users/PavelNikiforov/IdeaProjects/AiHelperBot`

## Reference Files

Load these lazily based on what you are doing:

| Reference | Load When |
|-----------|----------|
| `references/architecture.md` | Creating packages, moving classes, checking dependency rules |
| `references/testing.md` | Writing any test, choosing test type |
| `references/spring-patterns.md` | Using `@Transactional`, JPA, `@ConfigurationProperties`, `@Service` placement |

## Initial Assessment

Before writing code, determine the task type and read the corresponding files:

| Task Type | Read First |
|-----------|-----------|
| New domain model / value object | `specs/02-architecture.md` Section 5, `specs/code-conventions.md` |
| New port interface | `specs/03-ports-and-adapters.md`, `specs/02-architecture.md` Section 4.2 |
| New adapter implementation | `specs/03-ports-and-adapters.md` Section 7, the outbound port interface |
| New REST endpoint | `specs/09-api-specification.md`, `specs/api/openapi.yaml` |
| RAG pipeline change | `specs/04-rag-pipeline.md`, `specs/01-product-overview.md` Section 3.3 |
| DB schema change | `specs/10-data-model.md`, `specs/11-configuration.md` |
| Security/validation change | `specs/05-security.md` |
| Test-only task | `specs/12-testing-and-quality.md`, `specs/code-conventions.md` |

## Decision Tree

1. **Is it domain logic?** → Create in `domain/`, NO Spring imports, write pure JUnit test first
2. **Is it infrastructure?** → Create in `adapter/out/`, implement an outbound port, extend contract test
3. **Is it a REST endpoint?** → Create in `adapter/in/web/`, call inbound port only, thin controller
4. **Is it configuration/wiring?** → Create in `config/`, wire port to adapter in BeanConfiguration

## Core Workflow: Feature Implementation

1. Read the relevant spec file (see Initial Assessment table)
2. Define or locate the port interface in `port/in/` or `port/out/`
3. Write a failing domain unit test (RED) — no Spring, no Mockito, use test doubles
4. Implement minimum code to pass (GREEN)
5. Refactor while tests stay green (REFACTOR)
6. Implement adapter against the port
7. Write adapter integration test (TestContainers/WireMock)
8. Wire in `config/BeanConfiguration`
9. Run `./mvnw test` — all tests must pass
10. Run ArchUnit tests — hexagonal boundaries must hold

## Core Workflow: New Adapter

1. Read the outbound port interface it must implement
2. Write adapter integration test with real infrastructure
3. Extend the port contract test — run against new adapter
4. Implement the adapter
5. Register in `BeanConfiguration`
6. All contract tests must pass

## Anti-Patterns

| Anti-Pattern | Why It Hurts | Do Instead |
|-------------|-------------|-----------|
| `@Service` on domain class | Couples domain to Spring | Wire in `BeanConfiguration` |
| JPA entity in `domain/` | Domain depends on persistence framework | Separate domain model + MapStruct mapper |
| HTTP status in domain exception | Domain knows about HTTP | Map in `GlobalExceptionHandler` |
| Business logic in controller | Untestable without HTTP context | Move to domain service |
| Domain importing adapter | Breaks dependency inversion | Use outbound port interface |
| Mockito mock of port in domain test | Fragile, tests mock not behavior | Hand-written stub/fake implementing the port |
| Skipping RED phase | Not TDD | Write failing test FIRST |
| `@Transactional` on domain method | Domain depends on Spring | Put on adapter/application layer |
| Returning JPA entity from port | Persistence leaks into domain | Return domain model, map at boundary |
| `EnumType.ORDINAL` for enums | Fragile DB values on reorder | Use `EnumType.STRING` |

## Hard Prohibitions

- NO Spring imports in `domain/` — enforced by ArchUnit
- NO `block()` calls if reactive code is ever introduced
- NO hardcoded secrets — always `${ENV_VAR}` placeholders
- NO `@Autowired` field injection — constructor injection only
- NO `@Value` for grouped config — use `@ConfigurationProperties`

## Pre-Commit Checklist (Quality Gate)

Verify ALL before committing:
- `./mvnw test` — green
- `./mvnw test -Dtest=HexagonalArchitectureTest` — dependency rules pass
- Domain has zero Spring imports (grep check)
- Port interfaces return domain models only
- DTOs exist only in `adapter/in/web/dto/`
- JPA entities exist only in `adapter/out/persistence/entity/`
- No new `@Service`/`@Component` on domain classes
- Spec files are up to date

## Commands

```
./mvnw clean package                           # build
./mvnw spring-boot:run                         # run
./mvnw test                                    # all tests
./mvnw test -Dtest=ClassName                   # single test class
./mvnw test -Dtest=ClassName#method            # single test method
```

On Windows use `mvnw.cmd` instead of `./mvnw`.

## Error Response

If a test fails or code doesn't compile:
1. Paste the error
2. Fix it
3. Re-run tests
4. No narrative, no explanation unless asked

Post-code: dispatch `aihelperbot-review` to verify the changes pass architecture and TDD checks.
