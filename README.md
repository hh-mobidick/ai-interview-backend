# 📘 Спецификация Backend-сервиса AI Interviewer

## 1. Цель
Сервис позволяет проводить тренировочные собеседования (**ai interviewer**) на основе вакансий с hh.ru.  
Интервьюером выступает LLM (через Spring AI). После прохождения всех вопросов кандидат получает подробный фидбек.

---

## 2. Основной флоу (v1.3)

1. **Создание сессии**:
    - JSON: `POST /sessions` — режимы `mode=vacancy|role`.
      - `mode=vacancy`: источники — один из `vacancyUrl | vacancyText` (или файл в multipart).
      - `mode=role`: обязателен `roleName` (см. автодополнение `GET /roles/suggest`).
      - Доп. параметры: `numQuestions(1..50)`, `planPreferences`, `interviewFormat=training|moderate|realistic`, `communicationStylePreset`, `communicationStyleFreeform`.
    - Multipart: `POST /sessions/form` — поддержка `vacancyFile` (.txt | .pdf | .docx) + те же поля.
    - На выходе — объект `Session` со статусом `planned` и планом интервью.

2. **Коррекция плана (статус planned)**:
    - Любое обычное сообщение (не команда «Начать интервью») → перегенерация плана с учётом правок; остаёмся в `planned`.

3. **Запуск интервью (переход planned → ongoing)**:
    - Пользователь отправляет команду «Начать интервью».
    - Ассистент начинает задавать вопросы по плану.

4. **Проведение Q&A (статус ongoing)**:
    - Формат: вопрос → ответ → следующий вопрос; возможно SSE поток.
    - Сообщения: текст или аудио (только WAV в `audioBase64`).

5. **Фидбек (переход ongoing → feedback)**:
    - Команда «Обратная связь» переводит в режим обсуждения и даёт развёрнутый фидбек.

6. **Завершение (feedback → completed или ongoing → completed)**:
    - Команда «Завершить интервью» завершает сессию (первая строка ответа — «Интервью завершено»), далее финальный отзыв.

---

## 3. Статусы сессии

- **`planned`** — план построен; можно корректировать свободным текстом; «Начать интервью» → `ongoing`.
- **`ongoing`** — идёт интервью (Q&A); «Обратная связь» → `feedback`; «Завершить интервью» → `completed`.
- **`feedback`** — обсуждение оценки и рекомендации; «Завершить интервью» → `completed`.
- **`completed`** — сессия завершена; любые попытки писать → HTTP 410.

---

## 4. Архитектура

### Технологии
- **Java 21 + Spring Boot**
- **Spring AI** (OpenAI GPT-5)
- **Postgres** для хранения сессий и сообщений
- **Lombok, Spring Data JPA** для удобства разработки
- **Slf4j** для логирования промптов и диалога

### Основные компоненты
- **SessionController** — REST API (`/sessions`, `/messages`, `/messages/stream`, `/status`).
- **GlobalExceptionHandler** — единый маппинг ошибок в корректные HTTP-ответы.
- **InterviewService** — оркестрация сессии: смена статусов, ведение диалога, сохранение сообщений.
- **InterviewQueryService** — выдача информации по сессии.
- **TranscriptionService** — транскрибирует аудио (base64) пользователя в текст (Spring AI / OpenAI).
- **VacancyService** — парсинг вакансии по URL (включая не hh.ru), извлечение текста из `.txt/.pdf/.docx` (Apache Tika), нормализация.
- **Prompts** — шаблоны промптов (план интервью, системный промпт интервьюера).
- **SessionChatMemory** — хранение и подготовка контекста/истории сообщений для LLM по сессии.
- **Repository слой** — `SessionRepository` (Spring Data JPA) для сессий.
- **Конфигурация** — `ApplicationConfig`, `ProxyConfig` (опциональный прокси для исходящих запросов), `CorsConfig`.
- **Утилиты** — `JsonUtils`.

### Таблицы (основное)
- `sessions`: `id, mode, role_name, vacancy_url, status(planned|ongoing|feedback|completed), num_questions, interview_plan, interview_format(training|moderate|realistic), plan_preferences, communication_style_preset, communication_style_freeform, created_at, started_at, ended_at`
- `messages`: `id, session_id, role, content, created_at [, type]`

---

## 5. OpenAPI 3.0 (Swagger) спецификация

Актуальная спецификация — `src/main/resources/docs/swagger.yaml` (версия 1.3). Также доступна по `GET /swagger.yaml` и в Swagger UI.

