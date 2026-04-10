# Spring Boot Patterns Reference

## `@Transactional` Placement

- `@Transactional` belongs on **adapter layer** methods (persistence adapter), NOT on domain services
- Domain services are framework-free — they don't know about transactions
- Use `@Transactional(readOnly = true)` for read operations
- Default rollback: unchecked exceptions only. Use `rollbackFor` for checked exceptions
- Self-invocation bypasses `@Transactional` proxy — same limitation as AOP
- NEVER call external APIs inside `@Transactional` — holds DB locks too long

## JPA Entity Rules

- JPA entities exist ONLY in `adapter/out/persistence/entity/`
- Separate from domain models — use MapStruct mapper to translate
- Use `EnumType.STRING` for enums — ORDINAL is fragile on reorder
- Use fetch join or `@EntityGraph` to prevent N+1 queries
- Never return JPA entities from port interfaces or controllers

## `@ConfigurationProperties`

- Use for grouped configuration — NOT `@Value` scattered across classes
- Prefix: `rag`, `bot`
- Always use `${ENV_VAR:default}` for secrets — never hardcode values

## Bean Wiring

- Domain services have NO Spring annotations
- Wire them in `config/BeanConfiguration` with explicit `@Bean` methods
- Constructor injection only — no `@Autowired` field injection
- Each adapter implements exactly one port interface

## Filter vs Interceptor vs AOP

| Mechanism | Level | Access To | Use Case |
|-----------|-------|-----------|----------|
| `Filter` | Servlet | Request/Response only | Auth, CORS, rate limiting |
| `Interceptor` | Spring MVC | Handler method info | Request timing, authorization |
| `AOP` | Spring bean | Method args, return value | Business cross-cutting concerns |

## Error Handling

- Domain exceptions extend `DomainException` — carry NO HTTP status codes
- `GlobalExceptionHandler` (in `adapter/in/web/`) maps domain exceptions → HTTP status + JSON
- Validation errors: `@Valid` on DTO at controller boundary → 400
- Not-found: domain throws `FeedbackNotFoundException` → handler maps to 404
- LLM unavailable: domain throws `LlmUnavailableException` → handler maps to 503
