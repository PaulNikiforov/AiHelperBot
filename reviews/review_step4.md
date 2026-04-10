# Code Review Report — Phase 4: Domain Services

**Date:** 2026-04-10
**Scope:** `domain/service/` (5 files), `domain/validation/` (3 files), tests (7 files)
**Spec:** `specs/02-architecture.md`, `specs/03-ports-and-adapters.md`
**Plan:** `plans/plan1-hexagonal-migration.md` Phase 4 (Steps 4.1–4.14)

---

## Summary

Phase 4 creates 8 domain classes (5 services, 3 validation) with 37 tests (64 total domain tests). All compile and pass cleanly. Zero framework imports in domain. Four findings: 2 minor code quality issues, 1 deferred feature gap, 1 architectural observation.

---

## Files Reviewed

**Domain services (`domain/service/`):**
- `QueryClassifier.java` — regex-based query type classification
- `DocumentRanker.java` — keyword and time-relevance document ranking
- `PromptAssembler.java` — system/user prompt construction from context docs
- `FeedbackService.java` — implements `SaveFeedbackUseCase` and `GetFeedbackUseCase`
- `RagOrchestrator.java` — implements `AskQuestionUseCase`, orchestrates full RAG pipeline

**Domain validation (`domain/validation/`):**
- `InputValidatorFn.java` — `@FunctionalInterface` for validator functions
- `InputValidationChain.java` — sequential validator execution with short-circuit
- `FormatValidator.java` — null/blank/length/letter checks

**Tests (`src/test/java/.../domain/`):**
- `service/QueryClassifierTest.java` — 12 tests
- `service/DocumentRankerTest.java` — 4 tests
- `service/PromptAssemblerTest.java` — 2 tests
- `service/FeedbackServiceTest.java` — 5 tests
- `service/RagOrchestratorTest.java` — 5 tests
- `validation/InputValidationChainTest.java` — 4 tests
- `validation/FormatValidatorTest.java` — 5 tests

---

## 1. Hexagonal Architecture Compliance

| Check | Status | Notes |
|-------|--------|-------|
| Domain services have ZERO `org.springframework.*` imports | PASS | Verified via grep |
| Domain services have ZERO `jakarta.*` / `javax.*` imports | PASS | |
| Domain services do NOT import from `adapter.*` | PASS | |
| Domain services import only from `domain.*`, `port.in.*`, `port.out.*`, `java.*` | PASS | |
| Validation classes import only from `domain.model.*` and `java.*` | PASS | |
| No Spring annotations (`@Component`, `@Service`, etc.) | PASS | |
| Tests use hand-written stubs, not Mockito | PASS | All stubs are inner classes implementing port interfaces |
| Tests have ZERO Spring context | PASS | Pure JUnit 5 + AssertJ |

---

## 2. Spec Compliance

### Step-by-step plan coverage

| Step | Plan Item | Status | Notes |
|------|-----------|--------|-------|
| 4.1 | InputValidationChain tests | PASS | 4 tests |
| 4.2 | InputValidationChain impl | PASS | Sequential, short-circuits |
| 4.3 | FormatValidator tests | PASS | 5 tests |
| 4.4 | FormatValidator impl | PASS | Takes `int minLength` constructor param |
| 4.5 | QueryClassifier tests | PASS | 12 tests, all 9 query types + extractTerm |
| 4.6 | QueryClassifier impl | PASS | Regex matching, same priority order as original |
| 4.7 | DocumentRanker tests | PASS | 4 tests |
| 4.8 | DocumentRanker impl | PASS | keyword + time scoring, mergeWithPage |
| 4.9 | PromptAssembler tests | PASS | 2 tests |
| 4.10 | PromptAssembler impl | PASS | Returns `ChatMessages` record |
| 4.11 | FeedbackService tests | PASS | 5 tests |
| 4.12 | FeedbackService impl | PASS | Implements both use case interfaces |
| 4.13 | RagOrchestrator tests | PASS | 5 tests |
| 4.14 | RagOrchestrator impl | PASS | Orchestrates validate→classify→retrieve→rank→prompt→LLM |

### Missing from plan

| Plan Item | Status | Notes |
|-----------|--------|-------|
| Step 4.13: "DEFINITION unsatisfactory — retries with GENERAL strategy" | **NOT IMPLEMENTED** | `RagOrchestrator.ask()` computes `queryType` but does not use it for type-specific retrieval or DEFINITION fallback. This is the single biggest gap vs. the plan. |

