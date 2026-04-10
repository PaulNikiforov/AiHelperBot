# 6. Multi-Tenancy & Multi-Document Support

---

## 11. Multi-Tenancy & Multi-Document Support

### 11.1 Tenant Model

A **tenant** has:
- Knowledge base documents (one or more PDFs)
- Embedding index (vector store partition)
- RAG configuration (chunk size, overlap, retrieval strategy)
- LLM configuration (model, temperature, system prompt)
- API keys (one or more)
- Rate limits

### 11.2 Data Isolation

| Layer | Isolation Method |
|-------|-----------------|
| **API Keys** | Each key maps to one tenant via `TenantConfigPort` |
| **Vector Store** | Per-tenant namespace (in `VectorSearchPort`: tenant is implicit context) |
| **Blob Storage** | Per-tenant prefix in `DocumentStoragePort` |
| **Database** | `tenant_id` column, enforced via row-level filtering |

### 11.3 Multi-Document Knowledge Base

Each document per tenant:
- Has a unique `document_id`
- Chunked independently
- Metadata: `source`, `document_id`, `page`, `section`
- Add/update/remove without affecting others
- Reindex is per-document (incremental)
