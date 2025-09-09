# 📘 Спецификация Backend-сервиса AI Interviewer

## 1. Цель
Сервис позволяет проводить тренировочные собеседования (**ai interviewer**) на основе вакансий с hh.ru.  
Интервьюером выступает LLM (через Spring AI). После прохождения всех вопросов кандидат получает подробный фидбек.

---

## 2. Основной флоу

1. **Создание сессии** (`POST /sessions`)
    - Вход: `vacancyUrl`, опционально `numQuestions`, опционально `instructions`, опционально `communicationStyle`.
    - Бэкенд парсит вакансию с hh.ru и формирует план интервью.
    - Генерируется вступительное сообщение: краткая сводка вакансии и план интервью.
    - Статус сессии = `planned`.
    - Ответ содержит `Session` с `sessionId`, статусом и первым сообщением (план).

2. **Запуск интервью** (`POST /sessions/{sessionId}/messages`)
    - Пользователь отправляет `"Начать интервью"`.
    - Статус = `ongoing`.
    - Ассистент начинает интервью: задаёт по одному вопросу.

3. **Проведение Q&A**
    - Всего вопросов: `numQuestions`.
    - Формат: вопрос → ответ → следующий вопрос.
    - Ответ можно отправлять текстом или аудио; аудио автоматически транскрибируется в текст.
    - Все сообщения сохраняются в БД.

4. **Завершение**
    - После последнего вопроса или команды завершения ассистент выводит строку `Интервью завершено` и даёт развёрнутый фидбек (сильные/слабые стороны, соответствие вакансии, рекомендации).
    - Статус = `completed`.

---

## 3. Статусы сессии

- **`planned`** — план интервью построен, ждём подтверждения («Начать интервью»).
- **`ongoing`** — идёт интервью (Q&A).
- **`completed`** — интервью завершено, выдан финальный фидбек.

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
- **VacancyService** — загрузка и нормализация описания вакансии с hh.ru.
- **Prompts** — шаблоны промптов (план интервью, системный промпт интервьюера).
- **SessionChatMemory** — хранение и подготовка контекста/истории сообщений для LLM по сессии.
- **Repository слой** — `SessionRepository` (Spring Data JPA) для сессий.
- **Конфигурация** — `ApplicationConfig`, `ProxyConfig` (опциональный прокси для исходящих запросов), `CorsConfig`.
- **Утилиты** — `JsonUtils`.

### Таблицы
- **sessions**: `id, vacancy_url, vacancy_title, status, num_questions, instructions, communicaton_style, interview_plan, created_at, started_at, ended_at`
- **messages**: `id, session_id, role, content, created_at`

---

## 5. OpenAPI 3.0 (Swagger) спецификация

