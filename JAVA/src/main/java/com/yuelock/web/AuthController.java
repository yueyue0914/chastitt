package com.yuelock.web;

import com.yuelock.dto.AuthDtos.*;
import com.yuelock.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {
  private final AuthService auth;

  public AuthController(AuthService auth) {
    this.auth = auth;
  }

  @PostMapping("/auth/sign-up")
  public AuthResponse signUp(@Valid @RequestBody SignUpRequest req) {
    return auth.signUp(req);
  }

  @PostMapping("/auth/sign-in")
  public AuthResponse signIn(@Valid @RequestBody SignInRequest req) {
    return auth.signIn(req);
  }

  @GetMapping("/auth/me")
  public UserView me(Authentication authentication) {
    // lightweight: id from JWT subject; email/name via profile path if needed
    return new UserView(authentication.getName(), "", "");
  }

  @GetMapping("/me/profile")
  public ProfileView profile(Authentication authentication) {
    return auth.getOrCreateProfile(authentication.getName());
  }

  @PutMapping("/me/profile")
  public ProfileView updateProfile(
      Authentication authentication, @Valid @RequestBody UpdateProfileRequest req) {
    return auth.updateProfile(authentication.getName(), req);
  }
}
