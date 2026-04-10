# 10. Data Model

---

## 18. Data Model

### 18.1 Current Tables

#### `bot_feedback`

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | BIGINT | PK, sequence (increment 50) |
| `question` | VARCHAR(4000) | NOT NULL |
| `answer` | VARCHAR(10000) | NOT NULL |
| `bot_feedback_type` | VARCHAR(50) | NOT NULL (LIKE / DISLIKE) |
| `employee_email` | VARCHAR(255) | nullable |
| `created_at` | TIMESTAMP | NOT NULL, default CURRENT_TIMESTAMP |

### 18.2 New Tables (Target)

#### `tenant`

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK |
| `name` | VARCHAR(100) | NOT NULL, UNIQUE |
| `display_name` | VARCHAR(255) | NOT NULL |
| `config_json` | JSONB | NOT NULL |
| `is_active` | BOOLEAN | NOT NULL, default TRUE |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |

#### `api_key`

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK |
| `tenant_id` | UUID | FK → tenant.id, NOT NULL |
| `name` | VARCHAR(100) | NOT NULL |
| `key_hash` | VARCHAR(64) | NOT NULL (SHA-256) |
| `key_prefix` | VARCHAR(12) | NOT NULL |
| `rate_limit_rpm` | INT | NOT NULL, default 100 |
| `is_active` | BOOLEAN | NOT NULL, default TRUE |
| `created_at` | TIMESTAMP | NOT NULL |
| `last_used_at` | TIMESTAMP | nullable |

#### `document_source`

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK |
| `tenant_id` | UUID | FK → tenant.id, NOT NULL |
| `name` | VARCHAR(255) | NOT NULL |
| `blob_path` | VARCHAR(500) | NOT NULL |
| `content_hash` | VARCHAR(64) | NOT NULL (SHA-256) |
| `chunk_size` | INT | NOT NULL |
| `overlap` | INT | NOT NULL |
| `chunk_count` | INT | NOT NULL |
| `status` | VARCHAR(20) | NOT NULL (PENDING / INDEXING / READY / ERROR) |
| `indexed_at` | TIMESTAMP | nullable |
| `created_at` | TIMESTAMP | NOT NULL |

#### `question_log`

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | BIGINT | PK, sequence |
| `tenant_id` | UUID | FK → tenant.id, NOT NULL |
| `question` | TEXT | NOT NULL |
| `answer` | TEXT | nullable |
| `query_type` | VARCHAR(50) | nullable |
| `llm_provider` | VARCHAR(50) | nullable |
| `validation_result` | VARCHAR(20) | NOT NULL (PASS / REJECTED) |
| `rejection_reason` | VARCHAR(255) | nullable |
| `latency_ms` | INT | NOT NULL |
| `tokens_prompt` | INT | nullable |
| `tokens_completion` | INT | nullable |
| `created_at` | TIMESTAMP | NOT NULL |
