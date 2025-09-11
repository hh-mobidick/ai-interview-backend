-- V2: Extend sessions with new fields and optionally messages with type

-- Sessions: add new columns for v2 API
ALTER TABLE IF EXISTS sessions
    ADD COLUMN IF NOT EXISTS mode VARCHAR(16) NULL,
    ADD COLUMN IF NOT EXISTS role_name TEXT NULL,
    ADD COLUMN IF NOT EXISTS interview_format VARCHAR(16) NULL,
    ADD COLUMN IF NOT EXISTS plan_preferences TEXT NULL,
    ADD COLUMN IF NOT EXISTS communication_style_preset VARCHAR(32) NULL,
    ADD COLUMN IF NOT EXISTS communication_style_freeform TEXT NULL;

-- Messages: optional type column (text|audio|system)
ALTER TABLE IF EXISTS messages
    ADD COLUMN IF NOT EXISTS type VARCHAR(16) NULL;


