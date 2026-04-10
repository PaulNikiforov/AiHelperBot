# План реализации: исправления по review_all.md

**Дата:** 2026-04-09
**Источник:** review_all.md + опрос

---

## Блок 1 — Баги (Critical)

### 1.1 Исправить двойной CANDIDATE_BUFFER

**Решение:** Убрать `+ CANDIDATE_BUFFER` из `searchCandidates()`. Логика буферизации остаётся у callers — они уже передают `maxDocuments + CANDIDATE_BUFFER`.

**Файл:** `service/rag/DocumentRetriever.java:207`
```java
// ДО
private List<Document> searchCandidates(String searchQuery, int maxDocuments) {
    List<Document> results = indexer.search(searchQuery, maxDocuments + CANDIDATE_BUFFER);

// ПОСЛЕ
private List<Document> searchCandidates(String searchQuery, int maxDocuments) {
    List<Document> results = indexer.search(searchQuery, maxDocuments);
```

Проверить: `retrieveForDefinition()` и `retrieveForGeneral()` не добавляют буфер при вызове `searchCandidates()` — после фикса они будут запрашивать `maxDocuments` без буфера. Решение: добавить `+ CANDIDATE_BUFFER` и в эти два вызова, чтобы все методы были единообразны.

---

### 1.2 Исправить `retrieveForSupport()` — использовать `searchCandidates()`

**Файл:** `service/rag/DocumentRetriever.java:84-91`
```java
// ДО
List<Document> docs = indexer.search(searchQuery, maxDocuments + CANDIDATE_BUFFER);

// ПОСЛЕ
List<Document> docs = searchCandidates(searchQuery, maxDocuments + CANDIDATE_BUFFER);
```

---

### 1.3 `BotFeedback.id` — добавить `private`

**Файл:** `model/BotFeedback.java:30`
```java
// ДО
Long id;

// ПОСЛЕ
private Long id;
```

---

## Блок 2 — LLM ошибки → HTTP 503

**Решение:** Убрать `catch (LlmException e)` из `RagService.answer()`. Пусть исключение всплывает до `GlobalExceptionHandler.handleLlmException()`, который вернёт 503 + JSON.

**Файл 1:** `service/rag/RagService.java` — удалить блок catch:
```java
// УДАЛИТЬ:
} catch (LlmException e) {
    log.error("LLM call failed for question: {}", question, e);
    return ERROR_RESPONSE;
}
// УДАЛИТЬ константу ERROR_RESPONSE (она больше не нужна)
```

**Файл 2:** `service/rag/RagService.java` — метод `answer()` теперь может бросать `LlmException`, это ожидаемое поведение.

**Результат:** `GlobalExceptionHandler.handleLlmException()` оживает и возвращает:
```json
{ "errorCode": "LLM_ERROR", "message": "AI service temporarily unavailable" }
```
с HTTP 503.

---

## Блок 3 — Topics feature — удалить отовсюду

**Решение:** Фича мертвая, убрать все упоминания.

**Изменения:**
1. `config/properties/BotProperties.java` — переименовать класс или оставить только `introText` (уже так, ничего не менять в коде)
2. `CLAUDE.md` — убрать строку `solbeg.bot.topics — List of topics shown to users`
3. `controller/BotTopicsController.java` — эндпоинт `/bot/topics` переименовать в `/bot/intro` или оставить, обновить Swagger-описание чтобы не вводило в заблуждение
4. Обновить `CLAUDE.md`: описание `BotTopicsServiceImpl.getIntroAndTopics()` поправить — метод возвращает только introText

---

## Блок 4 — Mapper + truncation

**Решение:** Оставить `@Mapping(target = "answer", ignore = true)`, но добавить truncation в `BotFeedbackServiceImpl.save()`.

**Файл:** `service/BotFeedbackServiceImpl.java`
```java
// ДО
entity.setAnswer(request.answer());

// ПОСЛЕ
private static final int MAX_ANSWER_LENGTH = 10_000;

entity.setAnswer(truncate(request.answer(), MAX_ANSWER_LENGTH));

private static String truncate(String value, int maxLength) {
    return value != null && value.length() > maxLength
            ? value.substring(0, maxLength)
            : value;
}
```

**Примечание:** `@Size(max = 10000)` на DTO валидирует входные данные и кинет 400 при превышении. Truncation в сервисе добавляем как защитный слой на случай, если данные придут в обход валидации (например, прямой вызов сервиса).

---

## Блок 5 — Ручной `Stream.concat` в DocumentRetriever → использовать ranker

