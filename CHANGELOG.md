# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **JWT Authentication with Keycloak** (Phase C):
  - `KeycloakJwtConverter` for extracting roles from JWT realm_access claims
  - `SecurityConfig` with OAuth2 Resource Server configuration
  - Role-based authorization via `@EnableMethodSecurity`
  - JWK Set caching for performance
  - Production profile restrictions on Swagger UI (admin-only)
  - Keycloak service in docker-compose.yml with realm configuration
  - 24 security tests covering JWT conversion, role extraction, and identity provider
- Keycloak realm export with test users (`testuser`, `testadmin`)
- Test security configuration for integration tests
- Enhanced JavaDoc on `IdentityProviderPort` for null handling documentation
- Logging for malformed JWTs (missing realm_access or roles claims)
- Type validation for role claims with pattern matching

### Changed

- `SecurityConfig` moved from `config/` package to `adapter/in/web/security/` (hexagonal compliance)
- `BotFeedbackControllerAdapter` now requires authentication via `@PreAuthorize("isAuthenticated()")`
- All `/api/v1/**` endpoints now require valid JWT authentication
- `SpringSecurityIdentityAdapter` enhanced with explicit exception for null email extraction
- `application.yml` added JWK cache configuration

### Removed

- **BotIntro endpoint stack** (Phase A cleanup):
  - `GET /api/v1/bot/intro` endpoint
  - `BotIntroControllerAdapter.java`
  - `BotIntroAdapter.java`
  - `IntroResponse.java`
  - `GetBotIntroUseCase.java`
  - `BotProperties.java`
  - `BotIntroControllerAdapterTest.java`
  - `bot.intro-text` configuration from `application.yml` and `application-test.yml`
  - BotIntro references from OpenAPI spec
- `SecurityUtils.java` — unnecessary intermediary class (Phase C)
- Empty `service/rag/` directory
- All references to "SAS" and "Solbeg" from codebase

### Deprecated

- None

### Fixed

- Null safety: Added null check for JWT subject claim in `KeycloakJwtConverter`
- Null safety: Added explicit exception when email extraction returns null
- Type safety: Added instanceof check before casting roles claim to Collection
- Code quality: Moved `@SuppressWarnings("unchecked")` to specific line instead of method-level

### Security

- **JWT Authentication**: All API endpoints now require valid JWT tokens from Keycloak
- **Role-Based Authorization**: Users must have appropriate roles for admin-only endpoints (Swagger UI in production)
- **JWK Caching**: Enabled to reduce latency on token validation
- **Input Validation**: Role names validated against pattern `^[a-zA-Z0-9_-]+$`
- **Null Safety**: JWT subject and email claims have explicit null/blank checks with exceptions
- **Audit Trail**: User email extracted from JWT claims for feedback persistence

## [0.0.1] - 2026-04-11

### Added

- Initial hexagonal architecture migration (Plan 1)
- RAG/LLM chatbot service
- Vector search with in-memory store
- Feedback persistence with PostgreSQL
- OpenRouter LLM integration
- Azure Blob Storage integration (optional)
- Ollama embeddings integration