Ключевые схемы:
- `CreateSessionRequestJson` и `CreateSessionRequestMultipart` (см. режимы `vacancy|role`).
- `MessageRequest` (`type: text|audio`, `audioBase64` — только WAV).
- `Session`, `SessionStatusResponse`, `ApiError`.

Основные эндпойнты:
- `POST /sessions` — создать сессию (JSON).
- `POST /sessions/form` — создать сессию (multipart, файл вакансии).
- `POST /sessions/{sessionId}/messages` — отправить сообщение и получить ответ.
- `POST /sessions/{sessionId}/messages/stream` — SSE поток (`text/event-stream`, строки `data: <chunk>`).
- `GET /sessions/{sessionId}` — получить состояние и историю.
- `GET /sessions/{sessionId}/status` — получить статус.
- `GET /roles/suggest?q=...` — подсказки по ролям.

Примеры:
```bash
curl -X POST http://localhost:10000/sessions \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer $AUTH_TOKEN' \
  -d '{
    "mode": "vacancy",
    "vacancyUrl": "https://example.com/vacancy/123",
    "numQuestions": 7,
    "planPreferences": "Сделай упор на архитектуру и SQL",
    "interviewFormat": "moderate",
    "communicationStylePreset": "friendly_ty"
  }'

curl -X POST http://localhost:10000/sessions/form \
  -H 'Authorization: Bearer $AUTH_TOKEN' \
  -F mode=vacancy -F vacancyFile=@job.txt -F numQuestions=5

curl -X POST http://localhost:10000/sessions/{id}/messages \
  -H 'Content-Type: application/json' -H 'Authorization: Bearer $AUTH_TOKEN' \
  -d '{"type":"text","message":"Начать интервью"}'
```
---

## Локальный запуск

### Требования
- Docker / Docker Desktop
- Java 21

### Быстрый старт
1. Установите ключ доступа к LLM в переменную окружения (или передайте через флаг `--openai-key`, см. ниже):
```bash
export OPENAI_API_KEY=your_key_here
```
2. (Опционально) Установите токен авторизации для API:
```bash
export AUTH_TOKEN="$(python3 -c 'import secrets; print(secrets.token_urlsafe(48))')"
```
   Этот токен будет требоваться в заголовке `Authorization: Bearer <token>` для защищённых эндпоинтов.

3. Запустите сервис (скрипт поднимет Postgres через Docker Compose, соберёт и запустит приложение):
```bash
chmod +x ./run.sh
./run.sh
```

Приложение будет доступно на `http://localhost:10000`.

Swagger UI: [`http://localhost:10000/swagger-ui/index.html`](http://localhost:10000/swagger-ui/index.html).

### Передача ключа OpenAI через флаг
Вместо переменной окружения можно передать ключ через аргумент командной строки:
```bash
./run.sh --openai-key=sk-xxxx
# или
./run.sh openai-key=sk-xxxx
```
Скрипт экспортирует переменную `OPENAI_API_KEY` на время запуска приложения.

### Передача AUTH_TOKEN через флаг
Можно передать токен авторизации напрямую в запуск:
```bash
./run.sh --auth-token=my-secret-token
# или
./run.sh auth-token=my-secret-token
```
Если переменная окружения `AUTH_TOKEN` или флаг не заданы, защищённые эндпоинты вернут 401.

### Прокси для исходящих запросов в LLM
Если доступ к LLM возможен только через прокси, передайте параметр `proxy` в формате `host:port`:
```bash
./run.sh --proxy=proxy.example.com:8080
# или
./run.sh proxy=proxy.example.com:8080
```

В этом случае исходящие запросы к LLM пойдут через указанный прокси. Если параметр не задан — прокси не используется.

### Запуск через Docker Compose (без локальной Java)
Можно запустить приложение и базу в контейнерах (сборка образа произойдёт автоматически):

1) Укажите необходимые переменные окружения для сервиса в `docker-compose.yml` → `services.app.environment`:
- `OPENAI_API_KEY` — обязательный ключ для доступа к LLM
- `OPENAI_API_PROJECT_ID` — опционально, если требуется
- `PROXY` — опционально, формат `host:port` для прокси исходящих запросов к LLM
- `AUTH_TOKEN` — строковый токен, который должен совпадать с заголовком `Authorization: Bearer <token>`

Пример фрагмента:
```yaml
services:
  app:
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/ai_interviewer
      DB_USERNAME: ai
      DB_PASSWORD: ai
      OPENAI_API_KEY: "sk-..."
      OPENAI_API_PROJECT_ID: ""
      AUTH_TOKEN: ""
      #PROXY: "proxy.example.com:8080"
```

