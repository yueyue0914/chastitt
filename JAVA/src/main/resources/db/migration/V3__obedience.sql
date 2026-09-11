-- Server-driven obedience challenges with timeout penalties.

ALTER TABLE locks ADD COLUMN obedience_timeout_ms BIGINT NOT NULL DEFAULT 120000;
ALTER TABLE locks ADD COLUMN obedience_penalty_ms BIGINT NOT NULL DEFAULT 3600000;
ALTER TABLE locks ADD COLUMN obedience_success_count INT NOT NULL DEFAULT 0;
ALTER TABLE locks ADD COLUMN obedience_fail_count INT NOT NULL DEFAULT 0;
ALTER TABLE locks ADD COLUMN obedience_last_completed_at BIGINT;
ALTER TABLE locks ADD COLUMN obedience_challenge_due_at BIGINT;
