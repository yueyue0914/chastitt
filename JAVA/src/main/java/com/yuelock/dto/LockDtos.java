package com.yuelock.dto;

import java.util.List;

public class LockDtos {
  public record CreateLockRequest(
      long durationMs,
      boolean allowEmergency,
      String emergencyLimitMode,
      long emergencyPenaltyMs,
      boolean allowHygiene,
      long hygieneMaxMs,
      String hygienePenaltyMode,
      long hygienePenaltyFixedMs,
      double hygienePenaltyMultiplier,
      String endPhrase,
      boolean notifyExpiry,
      long minLockMs,
      Boolean obedienceEnabled,
      long obedienceIntervalMs,
      String obediencePhrase) {}

  public record TokenRequest(String token) {}

  public record UnlockRequest(String token, String mode, String phrase) {}

  public record HygieneRequest(String token, String role) {}

  public record TimeDeltaRequest(String token, long ms) {}

  public record FreezeRequest(String token, boolean frozen) {}

  public record MinLockRequest(String token, long minLockMs) {}

  public record PhotoSubmitRequest(String token, String thumbDataUrl) {}

  public record CreateTaskRequest(String token, String title, String rewardType, long rewardMs) {}

  public record CompleteTaskRequest(String token, String role, String taskId) {}

  public record IntegrityRequest(
      String token, long clientNow, long localEndsAt, String sessionNonce) {}

  public record LockView(
      String id,
      String wearerToken,
      String keyholderToken,
      long startedAt,
      long durationMs,
      long endsAt,
      boolean allowEmergency,
      String emergencyLimitMode,
      long emergencyPenaltyMs,
      Long emergencyLastUsedAt,
      int emergencyUseCount,
      boolean allowHygiene,
      long hygieneMaxMs,
      String hygienePenaltyMode,
      long hygienePenaltyFixedMs,
      double hygienePenaltyMultiplier,
      String endPhrase,
      boolean notifyExpiry,
      Long hygieneStartedAt,
      Long frozenAt,
      long minLockMs,
      boolean photoRequestActive,
      Long photoSubmittedAt,
      String photoThumb,
      boolean obedienceEnabled,
      long obedienceIntervalMs,
      String obediencePhrase,
      Long lastClientNow,
      int integrityPenaltyCount,
      String sessionNonce,
      String status) {}

  public record EventView(
      String id, String lockId, String kind, long amountMs, String detail, long createdAt) {}

  public record TaskView(
      String id,
      String lockId,
      String title,
      String rewardType,
      long rewardMs,
      String status,
      long createdAt,
      Long completedAt) {}

  public record ManagedLockSummary(
      String id, String keyholderToken, String status, long endsAt, long durationMs) {}

  public record IntegrityResponse(LockView lock, List<String> penalties) {}

  public record CompleteTaskResponse(LockView lock, TaskView task) {}
}
