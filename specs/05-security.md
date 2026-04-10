# 5. Security Architecture & Input Validation

---

## 9. Security Architecture

### 9.1 Threat Model

| Threat | Attack Vector | Impact | Mitigation |
|--------|--------------|--------|-----------|
| **Unauthorized API access** | Direct API calls without auth | Data leak, cost abuse | API Key auth, Zero Trust |
| **Prompt injection** | Malicious instructions in question | LLM hijack, data exfiltration | Input sanitization, system prompt hardening |
| **Off-topic masquerading** | Questions bypassing domain check | Wasted tokens, wrong answers | Improved relevance checking, LLM guardrails |
| **Data exfiltration** | Extracting system prompt or config | IP leak | Output filtering |
| **Token exhaustion / DoS** | Flood of requests | Cost spike | Rate limiting, per-client quotas |

### 9.2 Zero Trust Client Model

```
Client Request
    |
    v
[TLS Termination]          <-- HTTPS required
    |
    v
[API Key Validation]       <-- header: X-API-Key; lookup via TenantConfigPort
    |
    v
[Rate Limiter]             <-- per-API-key sliding window
    |
    v
[Request Validation]       <-- schema validation, max payload size
    |
    v
[Input Sanitization]       <-- strip control chars, injection patterns
    |
    v
[Use Case (domain)]
    |
    v
[Output Sanitization]      <-- strip leaked system prompt fragments
    |
    v
Response
```

### 9.3 API Key Management

| Property | Value |
|----------|-------|
| **Format** | UUID v4, prefixed: `ahb_` + 32 hex chars |
| **Storage** | SHA-256 hash in database; plaintext shown only once at creation |
| **Header** | `X-API-Key: ahb_...` |
| **Scoping** | Each key maps to a tenant with rate limits and allowed endpoints |

### 9.4 Secret Management

| Secret | Current | Target |
|--------|---------|--------|
| `DB_PASSWORD` | Env var | Vault |
| `OPEN_ROUTER_API_KEY` | Env var | Vault with rotation |
| `AZURE_BLOB_URL` (SAS) | Env var | Vault, short-lived tokens |
| API keys (client) | N/A | Hashed in DB |

---

## 10. Input Validation & Adversarial Defense

### 10.1 Current Validation Pipeline

| Order | Filter | Cost | Purpose |
|-------|--------|------|---------|
| 1 | QuickInputFilter | O(1) | Reject blank, too short, no letters |
| 2 | LanguageDetectorFilter | O(n) | Reject non-English |
| 3 | DomainRelevanceChecker | O(embedding) | Reject off-topic via vector similarity |

### 10.2 Extended Validation Pipeline (Target)

| Order | Filter | In Hexagonal | Purpose |
|-------|--------|-------------|---------|
| 1 | **FormatValidator** | Domain | Blank/short/no-letters check |
| 2 | **RateLimitFilter** | Adapter (web) | Per-client rate limiting |
| 3 | **PayloadSanitizer** | Adapter (web) | Strip control chars, normalize Unicode |
| 4 | **LanguageValidator** | Domain (via `LanguageDetectionPort`) | Reject non-accepted languages |
| 5 | **PromptInjectionDetector** | Domain | Pattern-based injection detection |
| 6 | **DomainRelevanceValidator** | Domain (via `VectorSearchPort`) | Vector similarity check |
| 7 | **SemanticGuardrail** (optional) | Domain (via `LlmPort`) | LLM-based domain classification |

**Key distinction:** Filters 2-3 are adapter concerns (HTTP-level). Filters 1, 4-7 are domain validation.

### 10.3 Prompt Injection Mitigation

**Layered defense:**

1. **Input layer**: Pattern-based detection of known injection techniques
2. **Prompt layer**: Clear delimiter between system and user content
3. **Output layer**: Post-generation check for system prompt leaks
4. **Monitoring layer**: Log and alert on detected injection attempts
