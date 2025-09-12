-- Roles dictionary tables and seed data

-- Enable pgcrypto for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS roles
(
    id          UUID PRIMARY KEY,
    name        TEXT        NOT NULL,
    normalized  TEXT        NOT NULL,
    popularity  INT         NOT NULL DEFAULT 0,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS roles_normalized_idx ON roles (normalized);

CREATE TABLE IF NOT EXISTS role_synonyms
(
    id          UUID PRIMARY KEY,
    role_id     UUID        NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    name        TEXT        NOT NULL,
    normalized  TEXT        NOT NULL
);

CREATE INDEX IF NOT EXISTS role_synonyms_role_id_idx ON role_synonyms (role_id);
CREATE INDEX IF NOT EXISTS role_synonyms_norm_idx ON role_synonyms (normalized);

-- Seed a minimal roles set
INSERT INTO roles (id, name, normalized, popularity)
VALUES
    (gen_random_uuid(), 'Backend Developer (Java)', LOWER('Backend Developer (Java)'), 100),
    (gen_random_uuid(), 'Backend Developer (Go)', LOWER('Backend Developer (Go)'), 80),
    (gen_random_uuid(), 'Backend Developer (Node.js)', LOWER('Backend Developer (Node.js)'), 70),
    (gen_random_uuid(), 'Frontend Developer (React)', LOWER('Frontend Developer (React)'), 90),
    (gen_random_uuid(), 'Fullstack Developer (React + Java)', LOWER('Fullstack Developer (React + Java)'), 60),
    (gen_random_uuid(), 'Mobile Developer (iOS)', LOWER('Mobile Developer (iOS)'), 60),
    (gen_random_uuid(), 'Mobile Developer (Android)', LOWER('Mobile Developer (Android)'), 60),
    (gen_random_uuid(), 'Data Engineer', LOWER('Data Engineer'), 70),
    (gen_random_uuid(), 'Data Scientist', LOWER('Data Scientist'), 50),
    (gen_random_uuid(), 'ML Engineer', LOWER('ML Engineer'), 50),
    (gen_random_uuid(), 'DevOps Engineer', LOWER('DevOps Engineer'), 85),
    (gen_random_uuid(), 'Site Reliability Engineer', LOWER('Site Reliability Engineer'), 40),
    (gen_random_uuid(), 'QA Engineer', LOWER('QA Engineer'), 75),
    (gen_random_uuid(), 'Automation QA Engineer', LOWER('Automation QA Engineer'), 65),
    (gen_random_uuid(), 'Product Manager', LOWER('Product Manager'), 90),
    (gen_random_uuid(), 'Project Manager', LOWER('Project Manager'), 70),
    (gen_random_uuid(), 'Business Analyst', LOWER('Business Analyst'), 60),
    (gen_random_uuid(), 'System Analyst', LOWER('System Analyst'), 55),
    (gen_random_uuid(), 'UX/UI Designer', LOWER('UX/UI Designer'), 65)
ON CONFLICT DO NOTHING;