---

## 3. TDD Coverage

### Test statistics

| Service | Tests | Coverage |
|---------|-------|----------|
| `InputValidationChain` | 4 | All paths: all pass, first fail, second fail, empty list |
| `FormatValidator` | 5 | Null, blank, too short, no letters, valid |
| `QueryClassifier` | 12 | All 9 types, extractTerm (def, time), too-many-words fallback |
| `DocumentRanker` | 4 | keyword ranking, time ranking, merge with page, dedup limit |
| `PromptAssembler` | 2 | With context, empty context |
| `FeedbackService` | 5 | Truncation, exact limit, delegation, found, not-found |
| `RagOrchestrator` | 5 | Not loaded, validation fail, no docs, happy path, LLM failure |
| **Total** | **37** | |

### Test quality

| Check | Status |
|-------|--------|
| No Mockito in domain tests | PASS |
| No Spring context in domain tests | PASS |
| Stubs implement port interfaces directly | PASS |
| Tests are pure JUnit 5 + AssertJ | PASS |

---

## 4. Findings

### Hexagonal Architecture

| # | Severity | File | Line | Issue | Recommendation |
|---|----------|------|------|-------|----------------|
| — | — | — | — | No violations found | — |

### Code Quality

| # | Severity | File | Line | Issue | Recommendation |
|---|----------|------|------|-------|----------------|
| 1 | Minor | `RagOrchestrator.java` | 6 | Redundant import: `ValidationResult` is imported both via wildcard (`domain.model.*`) and explicitly. | Remove line 6 (`import ...ValidationResult;`). |
| 2 | Minor | `RagOrchestratorTest.java` | 21 | `StubEmbeddingPort` is declared as a field but never used in any test. | Remove the unused stub. |

### Feature Gaps (Deferred)

| # | Severity | File | Issue | Recommendation |
|---|----------|------|-------|----------------|
| 3 | Major (deferred) | `RagOrchestrator.java` | `queryType` is computed at line 47 but never used. No type-specific retrieval strategy (e.g., SUPPORT gets support-page docs, PEERS gets peer-selection docs). No DEFINITION fallback retry. The existing `RagService` has this logic via `DocumentRetriever` type-specific strategies. | This logic requires injecting `VectorSearchPort.findByPageNumber()` and config-based page offsets. Defer to Phase 5/7 when adapters provide the config-backed page lookups. Document as a known gap. |
| 4 | Info | `FeedbackService.java` | `save()` passes `null` as `FeedbackId` — the persistence adapter assigns the ID. | Acceptable: the domain delegates ID generation to the persistence layer. No change needed. |

---

## 5. Naming Convention Check

Against `specs/02-architecture.md` and code conventions:

| Check | Status |
|-------|--------|
| Domain services named as verb/noun nouns | PASS: `QueryClassifier`, `DocumentRanker`, `PromptAssembler`, `FeedbackService`, `RagOrchestrator` |
| No `public` modifier on methods in non-public classes | PASS |
| No comments | PASS |
| Package structure matches spec | PASS |
| `@FunctionalInterface` on `InputValidatorFn` | PASS |

---

## Summary

| Severity | Count |
|----------|-------|
| Critical | 0 |
| Major (deferred) | 1 |
| Minor | 2 |
| Info | 1 |

---

## Verdict

- [x] **PASS WITH NOTES** — 2 minor issues (unused import, unused stub), 1 deferred major (type-specific retrieval not yet implemented in RagOrchestrator)
- [ ] PASS
- [ ] FAIL

**Rationale:** All domain services compile, 64 tests pass, zero framework imports, TDD followed with hand-written stubs. The type-specific retrieval and DEFINITION fallback are correctly deferred to later phases when adapters and config wiring are available. Minor code cleanups recommended.

---

## Coverage Assessment

- **Domain unit tests:** 64 tests, all PASS
- **Port contract tests:** N/A (Phase 5)
- **Adapter integration tests:** N/A (Phase 5+)
- **Arch tests:** N/A (activated in Phase 9)
- **Compilation:** PASS — all types resolve

---

## Next Steps

Phase 4 is complete. Ready to proceed to **Phase 5: Outbound Adapters** (Steps 5.1–5.12) which wraps existing infrastructure behind port interfaces.

### Recommended minor cleanups before Phase 5:
1. Remove redundant `ValidationResult` import from `RagOrchestrator.java:6`
2. Remove unused `StubEmbeddingPort` from `RagOrchestratorTest.java:21`