```yaml
openapi: 3.0.3
info:
  title: AI Interview Backend API
  version: "1.0"
servers:
  - url: https://api.example.com/
    description: Базовый URL API
components:
  schemas:
    CreateSessionRequest:
      type: object
      required:
        - vacancyUrl
      properties:
        vacancyUrl:
          type: string
          description: URL вакансии на hh.ru
          example: "https://hh.ru/vacancy/123456"
        numQuestions:
          type: integer
          description: Количество вопросов в интервью (по умолчанию 5)
          example: 5
        instructions:
          type: string
          description: Кастомные инструкции для составления интервью
          example: "Фокусируйся на практических кейсах и примерах проектов."
        communicationStyle:
          type: string
          description: Особые пожелания о стиле общения
          example: "Общение в дружелюбном, располагающем стиле на «ты»."
    SessionStatusResponse:
      type: object
      properties:
        status:
          $ref: "#/components/schemas/SessionStatus"
    MessageRequest:
      type: object
      required:
        - type
      properties:
        type:
          type: string
          enum: [ text, audio ]
          description: Тип сообщения
          example: text
        message:
          type: string
          description: Текст сообщения (обязательно для type=text)
          example: "Начать интервью"
        audioBase64:
          type: string
          description: Бинарные данные аудио в base64 (обязательно для type=audio)
        audioMimeType:
          type: string
          description: MIME-тип аудио (например, audio/webm, audio/mpeg). Обязательно для type=audio
    MessageResponse:
      type: object
      properties:
        sessionId:
          type: string
          example: "f3c1b9be-6b2a-4d3c-9a51-5d2a1c4a1a45"
        message:
          type: string
          description: Сообщение ассистента (вопрос или финальное сообщение с фидбеком)
          example: "Вопрос 1/5. Расскажите о вашем опыте со Spring Boot..."
        interviewComplete:
          type: boolean
          description: true — интервью завершено (текущее message содержит финальный фидбек)
          example: false
    Session:
      type: object
      properties:
        sessionId:
          type: string
        vacancyUrl:
          type: string
          example: "https://hh.ru/vacancy/123456"
        status:
          $ref: "#/components/schemas/SessionStatus"
        numQuestions:
          type: integer
          example: 5
        startedAt:
          type: string
          format: date-time
        endedAt:
          type: string
          format: date-time
        instructions:
          type: string
          description: Кастомные инструкции (если заданы при создании)
        messages:
          type: array
          description: История диалога
          items:
            type: object
            properties:
              role:
                type: string
                enum: [assistant, user]
                example: "assistant"
              content:
                type: string
                example: "Роль: ... План интервью: ... Если план подходит — нажмите 'Начать интервью'."
    SessionStatus:
      type: string
      enum: [ planned, ongoing, completed ]
      example: "planned"
paths:
  /sessions:
    post:
      summary: Создать новую сессию интервью
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/CreateSessionRequest"
      responses:
        "201":
          description: Сессия создана
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/Session"
        "400":
          description: Некорректный запрос
        "500":
          description: Ошибка сервера
  /sessions/{sessionId}/messages:
    post:
      summary: Отправить сообщение пользователя и получить ответ ассистента
      parameters:
        - name: sessionId
          in: path
          required: true
          schema:
            type: string
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/MessageRequest"
      responses:
        "200":
          description: Ответ ассистента
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/MessageResponse"
        "404":
          description: Сессия не найдена
        "410":
          description: Сессия завершена
        "500":
          description: Ошибка сервера
  /sessions/{sessionId}/messages/stream:
    post:
      summary: Отправить сообщение пользователя и получить ответ ассистента в виде stream
      parameters:
        - name: sessionId
          in: path
          required: true
          schema:
            type: string
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/MessageRequest"
      responses:
        "200":
          description: Ответ ассистента
          content:
            text/event-stream:
              schema:
                type: string
                description:
                  SSE поток токенов ответа ассистента
              examples:
                stream:
                  summary: Пример SSE потока
                  value: |
                    data: Вопрос 1/5. Расскажите о вашем опыте со Spring

                    data:  Boot и микросервисами?

        "404":
          description: Сессия не найдена
        "410":
          description: Сессия завершена
        "500":
          description: Ошибка сервера
  /sessions/{sessionId}:
    get:
      summary: Получить состояние сессии и историю сообщений
      parameters:
        - name: sessionId
          in: path
          required: true
          schema:
            type: string
      responses:
        "200":
          description: Состояние сессии
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/Session"
        "404":
          description: Сессия не найдена
  /sessions/{sessionId}/status:
    get:
      summary: Получить состояние сессии и историю сообщений
      parameters:
        - name: sessionId
          in: path
          required: true
          schema:
            type: string
      responses:
        "200":
          description: Состояние сессии
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/SessionStatusResponse"
        "404":
          description: Сессия не найдена
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

- Команды этапов:
  - «План интервью»: стартовая команда. Ассистент приветствует и кратко описывает вакансию, цели и темы интервью. Далее ожидается ввод "Начать интервью".
  - «Начать интервью»: ассистент переходит к первому вопросу.
  - «Завершить интервью»: немедленное завершение. Ассистент выводит на первой строке фразу: `Интервью завершено`, затем даёт финальный фидбек.

- Формат основного вопроса:
  - `Вопрос X/N (Тема: <...>): …?`

- Формат уточняющего вопроса:
  - `Уточняющий вопрос (тема: <...>): …?`

- Стиль общения:
  - По умолчанию дружелюбный, на «ты». Можно задать через `communicationStyle` при создании сессии.
