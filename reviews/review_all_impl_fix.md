# Отчёт о выполнении: review_all_impl.md

**Дата:** 2026-04-09
**Статус компиляции:** ✅ `mvnw compile` — успешно, без ошибок

---

## Выполненные задачи

### 1. ✅ `BotFeedback.id` — добавлен `private`

**Файл:** `model/BotFeedback.java:30`

```java
// До
Long id;

// После
private Long id;
```

---

### 2. ✅ Двойной CANDIDATE_BUFFER — исправлен

**Файл:** `service/rag/DocumentRetriever.java`

- Убран `+ CANDIDATE_BUFFER` из `searchCandidates()` — теперь метод передаёт `maxDocuments` как есть
- `retrieveForDefinition()` теперь явно передаёт `maxDocuments + CANDIDATE_BUFFER` (было без буфера)
- Все методы единообразны: каждый caller добавляет буфер сам, `searchCandidates()` не дублирует

```java
// До
private List<Document> searchCandidates(String searchQuery, int maxDocuments) {
    List<Document> results = indexer.search(searchQuery, maxDocuments + CANDIDATE_BUFFER);

// После
private List<Document> searchCandidates(String searchQuery, int maxDocuments) {
    List<Document> results = indexer.search(searchQuery, maxDocuments);
```

---

### 3. ✅ `retrieveForSupport()` — теперь использует `searchCandidates()`

**Файл:** `service/rag/DocumentRetriever.java:84`

```java
// До
List<Document> docs = indexer.search(searchQuery, maxDocuments + CANDIDATE_BUFFER);

// После
List<Document> docs = searchCandidates(searchQuery, maxDocuments + CANDIDATE_BUFFER);
```

Теперь `retrieveForSupport()` имеет ту же null-safe защиту, что и остальные методы.

---

### 4. ✅ LLM ошибки → HTTP 503

**Файл:** `service/rag/RagService.java`

- Удалён `catch (LlmException e)` из `answer()`
- Удалена константа `ERROR_RESPONSE` (текст "Bot temporarily unavailable...")
- Добавлена константа `UNAVAILABLE_RESPONSE` для случая "vector store not loaded" (семантически отдельный случай)
- `LlmException` теперь всплывает до `GlobalExceptionHandler.handleLlmException()` → HTTP 503 + JSON
- Импорт `LlmException` удалён из `RagService`

```java
// До (в answer()):
try {
    String answer = generateAnswer(question, documents);
    ...
} catch (LlmException e) {
    log.error("LLM call failed for question: {}", question, e);
    return ERROR_RESPONSE;
}

// После:
String answer = generateAnswer(question, documents);
...
// LlmException всплывает → GlobalExceptionHandler → 503
```

---

### 5. ✅ Topics feature — удалена из всех мест

**Изменения:**
- `CLAUDE.md`: строка `| GET | /api/v1/bot/topics | ... | Get bot intro text and topic list |` → `Get bot intro text`
- `CLAUDE.md`: удалена строка `| solbeg.bot.topics | List of topics shown to users |`
- `CLAUDE.md`: уточнено описание truncation — теперь отражает реальное поведение (defensive guard + DTO validation)
- `controller/BotTopicsController.java`: `@Tag(name = "Bot intro and topics")` → `@Tag(name = "Bot intro")`
- `controller/BotTopicsController.java`: `@Operation(summary = "Get bot intro text and available topics")` → `@Operation(summary = "Get bot intro text")`

---

### 6. ✅ Truncation добавлен в `BotFeedbackServiceImpl`

**Файл:** `service/BotFeedbackServiceImpl.java`

```java
private static final int MAX_ANSWER_LENGTH = 10_000;

entity.setAnswer(truncate(request.answer(), MAX_ANSWER_LENGTH));

private static String truncate(String value, int maxLength) {
    return value != null && value.length() > maxLength ? value.substring(0, maxLength) : value;
}
```

CLAUDE.md обновлён: теперь документировано что `@Size(max=10000)` защищает API, а truncation — defensive guard для прямых вызовов сервиса.

---

### 7. ✅ Dead code удалён (7 мест)

| Что удалено | Файл |
|-------------|------|
| `EmbeddingIndexer.getStore()` | `service/rag/EmbeddingIndexer.java` |
| `GuideProperties.filePath` + Javadoc | `config/properties/GuideProperties.java` |
| `RestException(String, int, String)` | `exception/RestException.java` |
| `RestException(ErrorCodes, boolean)` | `exception/RestException.java` |
| `CodedException(String, int, String)` | `exception/CodedException.java` |
| `CodedException(ErrorCodes, boolean)` | `exception/CodedException.java` |
| `@Slf4j` + import на `BotQueryController` | `controller/BotQueryController.java` |

