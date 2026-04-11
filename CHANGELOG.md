# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Docker Compose configuration for local development (`docker-compose.yml`)
- Docker Compose production configuration (`docker-compose.prod.yml`)
- Multi-stage Dockerfile for containerized deployment
- `.dockerignore` to optimize Docker build context
- Environment variables for flexible database connection (`DB_HOST`, `DB_PORT`)
- Keycloak environment variables (for Phase C)

### Changed

- Project description in `pom.xml`: "SAS AI Helper Bot" → "AI Helper Bot"
- Datasource URL now supports `${DB_HOST}` and `${DB_PORT}` environment variables

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
- Empty `service/rag/` directory
- All references to "SAS" and "Solbeg" from codebase

### Deprecated

- None

### Fixed

- None

### Security

- No security changes in Phase A or B
- Note: Application still lacks authentication mechanism (planned for Phase C)

## [0.0.1] - 2026-04-11

### Added

- Initial hexagonal architecture migration (Plan 1)
- RAG/LLM chatbot service
- Vector search with in-memory store
- Feedback persistence with PostgreSQL
- OpenRouter LLM integration
- Azure Blob Storage integration (optional)
- Ollama embeddings integration