**Решение:** Заменить ручной merge в `retrieveForRemindPeers()` и `retrieveForGrowthPlan()` на вызов методов `DocumentRanker`.

**Файл:** `service/rag/DocumentRetriever.java`

`retrieveForRemindPeers()`: вместо ручного concat использовать `documentRanker.mergeWithRemindSubmissionPage()` (добавить метод в DocumentRanker по аналогии с существующими) или `mergeWithSupportPage`.

`retrieveForGrowthPlan()`: аналогично — нет подходящего метода в Ranker для growth plan (4 страницы), поэтому здесь допустимо оставить ручной concat, но вынести логику в отдельный приватный метод `mergeWithPages(roleDocs, candidates)`.

**Конкретный план:**
- `retrieveForRemindPeers()`: добавить `mergeWithRemindSubmissionPage(docs, page, max)` в `DocumentRanker`, использовать его
- `retrieveForGrowthPlan()`: оставить ручной concat, но вынести в приватный хелпер `mergeAndRankByKeywords(primaryDocs, candidates, rankingQuery, maxDocuments)` чтобы не дублировать паттерн

---

## Блок 6 — Dead code — удалить всё

| Что удалить | Файл | Строка |
|-------------|------|--------|
| `EmbeddingIndexer.getStore()` | `service/rag/EmbeddingIndexer.java` | 57-59 |
| `GuideProperties.filePath` + Javadoc | `config/properties/GuideProperties.java` | 13-17 |
| `RestException(String, int, String)` | `exception/RestException.java` | 12-14 |
| `RestException(ErrorCodes, boolean)` | `exception/RestException.java` | 16-18 |
| `CodedException(String, int, String)` | `exception/CodedException.java` | 19-23 |
| `CodedException(ErrorCodes, boolean)` | `exception/CodedException.java` | 25-29 |
| `@Slf4j` на `BotQueryController` | `controller/BotQueryController.java` | 21 |

---

## Блок 7 — Integration тесты с TestContainers

**Что написать:**

### 7.1 `BotFeedbackControllerIT`
- `POST /api/v1/botfeedback` — сохранение, проверка 201
- `GET /api/v1/botfeedback/{id}` — получение по id, проверка 200
- `GET /api/v1/botfeedback/99999` — несуществующий, проверка 404
- `POST` с невалидными данными (пустой question) — проверка 400

### 7.2 `BotFeedbackServiceIT`
- Truncation: передать answer длиннее 10000 символов, проверить что сохранилось ровно 10000

### 7.3 Конфигурация TestContainers
```java
@Testcontainers
@SpringBootTest
class BotFeedbackControllerIT {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    // DynamicPropertySource для datasource
}
```

**Примечание:** Убрать H2 из test scope (или оставить для unit-тестов). TestContainers использовать только для integration-тестов.

---

## Блок 8 — Мелкие исправления

### 8.1 Двойной пробел в GlobalExceptionHandler
**Файл:** `exceptionhandler/GlobalExceptionHandler.java:20`
```java
// ДО
private  static final String MESSAGE = "message";
// ПОСЛЕ
private static final String MESSAGE = "message";
```

### 8.2 Эндпоинт /bot/topics — Swagger описание
Обновить `@Operation(summary = ...)` в `BotTopicsController`, чтобы было понятно что возвращается только intro text.

---

## Порядок реализации

| # | Задача | Файлы | Приоритет |
|---|--------|-------|-----------|
| 1 | Фикс `BotFeedback.id` — добавить `private` | `BotFeedback.java` | Critical |
| 2 | Фикс двойного CANDIDATE_BUFFER | `DocumentRetriever.java` | Critical |
| 3 | Фикс `retrieveForSupport()` → `searchCandidates()` | `DocumentRetriever.java` | Critical |
| 4 | LLM ошибки → 503: убрать catch в RagService | `RagService.java` | High |
| 5 | Удалить topics из CLAUDE.md и Swagger | `CLAUDE.md`, `BotTopicsController.java` | High |
| 6 | Truncation в BotFeedbackServiceImpl | `BotFeedbackServiceImpl.java` | High |
| 7 | Dead code — удалить (7 мест) | см. Блок 6 | Medium |
| 8 | Ручной concat → ranker в DocumentRetriever | `DocumentRetriever.java`, `DocumentRanker.java` | Medium |
| 9 | Integration тесты с TestContainers | новые файлы в `src/test/` | Medium |
| 10 | Мелкие исправления (пробел, Swagger) | `GlobalExceptionHandler.java`, `BotTopicsController.java` | Low |
