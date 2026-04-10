# Testing Reference

## Test Pyramid

| Layer | What | Tools | Speed | Share |
|-------|------|-------|-------|-------|
| Domain Unit Tests | Domain services, value objects, entities. Pure logic, no Spring. | JUnit 5, AssertJ | < 1ms | ~40% |
| Port Contract Tests | Port interface behavior. Written once, run against every adapter. | JUnit 5, test doubles | < 10ms | ~20% |
| Adapter Integration Tests | Each adapter against real infrastructure (DB, HTTP). | TestContainers, WireMock | < 5s | ~25% |
| Architecture Tests | Enforce hexagonal dependency rules. | ArchUnit | < 1s | ~10% |
| Application Tests | Full flow through ports + adapters, end-to-end. | @SpringBootTest, TestContainers | < 10s | ~5% |

## Test Placement

| Test type | Location | Framework |
|-----------|----------|-----------|
| Domain unit tests | `src/test/java/.../domain/` | JUnit 5, AssertJ (NO Spring) |
| Port contract tests | `src/test/java/.../port/` | JUnit 5 abstract classes |
| Adapter integration tests | `src/test/java/.../adapter/` | TestContainers, WireMock |
| Application tests | `src/test/java/.../` | @SpringBootTest |
| Architecture tests | `src/test/java/.../arch/` | ArchUnit |

## TDD Cycle: RED → GREEN → REFACTOR

### RED — Write Failing Test
1. Write a test that describes the desired behavior
2. Test MUST fail (compile error or assertion failure)
3. Test is written against a port interface (not an adapter)
4. Domain tests use test doubles implementing port interfaces, NOT Mockito mocks

### GREEN — Minimum Code to Pass
1. Write only enough production code to make the test pass
2. No optimization, no refactoring, no extra features
3. Domain logic lives in the domain core; adapters implement port interfaces

### REFACTOR — Improve Structure
1. Improve code structure while all tests remain green
2. Extract value objects, apply patterns, simplify
3. No new behavior — only structural improvements

## Test Rules

1. No production code without a failing test — if there is no test, the feature does not exist
2. Domain tests have ZERO infrastructure dependencies — no Spring, no DB, no HTTP
3. Domain tests use hand-written stubs implementing port interfaces, not Mockito mocks
4. Port contract tests define the behavior — adapters must pass all contract tests for their port
5. Adapter tests use real infrastructure — TestContainers for DB, WireMock for HTTP
6. No `@Ignore` / `@Disabled` tests without explanation

## Test Naming

Format: `whenCondition_thenOutcome`

Good: `ask_whenVectorStoreNotLoaded_returnsUnavailable`
Bad: `test1`, `testSave`
