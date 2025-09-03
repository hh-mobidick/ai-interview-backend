CREATE TABLE IF NOT EXISTS sessions (
    id UUID PRIMARY KEY,
    vacancy_url TEXT NOT NULL,
    vacancy_title TEXT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('planned','ongoing','completed')),
    num_questions INT NOT NULL,
    started_at TIMESTAMP NULL,
    ended_at TIMESTAMP NULL,
    instructions TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sessions_status ON sessions(status);
CREATE INDEX IF NOT EXISTS idx_sessions_created_at ON sessions(created_at);
