-- Phrase fail tracking: wrong end-phrase attempts → configurable penalty.

ALTER TABLE locks ADD COLUMN phrase_fail_count INT NOT NULL DEFAULT 0;
ALTER TABLE locks ADD COLUMN phrase_max_fails INT NOT NULL DEFAULT 3;
ALTER TABLE locks ADD COLUMN phrase_fail_penalty_ms BIGINT NOT NULL DEFAULT 3600000;
