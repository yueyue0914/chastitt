package com.yuelock.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {
  public record SignUpRequest(
      @Email @NotBlank String email,
      @NotBlank @Size(min = 8, max = 72) String password,
      String name) {}

  public record SignInRequest(
      @Email @NotBlank String email,
      @NotBlank String password) {}

  public record AuthResponse(String token, UserView user) {}

  public record UserView(String id, String email, String name) {}

  public record ProfileView(String userId, String displayName, String role) {}

  public record UpdateProfileRequest(
      @Size(max = 40) String displayName,
      @NotBlank String role) {}
}
