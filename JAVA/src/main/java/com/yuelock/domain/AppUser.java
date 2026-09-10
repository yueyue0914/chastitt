package com.yuelock.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "app_user")
@Getter
@Setter
public class AppUser {
  @Id
  private String id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(nullable = false)
  private String name = "";

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();
}
