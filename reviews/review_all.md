# Full Project Review: AiHelperBot

**Date:** 2026-04-09
**Scope:** All source code, configuration, dependencies, documentation

---

## 1. Bugs

### 1.1 `BotFeedback.id` missing `private` modifier

**File:** `model/BotFeedback.java:30`

```java
Long id;  // package-private, all other fields are private
```

Все остальные поля в классе помечены `private`, но `id` объявлено с пакетным доступом. Скорее всего ошибка. Lombok `@Getter/@Setter` все равно генерирует accessors, но прямой доступ к полю из того же пакета не должен быть разрешен.

---

### 1.2 Двойная буферизация кандидатов в `DocumentRetriever`

**File:** `service/rag/DocumentRetriever.java:207-209`

```java
private List<Document> searchCandidates(String searchQuery, int maxDocuments) {
    List<Document> results = indexer.search(searchQuery, maxDocuments + CANDIDATE_BUFFER);
    // ...
}
```

Метод `searchCandidates()` **сам** добавляет `CANDIDATE_BUFFER` (10) к `maxDocuments`. Но большинство вызывающих методов уже передают `maxDocuments + CANDIDATE_BUFFER`:

- `retrieveForTimeRelated()` (line 75): `searchCandidates(searchQuery, maxDocuments + CANDIDATE_BUFFER)` -> итого `maxDocuments + 20`
- `retrieveForInbox()` (line 94): то же
- `retrieveForPeers()` (line 103): то же
- `retrieveForReviewProcess()` (line 123): то же
- `retrieveForRemindPeers()` (line 154): то же
- `retrieveForGrowthPlan()` (line 182): то же

Только `retrieveForDefinition()` и `retrieveForGeneral()` не добавляют буфер при вызове, и получают `maxDocuments + 10`.

**Результат:** для большинства типов запросов из vector store извлекается на 10 документов больше, чем задумано. Непоследовательное поведение.

---

### 1.3 `retrieveForSupport()` обходит `searchCandidates()`

**File:** `service/rag/DocumentRetriever.java:86`

```java
List<Document> docs = indexer.search(searchQuery, maxDocuments + CANDIDATE_BUFFER);
```

Все остальные методы вызывают `searchCandidates()`, который оборачивает результат в null-safe проверку (`results != null ? results : List.of()`). `retrieveForSupport()` вызывает `indexer.search()` напрямую, что теоретически может вернуть `null`.

---

## 2. Dead Code

### 2.1 `EmbeddingIndexer.getStore()` - никогда не вызывается

**File:** `service/rag/EmbeddingIndexer.java:57-59`

```java
public CustomSimpleVectorStore getStore() {
    return store;
}
```

Нигде в проекте не используется. Можно удалить.

---

### 2.2 `GuideProperties.filePath` - не используется и не сконфигурирован

**File:** `config/properties/GuideProperties.java:17`

Поле объявлено, но:
- Нет соответствующей записи в `application.yml`
- Ни один класс не обращается к `getFilePath()`
- PDF всегда загружается из Azure Blob Storage

---

### 2.3 Два неиспользуемых конструктора `RestException`

**File:** `exceptionhandler/exception/RestException.java:12,16`

```java
public RestException(String message, int httpStatus, String errorCode) { ... }
public RestException(ErrorCodes errorCode, boolean writableStackTrace) { ... }
```

Используется только `RestException(ErrorCodes)` (в `BotFeedbackServiceImpl`). Соответственно, и конструкторы `CodedException(String, int, String)` и `CodedException(ErrorCodes, boolean)` тоже мертвый код.

---

### 2.4 `GlobalExceptionHandler.handleLlmException()` - недостижимый код

**File:** `exceptionhandler/GlobalExceptionHandler.java:31-38`

`RagService.answer()` перехватывает все `LlmException` на строке 84 и возвращает строку-заглушку с HTTP 200. `LlmException` не выбрасывается нигде, кроме `LlmClient`, а единственный потребитель `LlmClient` - это `RagService`, который глотает исключение. Обработчик в `GlobalExceptionHandler` никогда не сработает.

