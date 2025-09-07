CREATE TABLE IF NOT EXISTS messages
(
    id         UUID PRIMARY KEY,
    session_id UUID        REFERENCES sessions (id),
    role       VARCHAR(16) NOT NULL,
    internal   BOOLEAN     NOT NULL,
    content    TEXT        NOT NULL,
    created_at TIMESTAMP   NOT NULL
);
