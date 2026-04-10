# 9. API Specification

---

## 17. API Specification

### 17.1 Endpoints

#### Ask (Synchronous)

```
POST /api/v1/ask
X-API-Key: ahb_...
Content-Type: application/json

Request:  { "question": "How do I schedule a performance review?" }
Response (200): { "answer": "To schedule a performance review, navigate to..." }
Response (400): validation error
Response (401): missing/invalid API key
Response (429): rate limit exceeded
Response (503): LLM unavailable
```

#### Ask (Streaming)

```
POST /api/v1/ask/stream
X-API-Key: ahb_...
Accept: text/event-stream

Request:  { "question": "..." }
Response (200): SSE stream
```

#### Feedback

```
POST /api/v1/botfeedback
X-API-Key: ahb_...

Request:  { "question": "...", "answer": "...", "botFeedbackType": "LIKE" }
Response (201): { "id": 1, "question": "...", "answer": "...", "botFeedbackType": "LIKE", "employeeEmail": null, "createdAt": "..." }
```

```
GET /api/v1/botfeedback/{id}
X-API-Key: ahb_...

Response (200): FeedbackResponse
Response (404): { "errorCode": "300000", "message": "Bot feedback not found" }
```

#### Bot Intro

```
GET /api/v1/bot/intro
X-API-Key: ahb_...

Response (200): { "introText": "Hello! I'm the AI Helper Bot..." }
```

### 17.2 Common Error Response Format

```json
{
    "errorCode": "LLM_ERROR",
    "message": "AI service temporarily unavailable"
}
```

| HTTP Status | Error Code | Description |
|-------------|-----------|-------------|
| 400 | VALIDATION_FAILED | Request body validation errors |
| 401 | UNAUTHORIZED | Missing or invalid API key |
| 404 | 300000 | Bot feedback not found |
| 429 | RATE_LIMIT_EXCEEDED | Too many requests |
| 503 | LLM_ERROR | All LLM providers unavailable |
| 500 | INTERNAL_ERROR | Unexpected server error |

The canonical OpenAPI 3.1 contract is in [api/openapi.yaml](api/openapi.yaml).
