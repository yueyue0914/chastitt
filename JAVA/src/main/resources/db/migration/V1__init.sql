-- Yue Lock schema (H2 PostgreSQL mode + real Postgres)

CREATE TABLE IF NOT EXISTS app_user (
  id VARCHAR(36) PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  name VARCHAR(80) NOT NULL DEFAULT '',
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_profile (
  user_id VARCHAR(36) PRIMARY KEY,
  display_name VARCHAR(80) NOT NULL DEFAULT '',
  role VARCHAR(20) NOT NULL DEFAULT 'both',
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

CREATE TABLE IF NOT EXISTS locks (
  id VARCHAR(36) PRIMARY KEY,
  wearer_token VARCHAR(64) NOT NULL UNIQUE,
  keyholder_token VARCHAR(64) NOT NULL UNIQUE,
  started_at BIGINT NOT NULL,
  duration_ms BIGINT NOT NULL,
  ends_at BIGINT NOT NULL,
  allow_emergency BOOLEAN NOT NULL DEFAULT TRUE,
  emergency_limit_mode VARCHAR(32) NOT NULL DEFAULT 'cooldown_24h',
  emergency_penalty_ms BIGINT NOT NULL DEFAULT 86400000,
  emergency_last_used_at BIGINT,
  emergency_use_count INT NOT NULL DEFAULT 0,
  allow_hygiene BOOLEAN NOT NULL DEFAULT FALSE,
  hygiene_max_ms BIGINT NOT NULL DEFAULT 900000,
  hygiene_penalty_mode VARCHAR(20) NOT NULL DEFAULT 'multiplier',
  hygiene_penalty_fixed_ms BIGINT NOT NULL DEFAULT 3600000,
  hygiene_penalty_multiplier DOUBLE PRECISION NOT NULL DEFAULT 2,
  end_phrase VARCHAR(200) NOT NULL DEFAULT '',
  notify_expiry BOOLEAN NOT NULL DEFAULT TRUE,
  hygiene_started_at BIGINT,
  frozen_at BIGINT,
  min_lock_ms BIGINT NOT NULL DEFAULT 0,
  photo_request_active BOOLEAN NOT NULL DEFAULT FALSE,
  photo_submitted_at BIGINT,
  photo_thumb CLOB,
  obedience_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  obedience_interval_ms BIGINT NOT NULL DEFAULT 1800000,
  obedience_phrase VARCHAR(200) NOT NULL DEFAULT '服从主人',
  last_client_now BIGINT,
  integrity_penalty_count INT NOT NULL DEFAULT 0,
  session_nonce VARCHAR(64) NOT NULL DEFAULT '',
  wearer_user_id VARCHAR(36),
  keyholder_user_id VARCHAR(36),
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_locks_wearer_user ON locks(wearer_user_id);
CREATE INDEX IF NOT EXISTS idx_locks_keyholder_user ON locks(keyholder_user_id);
CREATE INDEX IF NOT EXISTS idx_locks_status ON locks(status);

CREATE TABLE IF NOT EXISTS lock_events (
  id VARCHAR(36) PRIMARY KEY,
  lock_id VARCHAR(36) NOT NULL,
  wearer_token VARCHAR(64) NOT NULL,
  kind VARCHAR(40) NOT NULL,
  amount_ms BIGINT NOT NULL DEFAULT 0,
  detail VARCHAR(500) NOT NULL DEFAULT '',
  created_at BIGINT NOT NULL,
  CONSTRAINT fk_event_lock FOREIGN KEY (lock_id) REFERENCES locks(id)
);

CREATE INDEX IF NOT EXISTS idx_events_lock ON lock_events(lock_id);

CREATE TABLE IF NOT EXISTS lock_tasks (
  id VARCHAR(36) PRIMARY KEY,
  lock_id VARCHAR(36) NOT NULL,
  title VARCHAR(120) NOT NULL,
  reward_type VARCHAR(20) NOT NULL,
  reward_ms BIGINT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'open',
  created_at BIGINT NOT NULL,
  completed_at BIGINT,
  CONSTRAINT fk_task_lock FOREIGN KEY (lock_id) REFERENCES locks(id)
);

CREATE INDEX IF NOT EXISTS idx_tasks_lock ON lock_tasks(lock_id);