---

### 2.5 `@Slf4j` на `BotQueryController` без единого вызова `log`

**File:** `controller/BotQueryController.java:21`

Аннотация генерирует поле `log`, но класс не содержит ни одного вызова логирования.

---

### 2.6 Неиспользуемая конфигурация Spring AI Ollama

**File:** `application.yml:12-20`

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      embedding:
        enabled: true
        options:
          model: nomic-embed-text
      init:
        pull-model-strategy: never
```

Эта конфигурация парсится Spring Boot auto-configuration, но в коде нет **ни одного** прямого использования Ollama embedding client через Spring AI. `EmbeddingIndexer` работает через `CustomSimpleVectorStore`, который не вызывает Ollama напрямую через этот конфиг. Если Ollama embedding действительно нужен, то конфигурация корректна (используется через auto-configuration). Но стоит проверить и задокументировать.

---

## 3. Логические дубликаты

### 3.1 Три идентичных метода `mergeWith*Page()` в `DocumentRanker`

**File:** `service/rag/DocumentRanker.java:108-154`

```java
public List<Document> mergeWithGlossaryPage(List<Document> documents, int glossaryPage, int maxDocuments) {
    List<Document> glossaryDocs = indexer.findByPageNumber(glossaryPage);
    return Stream.concat(glossaryDocs.stream(), documents.stream())
            .distinct().limit(maxDocuments).toList();
}

public List<Document> mergeWithSupportPage(List<Document> documents, int supportPage, int maxDocuments) {
    List<Document> supportDocs = indexer.findByPageNumber(supportPage);
    return Stream.concat(supportDocs.stream(), documents.stream())
            .distinct().limit(maxDocuments).toList();
}

public List<Document> mergeWithInboxPage(List<Document> documents, int inboxPage, int maxDocuments) {
    List<Document> inboxDocs = indexer.findByPageNumber(inboxPage);
    return Stream.concat(inboxDocs.stream(), documents.stream())
            .distinct().limit(maxDocuments).toList();
}
```

Все три метода абсолютно идентичны по логике. Отличаются только именем параметра. Должен быть один метод `mergeWithPage(List<Document> documents, int pageNumber, int maxDocuments)`.

---

### 3.2 Дублирование логики merge в `retrieveForRemindPeers()` и `retrieveForGrowthPlan()`

**File:** `service/rag/DocumentRetriever.java:149-204`

Оба метода вручную делают `Stream.concat(roleDocs.stream(), candidates.stream()).distinct().toList()`, а затем `documentRanker.rankByKeywords(...)`. Эта логика (merge + rank) повторяется, хотя `DocumentRanker` уже содержит методы `mergeWith*Page()`, которые делают то же самое (но без ранкинга после merge). Нет единого паттерна "merge + rank".

---

## 4. Несогласованности

### 4.1 CLAUDE.md vs Код: BotFeedbackType

**CLAUDE.md** документирует:
> enum: `LIKE`, `DISLIKE`

**Код** (`model/BotFeedbackType.java`):
```java
public enum BotFeedbackType { LIKE, DISLIKE }
```

**Liquibase** (`001-create-bot-feedback.yaml`):
> `bot_feedback_type VARCHAR(50) NOT NULL`

Здесь все совпадает, но в **первом** описании CLAUDE.md (в таблице `bot_feedback`) указано:
> `bot_feedback_type` ... enum: `LIKE`, `DISLIKE`

Это корректно. Но в другом месте того же CLAUDE.md написано:
> BotFeedbackType — Enum (THUMBS_UP, THUMBS_DOWN)

Это противоречие в самой документации. Реальные значения - `LIKE` и `DISLIKE`.

---

### 4.2 CLAUDE.md vs Код: конфигурационный префикс

**CLAUDE.md** пишет:
> Config prefix is `solbeg.rag` for RAG properties and `solbeg.bot` for bot properties.

**Код:**
- `RagProperties`: `@ConfigurationProperties(prefix = "rag")`
- `BotProperties`: `@ConfigurationProperties(prefix = "bot")`

Реальные префиксы - `rag` и `bot`, без `solbeg.`.

---

### 4.3 CLAUDE.md vs Код: Topics feature

**CLAUDE.md** документирует:
> `solbeg.bot.topics` — List of topics shown to users

**Код:**
- `BotProperties` содержит только `introText`, **нет** поля `topics`
- `BotTopicsResponse` содержит только `introText`
- `BotTopicsServiceImpl.getIntroAndTopics()` возвращает только `introText`
- `application.yml` не содержит `bot.topics`

Фича "topics" упомянута в документации, но не реализована. Эндпоинт `/api/v1/bot/topics` и метод `getIntroAndTopics()` по названию обещают топики, но возвращают только intro text.

---

### 4.4 CLAUDE.md vs Код: "truncation" поведение

**CLAUDE.md** пишет:
> `BotFeedbackService` truncates answers to 10,000 chars before saving.

**Код** (`BotFeedbackServiceImpl.save()`):
```java
entity.setAnswer(request.answer());
```

Никакой truncation нет. Ограничение обеспечивается через `@Size(max = 10000)` на `BotFeedbackRequest.answer`, что вызовет **ошибку валидации** (HTTP 400), а не тихое обрезание. Поведение принципиально отличается от документированного.

---

### 4.5 Mapper игнорирует `answer`, а затем его ставят вручную

**File:** `mapper/BotFeedbackMapper.java:14` + `service/BotFeedbackServiceImpl.java:26`

```java
// Mapper
@Mapping(target = "answer", ignore = true)
BotFeedback toEntity(BotFeedbackRequest dto);

