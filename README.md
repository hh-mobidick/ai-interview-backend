# 📘 Спецификация Backend-сервиса AI Interviewer

## 1. Цель
Сервис позволяет проводить тренировочные собеседования (**ai interviewer**) на основе вакансий с hh.ru.  
Интервьюером выступает LLM (через Spring AI). После прохождения всех вопросов кандидат получает подробный фидбек.

---

## 2. Основной флоу

1. **Создание сессии** (`POST /sessions`)
    - Вход: `vacancyUrl`, опционально `numQuestions`, опционально `instructions`.
    - Бэкенд парсит вакансию с hh.ru.
    - Генерирует вступительное сообщение: summary вакансии + план интервью (темы).
    - Статус сессии = `planned`.
    - Пользователь получает `sessionId` и вступительное сообщение.

2. **Запуск интервью** (`POST /sessions/{id}/messages`)
    - Пользователь отправляет `"Начать интервью"`.
    - Статус = `ongoing`.
    - Ассистент начинает интервью: задаёт по одному вопросу.

3. **Проведение Q&A**
    - Всего вопросов: `numQuestions`.
    - Формат: вопрос → ответ → следующий вопрос.
    - Все сообщения сохраняются в БД.

4. **Завершение**
    - После последнего ответа LLM формирует итоговое сообщение с фидбеком:
        - Общая оценка (1–5).
        - Соответствие вакансии.
        - Сильные стороны.
        - Слабые стороны.
        - Оценка по каждой теме.
        - Рекомендации по улучшению.
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
- **Spring AI** (OpenAI GPT-4)
- **Postgres** для хранения сессий и сообщений
- **Lombok, Spring Data JPA** для удобства разработки
- **Slf4j** для логирования промптов и диалога

### Основные компоненты
- **SessionController** (REST API)
- **InterviewService** (бизнес-логика интервью)
- **VacancyService** (парсинг hh.ru вакансии)
- **LLMService** (обёртка над Spring AI для генерации промтов и ответов)
- **Repository слой** (`SessionRepository`, `MessageRepository`)

### Таблицы
- **sessions**: `id, vacancy_url, vacancy_title, status, num_questions, started_at, ended_at, instructions`
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
          description: Кастомные инструкции для интервьюера (будут добавлены в системный промт)
          example: "Фокусируйся на практических кейсах и примерах проектов."
    CreateSessionResponse:
      type: object
      properties:
        sessionId:
          type: string
          description: Уникальный идентификатор сессии
          example: "f3c1b9be-6b2a-4d3c-9a51-5d2a1c4a1a45"
        introMessage:
          type: string
          description: Вступительное сообщение ассистента (summary вакансии + план интервью + просьба нажать 'Начать интервью')
          example: "Роль: Senior Java Developer. Требования: Spring Boot, микросервисы... План: 1) Java Core 2) Spring 3) Системный дизайн. Если план подходит — нажмите 'Начать интервью'."
    SessionStatusResponse:
      type: object
      properties:
        status:
          $ref: "#/components/schemas/SessionStatus"
    MessageRequest:
      type: object
      required:
        - message
      properties:
        message:
          type: string
          description: Сообщение пользователя (например, 'Начать интервью' или ответ на вопрос)
          example: "Начать интервью"
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
                $ref: "#/components/schemas/CreateSessionResponse"
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
1. Установите ключ доступа к LLM в переменную окружения:
```bash
export OPENAI_API_KEY=your_key_here
```
2. Запустите сервис (скрипт поднимет Postgres через Docker Compose, соберёт и запустит приложение):
```bash
chmod +x ./run.sh
./run.sh
```

Приложение будет доступно на `http://localhost:8080`.
Swagger UI: `http://localhost:8080/swagger-ui/index.html`.

### Прокси для исходящих запросов в LLM
Если доступ к LLM возможен только через прокси, передайте параметр `proxy` в формате `host:port`:
```bash
./run.sh --proxy=proxy.example.com:8080
# или
./run.sh proxy=proxy.example.com:8080
```

В этом случае исходящие запросы к LLM пойдут через указанный прокси. Если параметр не задан — прокси не используется.

### Остановка Postgres
Скрипт оставляет контейнер БД запущенным. Остановить его можно так:
```bash
docker compose stop postgres
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
2. Запустите сервис (скрипт поднимет Postgres через Docker Compose, соберёт и запустит приложение):
```bash
chmod +x ./run.sh
./run.sh
```

Приложение будет доступно на `http://localhost:8080`.
Swagger UI: `http://localhost:8080/swagger-ui/index.html`.

### Прокси для исходящих запросов в LLM
Если доступ к LLM возможен только через прокси, передайте параметр `proxy` в формате `host:port`:
```bash
./run.sh --proxy=proxy.example.com:8080
# или
./run.sh proxy=proxy.example.com:8080
```

В этом случае исходящие запросы к LLM пойдут через указанный прокси. Если параметр не задан — прокси не используется.

### Передача ключа OpenAI через флаг
Вместо переменной окружения можно передать ключ через аргумент командной строки:
```bash
./run.sh --openai-key=sk-xxxx
# или
./run.sh openai-key=sk-xxxx
```
Скрипт экспортирует переменную `OPENAI_API_KEY` на время запуска приложения.

### Остановка Postgres
Скрипт оставляет контейнер БД запущенным. Остановить его можно так:
```bash
docker compose stop postgres
```
