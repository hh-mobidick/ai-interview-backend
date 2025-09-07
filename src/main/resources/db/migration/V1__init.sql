CREATE TABLE IF NOT EXISTS sessions
(
    id            UUID PRIMARY KEY,
    vacancy_url   TEXT        NOT NULL,
    status        VARCHAR(16) NOT NULL,
    num_questions INT,
    started_at    TIMESTAMP   NULL,
    ended_at      TIMESTAMP   NULL,
    instructions  TEXT        NULL,
    created_at    TIMESTAMP   NOT NULL
);