// Service
BotFeedback entity = botFeedbackMapper.toEntity(request);
entity.setAnswer(request.answer());  // <-- зачем ignore, если тут же ставим?
```

Нет причины игнорировать `answer` в маппере и потом ставить его вручную из того же объекта. Маппер мог бы маппить `answer` напрямую.

---

### 4.6 Непоследовательное логирование

| Класс | `@Slf4j` | Использование |
|-------|----------|---------------|
| `BotQueryController` | Да | Нет вызовов `log` |
| `BotFeedbackServiceImpl` | Нет | Нет логирования вообще |
| `BotTopicsServiceImpl` | Да | 1 вызов `log.info()` |
| `RagService` | Да | Активно используется |
| `DocumentRetriever` | Да | Активно используется |

Нет единого подхода: где-то аннотация без использования, где-то отсутствует совсем в сервисе, обрабатывающем пользовательские данные.

---

## 5. Дизайн-проблемы

### 5.1 LLM-ошибки возвращаются как HTTP 200

**File:** `service/rag/RagService.java:84-87`

```java
} catch (LlmException e) {
    log.error("LLM call failed for question: {}", question, e);
    return ERROR_RESPONSE;  // возвращается как обычный ответ с HTTP 200
}
```

Клиент не может отличить успешный ответ от ошибки LLM. Оба приходят с HTTP 200 в поле `answer`. При этом в `GlobalExceptionHandler` есть обработчик для `LlmException`, который вернул бы 503, но он никогда не срабатывает (см. п. 2.4).

---

### 5.2 `@Component` на property-классах вместо `@ConfigurationPropertiesScan`

**Файлы:** `RagProperties`, `RagValidationProperties`, `BotProperties`

Все используют `@Component` + `@ConfigurationProperties`. Идиоматический подход Spring Boot - `@ConfigurationPropertiesScan` или `@EnableConfigurationProperties`. `@Component` работает, но смешивает ответственности.

---

### 5.3 LlmException не наследует CodedException

`LlmException` extends `RuntimeException`, а `CodedException` - это отдельная иерархия для REST-ошибок. Из-за этого в `GlobalExceptionHandler` два разных обработчика с разным форматом ответа:
- `CodedException` -> `{ errorCode, message }` с кастомным HTTP-статусом
- `LlmException` -> `{ errorCode: "LLM_ERROR", message }` с 503

При этом `LlmException` handler недостижим (п. 2.4). Если бы `LlmException` наследовал `CodedException`, формат ответов был бы единообразным.

---

## 6. Лишние зависимости

### 6.1 `spring-ai-starter-model-ollama`

Если Ollama embedding используется только через auto-configuration Spring AI (а в коде нет явного Ollama-клиента), стоит проверить, действительно ли эта зависимость нужна, или embedding работает через кастомный `CustomSimpleVectorStore` без Spring AI Ollama starter.

### 6.2 TestContainers (в pom.xml, но не используется в тестах)

В `pom.xml` есть зависимости `testcontainers` (junit-jupiter, postgresql) со scope `test`, но единственный тест `AiHelperBotApplicationTests` использует H2, а не TestContainers. Зависимости не задействованы.

---

## 7. Мелкие замечания

| # | Файл | Замечание |
|---|------|-----------|
| 7.1 | `GlobalExceptionHandler.java:20` | Двойной пробел: `private  static final String MESSAGE` |
| 7.2 | `BotFeedbackController.java:33` | POST возвращает 201, но без `Location` header |
| 7.3 | `BotFeedbackRepository.java:7` | Избыточная аннотация `@Repository` на интерфейсе `JpaRepository` |
| 7.4 | `CodedException.java:10` | `@Setter` на классе с `final` полями `httpStatus` и `errorCode` - Lombok сгенерирует сеттеры, но они бесполезны для final-полей (компилятор запретит) |

Проверим п. 7.4 детальнее: поля `httpStatus` и `errorCode` не `final` в `CodedException`, хотя логически должны быть иммутабельными. `@Setter` позволяет их менять после создания, что нарушает инвариант.

---

## 8. Тесты

Единственный тест в проекте - `AiHelperBotApplicationTests.contextLoads()`. Тестирует только загрузку контекста Spring. Нет тестов для:
- RAG pipeline (`RagService`, `DocumentRetriever`, `DocumentRanker`, `QueryAnalyzer`)
- Input validation (`InputValidator`, фильтры)
- `LlmClient`
- `BotFeedbackService`
- Controllers
- Edge cases (пустые документы, null-значения, граничные длины)

Зависимости TestContainers есть, но ни одного integration-теста не написано.

---

## Сводка по приоритетам

### Critical (надо исправить)
| # | Проблема | Тип |
|---|----------|-----|
| 1.2 | Двойная буферизация `CANDIDATE_BUFFER` | Bug |
| 1.3 | `retrieveForSupport()` обходит null-safe обертку | Bug |
| 5.1 | LLM ошибки маскируются под HTTP 200 | Design |

### High (стоит исправить)
| # | Проблема | Тип |
|---|----------|-----|
| 1.1 | `BotFeedback.id` без `private` | Bug |
| 3.1 | Три идентичных `mergeWith*Page()` | Duplication |
| 4.3 | Topics feature не реализована, но документирована | Inconsistency |
| 4.4 | Truncation документирована, но не реализована | Inconsistency |
| 8 | Практически отсутствует тестовое покрытие | Testing |

### Medium (желательно)
| # | Проблема | Тип |
|---|----------|-----|
| 2.1-2.5 | Dead code (5 случаев) | Cleanup |
| 2.6 | Неиспользуемая конфигурация Ollama | Cleanup |
| 4.5 | Mapper ignore + ручная установка answer | Inconsistency |
| 6.2 | Неиспользуемые TestContainers зависимости | Dependency |

### Low (по возможности)
| # | Проблема | Тип |
|---|----------|-----|
| 4.1-4.2 | Расхождения CLAUDE.md с кодом | Documentation |
| 4.6 | Непоследовательное логирование | Consistency |
| 5.2 | `@Component` vs `@ConfigurationPropertiesScan` | Convention |
| 7.1-7.4 | Мелкие code style замечания | Style |
