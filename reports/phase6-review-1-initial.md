# Code Review Report — Phase 6 (Initial)

**Date:** 2026-04-10
**Scope:** Phase 6 Inbound Adapters (`adapter/in/web/**`, `GlobalExceptionHandler`)
**Spec:** `specs/03-ports-and-adapters.md`, `specs/09-api-specification.md`, `specs/api/openapi.yaml`

## Summary

Phase 6 delivers 3 REST controller adapters, 5 DTOs, 2 MapStruct mappers, updated exception handling, and 9 `@WebMvcTest` tests. The code is clean and follows conventions, with one architecture violation and several spec/coverage gaps.

## Findings

| # | Severity | File | Issue | Fix |
|---|----------|------|-------|-----|
| 1 | **Critical** | `BotFeedbackControllerAdapter.java:9` | Imports `service.security.SecurityUtils` — adapter/in depends on old service layer, violates hexagonal rule | Extract `port/out/IdentityProviderPort` interface; create adapter implementation wrapping `SecurityUtils` |
| 2 | **Major** | `AskRequest.java:6` | Missing `@Size(max = 4000)` on `question` — no upper bound, OpenAPI specifies `maxLength: 4000` | Add `@Size(max = 4000)` annotation |
| 3 | **Major** | `GlobalExceptionHandler.java:59-67` | Validation error (400) response missing `errorCode: "VALIDATION_FAILED"` — spec requires it | Add `errorCode` field to validation error body |
| 4 | **Major** | `GlobalExceptionHandler.java:69-75` | Generic error (500) response missing `errorCode: "INTERNAL_ERROR"` — spec requires it | Add `errorCode` field to generic error body |
| 5 | **Major** | `BotFeedbackControllerAdapterTest.java` | Missing validation tests: blank answer, null answer, null botFeedbackType, oversized fields | Add test cases for each validation constraint |
| 6 | **Major** | `FeedbackWebMapper.java:14-15` | Redundant `@Mapping` annotations where source=target names match (`employeeEmail`, `createdAt`) | Remove redundant `@Mapping` lines |
| 7 | **Minor** | `BotFeedbackControllerAdapter.java:28` | Unnecessary `@Validated` class-level annotation — only `@Valid @RequestBody` is used | Remove `@Validated` |
| 8 | **Minor** | `BotQueryControllerAdapter.java:19` etc. | Swagger `@Tag` names differ from OpenAPI spec (`"Question for LLM"` vs `"Question"`, etc.) | Align tag names with OpenAPI |
| 9 | **Minor** | `FeedbackRequest.java:3, FeedbackResponse.java:5` | DTOs directly reference domain enum `FeedbackType` | Acceptable per rules (`adapter.in` can use `domain.model`) — noted, no fix needed |
| 10 | **Minor** | `BotIntroControllerAdapterTest.java` | Only 1 happy-path test, no edge cases | Add null/empty return test |

**Deferred (Phase 7+):** Streaming endpoint (`/api/v1/ask/stream`), API key auth, `QuestionLogPort`, `TenantConfigPort` — these are future spec items, not Phase 6 scope.

## Verdict

- [ ] FAIL — 1 critical + 5 major issues found

## Coverage Assessment
- Domain unit tests: 65 (all pass)
- Adapter tests: 9 (pass, but validation coverage gaps)
- Architecture: `@ConditionalOnProperty` correctly gates adapters
- Build: `./mvnw clean test` — 83 pass, 0 fail
