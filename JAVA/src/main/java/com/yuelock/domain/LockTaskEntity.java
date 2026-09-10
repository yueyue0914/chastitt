package com.yuelock.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "lock_tasks")
@Getter
@Setter
public class LockTaskEntity {
  @Id
  private String id;

  @Column(name = "lock_id", nullable = false)
  private String lockId;

  @Column(nullable = false)
  private String title;

  @Column(name = "reward_type", nullable = false)
  private String rewardType;

  @Column(name = "reward_ms", nullable = false)
  private Long rewardMs = 0L;

  @Column(nullable = false)
  private String status = "open";

  @Column(name = "created_at", nullable = false)
  private Long createdAt;

  @Column(name = "completed_at")
  private Long completedAt;
}
