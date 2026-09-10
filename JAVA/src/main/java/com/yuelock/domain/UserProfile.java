package com.yuelock.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_profile")
@Getter
@Setter
public class UserProfile {
  @Id
  @Column(name = "user_id")
  private String userId;

  @Column(name = "display_name", nullable = false)
  private String displayName = "";

  /** wearer | keyholder | both */
  @Column(nullable = false)
  private String role = "both";

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();
}
