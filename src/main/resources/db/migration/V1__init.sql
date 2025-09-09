CREATE TABLE IF NOT EXISTS sessions
(
    id                  UUID PRIMARY KEY,
    vacancy_url         TEXT        NOT NULL,
    status              VARCHAR(16) NOT NULL,
    num_questions       INT,
    interview_plan      text        NOT NULL,
    instructions        TEXT        NULL,
    communication_style TEXT        NULL,
    started_at          TIMESTAMP   NULL,
    ended_at            TIMESTAMP   NULL,
    created_at          TIMESTAMP   NOT NULL
);
