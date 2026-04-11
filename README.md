# AiHelperBot

Spring Boot 3.2.5 / Java 17 RAG (Retrieval-Augmented Generation) service for a Performance Management chatbot.

## Prerequisites

- Java 17+
- PostgreSQL (local installation or Docker)
- Docker & Docker Compose (for containerized development)
- Ollama running locally (`http://localhost:11434`) with `nomic-embed-text` model
- Azure Blob Storage account (optional — degraded mode if absent)

## Environment Variables

Copy `.env.example` to `.env` and fill in the values:

| Variable | Required | Description |
|----------|----------|-------------|
| `DB_PASSWORD` | Yes | PostgreSQL password for `bot_user` |
| `DB_USERNAME` | No | PostgreSQL user (default: `bot_user`) |
| `DB_HOST` | No | Database host (default: `localhost`, use `db` for Docker) |
| `DB_PORT` | No | Database port (default: `5432`) |
| `OPEN_ROUTER_API_KEY` | Yes | API key for the LLM provider (OpenRouter) |
| `LLM_BASE_URL` | No | LLM API base URL (default: `https://openrouter.ai/api/v1`) |
| `LLM_MODEL` | Yes | Model identifier (e.g. `openai/gpt-4o`) |
| `LLM_HTTP_REFERER` | No | HTTP Referer header sent with LLM requests |
| `AZURE_BLOB_URL` | No | Azure Blob Storage connection string (degraded mode if absent) |
| `GUIDE_URL` | No | URL to the PDF user guide passed in LLM payload |

## Commands

### Local Development

```cmd
# Start PostgreSQL (Docker required)
docker compose up -d

# Build
mvnw.cmd clean package

# Run (connects to containerized PostgreSQL)
mvnw.cmd spring-boot:run

# Run all tests
mvnw.cmd test

# Run a single test class
mvnw.cmd test -Dtest=ClassName

# Stop PostgreSQL
docker compose down
```

### Docker Deployment

```cmd
# Build and start all services (app + database)
docker compose -f docker-compose.yml -f docker-compose.prod.yml up --build

# View logs
docker compose logs -f app

# Stop all services
docker compose -f docker-compose.yml -f docker-compose.prod.yml down
```

## REST API

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/ask` | Ask a question; returns LLM answer |
| POST | `/api/v1/botfeedback` | Save thumbs-up/down feedback |
| GET | `/api/v1/botfeedback/{id}` | Retrieve feedback by ID |
