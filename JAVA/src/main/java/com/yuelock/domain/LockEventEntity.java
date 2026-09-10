package com.yuelock.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "lock_events")
@Getter
@Setter
public class LockEventEntity {
  @Id
  private String id;

  @Column(name = "lock_id", nullable = false)
  private String lockId;

  @Column(name = "wearer_token", nullable = false)
  private String wearerToken;

  @Column(nullable = false)
  private String kind;

  @Column(name = "amount_ms", nullable = false)
  private Long amountMs = 0L;

  @Column(nullable = false)
  private String detail = "";

  @Column(name = "created_at", nullable = false)
  private Long createdAt;
}
