package com.yuelock.service;

import com.yuelock.domain.AppUser;
import com.yuelock.domain.UserProfile;
import com.yuelock.dto.AuthDtos.*;
import com.yuelock.repo.AppUserRepository;
import com.yuelock.repo.UserProfileRepository;
import com.yuelock.security.JwtService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final AppUserRepository users;
  private final UserProfileRepository profiles;
  private final PasswordEncoder encoder;
  private final JwtService jwt;

  public AuthService(
      AppUserRepository users,
      UserProfileRepository profiles,
      PasswordEncoder encoder,
      JwtService jwt) {
    this.users = users;
    this.profiles = profiles;
    this.encoder = encoder;
    this.jwt = jwt;
  }

  @Transactional
  public AuthResponse signUp(SignUpRequest req) {
    String email = req.email().trim().toLowerCase();
    if (users.existsByEmailIgnoreCase(email)) {
      throw new IllegalArgumentException("该邮箱已注册");
    }
    AppUser user = new AppUser();
    user.setId(UUID.randomUUID().toString());
    user.setEmail(email);
    user.setPasswordHash(encoder.encode(req.password()));
    String name =
        req.name() == null || req.name().isBlank()
            ? email.split("@")[0]
            : req.name().trim().substring(0, Math.min(40, req.name().trim().length()));
    user.setName(name);
    users.save(user);

    UserProfile profile = new UserProfile();
    profile.setUserId(user.getId());
    profile.setDisplayName(name);
    profile.setRole("both");
    profiles.save(profile);

    return tokenResponse(user);
  }

  public AuthResponse signIn(SignInRequest req) {
    AppUser user =
        users
            .findByEmailIgnoreCase(req.email().trim())
            .orElseThrow(() -> new IllegalArgumentException("邮箱或密码错误"));
    if (!encoder.matches(req.password(), user.getPasswordHash())) {
      throw new IllegalArgumentException("邮箱或密码错误");
    }
    return tokenResponse(user);
  }

  @Transactional
  public ProfileView getOrCreateProfile(String userId) {
    return profiles
        .findById(userId)
        .map(this::toView)
        .orElseGet(
            () -> {
              UserProfile p = new UserProfile();
              p.setUserId(userId);
              p.setDisplayName("");
              p.setRole("both");
              return toView(profiles.save(p));
            });
  }

  @Transactional
  public ProfileView updateProfile(String userId, UpdateProfileRequest req) {
    String role = req.role();
    if (!role.equals("wearer") && !role.equals("keyholder") && !role.equals("both")) {
      role = "both";
    }
    UserProfile p = profiles.findById(userId).orElseGet(UserProfile::new);
    p.setUserId(userId);
    p.setDisplayName(req.displayName() == null ? "" : req.displayName().trim());
    if (p.getDisplayName().length() > 40) {
      p.setDisplayName(p.getDisplayName().substring(0, 40));
    }
    p.setRole(role);
    p.setUpdatedAt(Instant.now());
    return toView(profiles.save(p));
  }

  private AuthResponse tokenResponse(AppUser user) {
    return new AuthResponse(
        jwt.issue(user.getId(), user.getEmail()),
        new UserView(user.getId(), user.getEmail(), user.getName()));
  }

  private ProfileView toView(UserProfile p) {
    return new ProfileView(p.getUserId(), p.getDisplayName(), p.getRole());
  }
}