2) Запустите docker-compose:
```bash
docker compose up -d --build
# или только приложение (база поднимется автоматически как зависимость):
docker compose up -d --build app
```

3) Проверьте логи при необходимости:
```bash
docker compose logs -f app | cat
```

4) Остановить контейнеры:
```bash
docker compose down
```

После запуска приложение доступно на `http://localhost:10000` (порт проброшен в `docker-compose.yml`).

### Остановка Postgres
Скрипт оставляет контейнер БД запущенным. Остановить его можно так:
```bash
docker compose stop postgres
```

---

## Формат и команды интервью

- Команды этапов (точные триггеры сообщений пользователя):
  - `План интервью` — стартовая команда. Краткая сводка вакансии/структуры.
  - `Начать интервью` — переход к первому вопросу.
  - `Обратная связь` — переход в режим обсуждения результатов (статус `feedback`).
  - `Завершить интервью` — немедленное завершение (первая строка ответа — `Интервью завершено`), далее финальный фидбек.

  Правила обработки триггеров:
  - Сопоставление нечувствительно к регистру и допускает, что пользовательский текст начинается с триггерной фразы (например, `Начать интервью, пожалуйста`).
  - В `planned` разрешён только `Начать интервью`. Сообщения, не начинающиеся с этой команды, трактуются как правки плана; команды `Обратная связь` и `Завершить интервью` в `planned` → 409 (`INVALID_STATUS_TRANSITION`).
  - В `ongoing` разрешены `Обратная связь` и `Завершить интервью`; `Начать интервью` → 409.
  - В `feedback` разрешено `Завершить интервью`; `Начать интервью` → 409.
  - При завершении ассистент обязан вывести точную строку `Интервью завершено` первой строкой ответа.

- Формат основного вопроса:
  - `Вопрос X/N (Тема: <...>): …?`

- Формат уточняющего вопроса:
  - `Уточняющий вопрос (тема: <...>): …?`

- Стиль общения и формат:
  - По умолчанию `communicationStylePreset` не задан → нейтральный дружелюбный тон.
  - `interviewFormat`: `training` (больше пояснений), `moderate` (по умолчанию), `realistic` (минимум подсказок).

## Ограничения и валидации

- Аудио: только WAV (передаётся в `audioBase64`). Базовая валидация RIFF/WAVE, `fmt`/`data` chunk'и, 1–2 канала, частота 8–48 кГц. Размер по умолчанию ≤ 25 МБ (настраивается).
- `numQuestions`: 1..50.
- `mode=vacancy`: обязателен хотя бы один источник (`vacancyUrl | vacancyText | vacancyFile`).
- `mode=role`: обязателен `roleName`.

## SSE поведение

- `POST /sessions/{sessionId}/messages/stream` возвращает `text/event-stream`.
- Каждый токен модели отдаётся в строке формата `data: <chunk>` и завершается переводом строки. Дополнительные служебные события (`event:`) не используются.
- Соединение закрывается, когда ответ полностью сформирован. Если в ответе присутствует триггер завершения, сессия помечается как `completed`.

## Ошибки (единый формат ApiError)

`{"code": "...", "message": "...", "details": {}}`

- 400: `VACANCY_NOT_PARSABLE`, `INVALID_INPUT`, `FILE_TYPE_NOT_SUPPORTED`, `FILE_TOO_LARGE`, `UNSUPPORTED_AUDIO_FORMAT`.
- 401: `UNAUTHORIZED` (если включена авторизация по токену).
- 404: `NOT_FOUND`.
- 409: `INVALID_STATUS_TRANSITION`.
- 410: `SESSION_COMPLETED`.
- 500: `INTERNAL_ERROR`.

Пример:
```json
{
  "code": "INVALID_STATUS_TRANSITION",
  "message": "Command is not allowed in planned status"
}
```

## Конфигурация

- Токен API: `auth.token` (заголовок `Authorization: Bearer <token>`). Если токен задан — эндпоинты требуют Bearer-токен; в Swagger описана схема `bearerAuth` и глобальное требование безопасности.
- Лимиты (см. `application.yaml` → `app.*`):
  - `app.max-file-size-bytes` (по умолчанию 5 МБ)
  - `app.max-audio-size-bytes` (по умолчанию 25 МБ)
  - `app.min-audio-sample-rate` / `app.max-audio-sample-rate` (по умолчанию 8–48 кГц)