---

### 8. ✅ Ручной `Stream.concat` в `DocumentRetriever` — заменён

**Изменения:**

- `DocumentRanker`: добавлен метод `mergeWithRemindSubmissionPage(docs, page, max)` по аналогии с `mergeWithGlossaryPage` / `mergeWithSupportPage` / `mergeWithInboxPage`
- `retrieveForRemindPeers()`: ручной `Stream.concat` заменён на `documentRanker.mergeWithRemindSubmissionPage(...)`
- `retrieveForGrowthPlan()`: ручной loop + `Stream.concat` вынесен в приватный хелпер `mergeWithConsecutivePages(candidates, startPage, pageCount, max)` — сам метод теперь читается чище

```java
// До (retrieveForRemindPeers):
List<Document> remindDocs = indexer.findByPageNumber(remindPage);
List<Document> merged = Stream.concat(remindDocs.stream(), candidates.stream()).distinct().toList();

// После:
List<Document> merged = documentRanker.mergeWithRemindSubmissionPage(candidates, remindPage, maxDocuments + CANDIDATE_BUFFER);
```

```java
// Новый приватный хелпер в DocumentRetriever:
private List<Document> mergeWithConsecutivePages(
        List<Document> candidates, int startPage, int pageCount, int maxDocuments) {
    List<Document> pageDocs = new ArrayList<>();
    for (int i = 0; i < pageCount; i++) {
        pageDocs.addAll(indexer.findByPageNumber(startPage + i));
    }
    return Stream.concat(pageDocs.stream(), candidates.stream())
            .distinct().limit(maxDocuments).toList();
}
```

---

### 9. ✅ Integration-тесты с TestContainers добавлены

**Новые файлы:**

#### `controller/BotFeedbackControllerIT.java`
- `saveFeedback_validRequest_returns201WithBody` — POST возвращает 201, body содержит id
- `getFeedback_existingId_returns200` — GET по сохранённому id возвращает 200
- `getFeedback_nonExistentId_returns404` — GET несуществующего id возвращает 404
- `saveFeedback_blankQuestion_returns400` — пустой question → 400
- `saveFeedback_nullFeedbackType_returns400` — null botFeedbackType → 400

#### `service/BotFeedbackServiceIT.java`
- `save_answerExactlyAtLimit_savedWithoutTruncation` — answer = 10000 символов → сохраняется без изменений
- `save_answerLongerThanLimit_isTruncatedTo10000` — answer = 15000 символов → сохраняется 10000
- `save_shortAnswer_savedAsIs` — короткий answer → сохраняется как есть

**Конфигурация тестов:**
- `@Testcontainers` + `@ServiceConnection` с `PostgreSQLContainer<>("postgres:15")`
- `@MockBean EmbeddingModel` — Ollama не нужен
- `@MockBean AzureBlobStorageService` — Azure Blob не вызывается, `BootstrapIndexRunner` деградирует gracefully
- `@TestPropertySource(spring.liquibase.enabled=true, ddl-auto=validate)` — реальная Liquibase-миграция

---

### 10. ✅ Мелкие исправления

- `GlobalExceptionHandler.java`: исправлен двойной пробел `private  static final` → `private static final`
- `BotTopicsController.java`: `@Tag` и `@Operation` обновлены (см. п. 5)

---

## Итоговая таблица

| # | Задача | Статус | Файлы |
|---|--------|--------|-------|
| 1 | `BotFeedback.id` private | ✅ | `BotFeedback.java` |
| 2 | Двойной CANDIDATE_BUFFER | ✅ | `DocumentRetriever.java` |
| 3 | `retrieveForSupport()` → `searchCandidates()` | ✅ | `DocumentRetriever.java` |
| 4 | LLM ошибки → 503 | ✅ | `RagService.java` |
| 5 | Topics feature удалена | ✅ | `CLAUDE.md`, `BotTopicsController.java` |
| 6 | Truncation в сервисе | ✅ | `BotFeedbackServiceImpl.java` |
| 7 | Dead code удалён (7 мест) | ✅ | 5 файлов |
| 8 | Ручной concat → ranker | ✅ | `DocumentRetriever.java`, `DocumentRanker.java` |
| 9 | Integration-тесты | ✅ | 2 новых файла |
| 10 | Мелкие исправления | ✅ | `GlobalExceptionHandler.java`, `BotTopicsController.java` |

**Компиляция:** ✅ Успешно  
**Изменено файлов:** 12  
**Добавлено файлов:** 2 (тесты)
