package com.yuelock.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "locks")
@Getter
@Setter
public class LockEntity {
  @Id
  private String id;

  @Column(name = "wearer_token", nullable = false, unique = true)
  private String wearerToken;

  @Column(name = "keyholder_token", nullable = false, unique = true)
  private String keyholderToken;

  @Column(name = "started_at", nullable = false)
  private Long startedAt;

  @Column(name = "duration_ms", nullable = false)
  private Long durationMs;

  @Column(name = "ends_at", nullable = false)
  private Long endsAt;

  @Column(name = "allow_emergency", nullable = false)
  private boolean allowEmergency = true;

  @Column(name = "emergency_limit_mode", nullable = false)
  private String emergencyLimitMode = "cooldown_24h";

  @Column(name = "emergency_penalty_ms", nullable = false)
  private Long emergencyPenaltyMs = 86_400_000L;

  @Column(name = "emergency_last_used_at")
  private Long emergencyLastUsedAt;

  @Column(name = "emergency_use_count", nullable = false)
  private int emergencyUseCount = 0;

  @Column(name = "allow_hygiene", nullable = false)
  private boolean allowHygiene = false;

  @Column(name = "hygiene_max_ms", nullable = false)
  private Long hygieneMaxMs = 900_000L;

  @Column(name = "hygiene_penalty_mode", nullable = false)
  private String hygienePenaltyMode = "multiplier";

  @Column(name = "hygiene_penalty_fixed_ms", nullable = false)
  private Long hygienePenaltyFixedMs = 3_600_000L;

  @Column(name = "hygiene_penalty_multiplier", nullable = false)
  private double hygienePenaltyMultiplier = 2.0;

  @Column(name = "end_phrase", nullable = false)
  private String endPhrase = "";

  @Column(name = "notify_expiry", nullable = false)
  private boolean notifyExpiry = true;

  @Column(name = "hygiene_started_at")
  private Long hygieneStartedAt;

  @Column(name = "frozen_at")
  private Long frozenAt;

  @Column(name = "min_lock_ms", nullable = false)
  private Long minLockMs = 0L;

  @Column(name = "photo_request_active", nullable = false)
  private boolean photoRequestActive = false;

  @Column(name = "photo_submitted_at")
  private Long photoSubmittedAt;

  @Lob
  @Column(name = "photo_thumb")
  private String photoThumb;

  @Column(name = "obedience_enabled", nullable = false)
  private boolean obedienceEnabled = true;

  @Column(name = "obedience_interval_ms", nullable = false)
  private Long obedienceIntervalMs = 1_800_000L;

  @Column(name = "obedience_phrase", nullable = false)
  private String obediencePhrase = "服从主人";

  @Column(name = "last_client_now")
  private Long lastClientNow;

  @Column(name = "integrity_penalty_count", nullable = false)
  private int integrityPenaltyCount = 0;

  @Column(name = "session_nonce", nullable = false)
  private String sessionNonce = "";

  @Column(name = "wearer_user_id")
  private String wearerUserId;

  @Column(name = "keyholder_user_id")
  private String keyholderUserId;

  @Column(nullable = false)
  private String status = "active";

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();
}
