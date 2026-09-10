package com.yuelock.service;

import com.yuelock.domain.LockEntity;
import com.yuelock.domain.LockEventEntity;
import com.yuelock.domain.LockTaskEntity;
import com.yuelock.dto.LockDtos.*;
import com.yuelock.repo.LockEventRepository;
import com.yuelock.repo.LockRepository;
import com.yuelock.repo.LockTaskRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LockService {
  private static final long MIN_MS = 60_000L;
  private static final long MAX_DURATION = 365L * 24 * 60 * 60_000;
  private static final long DAY = 24L * 60 * 60_000;
  private static final long INTEGRITY_PENALTY = 60L * 60_000;
  private static final String DEFAULT_END = "我是主人的无面锁屌latex性偶";
  private static final String DEFAULT_OBEDIENCE = "服从主人";

  private final LockRepository locks;
  private final LockEventRepository events;
  private final LockTaskRepository tasks;
  private final SecureRandom random = new SecureRandom();

  public LockService(
      LockRepository locks, LockEventRepository events, LockTaskRepository tasks) {
    this.locks = locks;
    this.events = events;
    this.tasks = tasks;
  }

  @Transactional
  public LockView create(String userId, CreateLockRequest req) {
    long now = System.currentTimeMillis();
    long duration = clamp(req.durationMs(), MIN_MS, MAX_DURATION);
    String endPhrase = normalizePhrase(req.endPhrase());
    if (endPhrase.length() < 4) endPhrase = DEFAULT_END;

    LockEntity lock = new LockEntity();
    lock.setId(UUID.randomUUID().toString());
    lock.setWearerToken(randomToken());
    lock.setKeyholderToken(randomToken());
    lock.setStartedAt(now);
    lock.setDurationMs(duration);
    lock.setEndsAt(now + duration);
    lock.setAllowEmergency(req.allowEmergency());
    lock.setEmergencyLimitMode(normalizeEmergencyMode(req.emergencyLimitMode()));
    lock.setEmergencyPenaltyMs(clamp(req.emergencyPenaltyMs(), MIN_MS, MAX_DURATION));
    lock.setAllowHygiene(req.allowHygiene());
    lock.setHygieneMaxMs(
        req.allowHygiene()
            ? clamp(req.hygieneMaxMs() <= 0 ? 900_000 : req.hygieneMaxMs(), MIN_MS, 2 * 60 * 60_000)
            : 900_000);
    lock.setHygienePenaltyMode("fixed".equals(req.hygienePenaltyMode()) ? "fixed" : "multiplier");
    lock.setHygienePenaltyFixedMs(
        clamp(req.hygienePenaltyFixedMs() <= 0 ? 3_600_000 : req.hygienePenaltyFixedMs(), MIN_MS, 30 * DAY));
    lock.setHygienePenaltyMultiplier(
        Math.max(0.5, Math.min(10, req.hygienePenaltyMultiplier() <= 0 ? 2 : req.hygienePenaltyMultiplier())));
    lock.setEndPhrase(endPhrase);
    lock.setNotifyExpiry(req.notifyExpiry());
    lock.setMinLockMs(clamp(Math.max(0, req.minLockMs()), 0, Math.max(duration, MAX_DURATION)));
    lock.setObedienceEnabled(req.obedienceEnabled() == null || req.obedienceEnabled());
    lock.setObedienceIntervalMs(
        clamp(req.obedienceIntervalMs() <= 0 ? 1_800_000 : req.obedienceIntervalMs(), MIN_MS, DAY));
    String obPhrase = normalizePhrase(req.obediencePhrase());
    lock.setObediencePhrase(obPhrase.length() < 2 ? DEFAULT_OBEDIENCE : obPhrase);
    lock.setSessionNonce(randomToken());
    lock.setWearerUserId(userId);
    lock.setStatus("active");
    lock.setUpdatedAt(Instant.now());
    locks.save(lock);
    appendEvent(lock, "started", duration, "锁定开始");
    return toView(lock);
  }

  public LockView byWearer(String token) {
    return locks.findByWearerToken(requireToken(token)).map(this::toView).orElse(null);
  }

  public LockView byKeyholder(String token) {
    return locks.findByKeyholderToken(requireToken(token)).map(this::toView).orElse(null);
  }

  public List<EventView> listEvents(String token, String role) {
    LockEntity lock = resolveByRole(token, role);
    return events.findTop100ByLockIdOrderByCreatedAtDesc(lock.getId()).stream()
        .map(
            e ->
                new EventView(
                    e.getId(),
                    e.getLockId(),
                    e.getKind(),
                    e.getAmountMs(),
                    e.getDetail(),
                    e.getCreatedAt()))
        .toList();
  }

  @Transactional
  public LockView unlock(UnlockRequest req) {
    String mode = req.mode() == null ? "" : req.mode();
    long now = System.currentTimeMillis();
    if ("keyholder".equals(mode)) {
      LockEntity lock = requireActiveKeyholder(req.token());
      finish(lock, "ended", "keyholder_unlock", 0, "钥匙开锁");
      return toView(lock);
    }
    if ("emergency".equals(mode)) {
      LockEntity lock = requireActiveWearer(req.token());
      if (!lock.isAllowEmergency()) throw new IllegalArgumentException("未开启紧急解锁");
      requirePhrase(lock, req.phrase());
      assertEmergencyAllowed(lock, now);
      lock.setEmergencyUseCount(lock.getEmergencyUseCount() + 1);
      lock.setEmergencyLastUsedAt(now);
      clearTransient(lock);
      lock.setStatus("emergency_ended");
      lock.setUpdatedAt(Instant.now());
      appendEvent(lock, "emergency", 0, "紧急解锁");
      if ("once_penalty".equals(lock.getEmergencyLimitMode())) {
        appendEvent(lock, "emergency_penalty", lock.getEmergencyPenaltyMs(), "紧急永久惩罚");
      }
      locks.save(lock);
      return toView(lock);
    }
    if ("expiry".equals(mode)) {
      LockEntity lock = requireActiveWearer(req.token());
      requirePhrase(lock, req.phrase());
      if (!canWearerEnd(lock, now)) throw new IllegalArgumentException("尚未满足结束条件");
      finish(lock, "ended", "ended", 0, "到期结束");
      return toView(lock);
    }
    throw new IllegalArgumentException("未知解锁模式");
  }

  @Transactional
  public LockView startHygiene(HygieneRequest req) {
    long now = System.currentTimeMillis();
    LockEntity lock;
    if ("keyholder".equals(req.role())) {
      lock = requireActiveKeyholder(req.token());
      lock.setAllowHygiene(true);
      appendEvent(lock, "force_hygiene", 0, "强制清洁");
    } else {
      lock = requireActiveWearer(req.token());
      if (!lock.isAllowHygiene()) throw new IllegalArgumentException("未开启清洁");
    }
    if (lock.getFrozenAt() != null) throw new IllegalArgumentException("冻结中不可清洁");
    if (lock.getHygieneStartedAt() != null) throw new IllegalArgumentException("清洁已在进行");
    lock.setHygieneStartedAt(now);
    lock.setUpdatedAt(Instant.now());
    return toView(locks.save(lock));
  }

  @Transactional
  public LockView endHygiene(HygieneRequest req) {
    LockEntity lock = resolveActiveByRole(req.token(), req.role());
    if (lock.getHygieneStartedAt() == null) throw new IllegalArgumentException("当前没有清洁");
    long now = System.currentTimeMillis();
    long overtime =
        Math.max(0, now - lock.getHygieneStartedAt() - lock.getHygieneMaxMs());
    long penalty = 0;
    if (overtime > 0) {
      penalty =
          "fixed".equals(lock.getHygienePenaltyMode())
              ? lock.getHygienePenaltyFixedMs()
              : Math.round(overtime * lock.getHygienePenaltyMultiplier());
      applyTimeDelta(lock, penalty, now);
      appendEvent(lock, "hygiene_penalty", penalty, "清洁超时惩罚");
    }
    lock.setHygieneStartedAt(null);
    lock.setUpdatedAt(Instant.now());
    return toView(locks.save(lock));
  }

  @Transactional
  public LockView addTime(TimeDeltaRequest req) {
    LockEntity lock = requireActiveKeyholder(req.token());
    long add = clamp(req.ms(), 1, 30 * DAY);
    applyTimeDelta(lock, add, System.currentTimeMillis());
    appendEvent(lock, "keyholder_add_time", add, "钥匙加时");
    return toView(locks.save(lock));
  }

  @Transactional
  public LockView subTime(TimeDeltaRequest req) {
    LockEntity lock = requireActiveKeyholder(req.token());
    long sub = clamp(req.ms(), 1, 30 * DAY);
    applyTimeDelta(lock, -sub, System.currentTimeMillis());
    appendEvent(lock, "keyholder_sub_time", sub, "钥匙减时");
    return toView(locks.save(lock));
  }

  @Transactional
  public LockView setFreeze(FreezeRequest req) {
    LockEntity lock = requireActiveKeyholder(req.token());
    long now = System.currentTimeMillis();
    if (req.frozen()) {
      if (lock.getFrozenAt() != null) return toView(lock);
      lock.setFrozenAt(now);
      appendEvent(lock, "freeze", 0, "冻结");
    } else {
      if (lock.getFrozenAt() == null) return toView(lock);
      long paused = Math.max(0, now - lock.getFrozenAt());
      applyTimeDelta(lock, paused, now);
      lock.setFrozenAt(null);
      appendEvent(lock, "unfreeze", paused, "解冻");
    }
    lock.setUpdatedAt(Instant.now());
    return toView(locks.save(lock));
  }

  @Transactional
  public LockView setMinLock(MinLockRequest req) {
    LockEntity lock = requireActiveKeyholder(req.token());
    lock.setMinLockMs(clamp(req.minLockMs(), 0, MAX_DURATION));
    lock.setUpdatedAt(Instant.now());
    appendEvent(lock, "min_lock_set", lock.getMinLockMs(), "最低锁定");
    return toView(locks.save(lock));
  }

  @Transactional
  public LockView requestPhoto(TokenRequest req) {
    LockEntity lock = requireActiveKeyholder(req.token());
    lock.setPhotoRequestActive(true);
    lock.setPhotoSubmittedAt(null);
    lock.setPhotoThumb(null);
    lock.setUpdatedAt(Instant.now());
    appendEvent(lock, "photo_request", 0, "要求拍照");
    return toView(locks.save(lock));
  }

  @Transactional
  public LockView submitPhoto(PhotoSubmitRequest req) {
    LockEntity lock = requireActiveWearer(req.token());
    if (!lock.isPhotoRequestActive()) throw new IllegalArgumentException("当前没有拍照请求");
    String thumb = req.thumbDataUrl() == null ? "" : req.thumbDataUrl();
    if (!thumb.startsWith("data:image/") || thumb.length() > 120_000) {
      throw new IllegalArgumentException("图片无效或过大");
    }
    lock.setPhotoThumb(thumb);
    lock.setPhotoSubmittedAt(System.currentTimeMillis());
    lock.setPhotoRequestActive(false);
    lock.setUpdatedAt(Instant.now());
    appendEvent(lock, "photo_submit", 0, "提交拍照");
    return toView(locks.save(lock));
  }

  @Transactional
  public TaskView createTask(CreateTaskRequest req) {
    LockEntity lock = requireActiveKeyholder(req.token());
    String title = req.title() == null ? "" : req.title().trim();
    if (title.length() < 2 || title.length() > 120) {
      throw new IllegalArgumentException("任务标题长度需 2–120");
    }
    String type = "unlock".equals(req.rewardType()) ? "unlock" : "reduce";
    LockTaskEntity task = new LockTaskEntity();
    task.setId(UUID.randomUUID().toString());
    task.setLockId(lock.getId());
    task.setTitle(title);
    task.setRewardType(type);
    task.setRewardMs(type.equals("reduce") ? clamp(req.rewardMs(), MIN_MS, 30 * DAY) : 0);
    task.setStatus("open");
    task.setCreatedAt(System.currentTimeMillis());
    tasks.save(task);
    appendEvent(lock, "task_created", task.getRewardMs(), "发布任务: " + title);
    locks.save(lock);
    return toTaskView(task);
  }

  public List<TaskView> listTasks(String token, String role) {
    LockEntity lock = resolveByRole(token, role);
    return tasks.findTop50ByLockIdOrderByCreatedAtDesc(lock.getId()).stream()
        .map(this::toTaskView)
        .toList();
  }

  @Transactional
  public CompleteTaskResponse completeTask(CompleteTaskRequest req) {
    LockEntity lock = resolveActiveByRole(req.token(), req.role());
    LockTaskEntity task =
        tasks
            .findByIdAndLockId(req.taskId(), lock.getId())
            .orElseThrow(() -> new IllegalArgumentException("任务不存在"));
    if (!"open".equals(task.getStatus())) throw new IllegalArgumentException("任务已完成");
    long now = System.currentTimeMillis();
    task.setStatus("done");
    task.setCompletedAt(now);
    tasks.save(task);
    appendEvent(lock, "task_done", task.getRewardMs(), "完成任务: " + task.getTitle());
    if ("unlock".equals(task.getRewardType())) {
      finish(lock, "ended", "ended", 0, "任务开锁");
    } else {
      applyTimeDelta(lock, -task.getRewardMs(), now);
      locks.save(lock);
    }
    return new CompleteTaskResponse(toView(lock), toTaskView(task));
  }

  @Transactional
  public IntegrityResponse syncIntegrity(IntegrityRequest req) {
    LockEntity lock = requireActiveWearer(req.token());
    long now = System.currentTimeMillis();
    List<String> penalties = new ArrayList<>();
    Long last = lock.getLastClientNow();
    if (last != null && req.clientNow() < last - 30_000) {
      applyTimeDelta(lock, INTEGRITY_PENALTY, now);
      lock.setIntegrityPenaltyCount(lock.getIntegrityPenaltyCount() + 1);
      penalties.add("时间回拨");
      appendEvent(lock, "integrity_penalty", INTEGRITY_PENALTY, "时间回拨");
    }
    if (req.localEndsAt() + 120_000 < lock.getEndsAt()) {
      applyTimeDelta(lock, INTEGRITY_PENALTY, now);
      lock.setIntegrityPenaltyCount(lock.getIntegrityPenaltyCount() + 1);
      penalties.add("本地篡改");
      appendEvent(lock, "integrity_penalty", INTEGRITY_PENALTY, "本地篡改");
    }
    if (req.sessionNonce() != null
        && !req.sessionNonce().isBlank()
        && !lock.getSessionNonce().isBlank()
        && !req.sessionNonce().equals(lock.getSessionNonce())) {
      applyTimeDelta(lock, INTEGRITY_PENALTY, now);
      lock.setIntegrityPenaltyCount(lock.getIntegrityPenaltyCount() + 1);
      penalties.add("nonce 异常");
      appendEvent(lock, "integrity_penalty", INTEGRITY_PENALTY, "nonce 异常");
    }
    long client = Math.max(0, Math.min(req.clientNow(), now + 60_000));
    lock.setLastClientNow(client);
    lock.setUpdatedAt(Instant.now());
    locks.save(lock);
    return new IntegrityResponse(toView(lock), penalties);
  }

  @Transactional
  public LockView claimKeyholder(String userId, String token) {
    LockEntity lock = requireActiveKeyholder(token);
    String existing = lock.getKeyholderUserId();
    if (existing != null && !existing.equals(userId)) {
      throw new IllegalStateException("这把钥匙已被其他账号认领");
    }
    lock.setKeyholderUserId(userId);
    lock.setUpdatedAt(Instant.now());
    appendEvent(lock, "keyholder_claim", 0, "钥匙账号已绑定");
    return toView(locks.save(lock));
  }

  public List<ManagedLockSummary> listKeyholderLocks(String userId) {
    return locks.findTop40ByKeyholderUserIdOrderByStartedAtDesc(userId).stream()
        .map(
            l ->
                new ManagedLockSummary(
                    l.getId(), l.getKeyholderToken(), l.getStatus(), l.getEndsAt(), l.getDurationMs()))
        .toList();
  }

  public List<ManagedLockSummary> listWearerLocks(String userId) {
    return locks.findTop40ByWearerUserIdOrderByStartedAtDesc(userId).stream()
        .map(
            l ->
                new ManagedLockSummary(
                    l.getId(), l.getKeyholderToken(), l.getStatus(), l.getEndsAt(), l.getDurationMs()))
        .toList();
  }

  // ---- helpers ----

  private void finish(LockEntity lock, String status, String kind, long amount, String detail) {
    clearTransient(lock);
    lock.setStatus(status);
    lock.setUpdatedAt(Instant.now());
    appendEvent(lock, kind, amount, detail);
    locks.save(lock);
  }

  private void clearTransient(LockEntity lock) {
    lock.setHygieneStartedAt(null);
    lock.setFrozenAt(null);
    lock.setPhotoRequestActive(false);
  }

  private void applyTimeDelta(LockEntity lock, long deltaMs, long now) {
    long newEnds = Math.max(now + MIN_MS, lock.getEndsAt() + deltaMs);
    long diff = newEnds - lock.getEndsAt();
    lock.setEndsAt(newEnds);
    lock.setDurationMs(Math.max(MIN_MS, lock.getDurationMs() + diff));
    lock.setUpdatedAt(Instant.now());
  }

  private boolean canWearerEnd(LockEntity lock, long now) {
    if (lock.getFrozenAt() != null) return false;
    long effective = lock.getFrozenAt() != null ? lock.getFrozenAt() : now;
    if (effective < lock.getEndsAt()) return false;
    return now >= lock.getStartedAt() + lock.getMinLockMs();
  }

  private void assertEmergencyAllowed(LockEntity lock, long now) {
    String mode = lock.getEmergencyLimitMode();
    if ("unlimited".equals(mode)) return;
    if ("once_penalty".equals(mode)) {
      if (lock.getEmergencyUseCount() >= 1) throw new IllegalArgumentException("紧急解锁已用完");
      return;
    }
    // cooldown_24h
    if (lock.getEmergencyLastUsedAt() != null && now - lock.getEmergencyLastUsedAt() < DAY) {
      throw new IllegalArgumentException("紧急解锁冷却中（24小时）");
    }
  }

  private void requirePhrase(LockEntity lock, String phrase) {
    String a = normalizePhrase(lock.getEndPhrase());
    String b = normalizePhrase(phrase == null ? "" : phrase);
    if (!a.equals(b)) throw new IllegalArgumentException("结束宣言不正确");
  }

  private LockEntity requireActiveWearer(String token) {
    LockEntity lock =
        locks
            .findByWearerToken(requireToken(token))
            .orElseThrow(() -> new IllegalArgumentException("锁定不存在"));
    if (!"active".equals(lock.getStatus())) throw new IllegalArgumentException("锁定已结束");
    return lock;
  }

  private LockEntity requireActiveKeyholder(String token) {
    LockEntity lock =
        locks
            .findByKeyholderToken(requireToken(token))
            .orElseThrow(() -> new IllegalArgumentException("锁定不存在"));
    if (!"active".equals(lock.getStatus())) throw new IllegalArgumentException("锁定已结束");
    return lock;
  }

  private LockEntity resolveActiveByRole(String token, String role) {
    return "keyholder".equals(role) ? requireActiveKeyholder(token) : requireActiveWearer(token);
  }

  private LockEntity resolveByRole(String token, String role) {
    if ("keyholder".equals(role)) {
      return locks
          .findByKeyholderToken(requireToken(token))
          .orElseThrow(() -> new IllegalArgumentException("锁定不存在"));
    }
    return locks
        .findByWearerToken(requireToken(token))
        .orElseThrow(() -> new IllegalArgumentException("锁定不存在"));
  }

  private void appendEvent(LockEntity lock, String kind, long amount, String detail) {
    LockEventEntity e = new LockEventEntity();
    e.setId(UUID.randomUUID().toString());
    e.setLockId(lock.getId());
    e.setWearerToken(lock.getWearerToken());
    e.setKind(kind);
    e.setAmountMs(amount);
    e.setDetail(detail == null ? "" : detail);
    e.setCreatedAt(System.currentTimeMillis());
    events.save(e);
  }

  private String randomToken() {
    byte[] buf = new byte[12];
    random.nextBytes(buf);
    return HexFormat.of().formatHex(buf);
  }

  private String requireToken(String token) {
    if (token == null) throw new IllegalArgumentException("缺少令牌");
    String t = token.trim();
    if (t.length() < 8 || t.length() > 64) throw new IllegalArgumentException("令牌无效");
    return t;
  }

  private static long clamp(long v, long min, long max) {
    return Math.max(min, Math.min(max, v));
  }

  private static String normalizePhrase(String s) {
    return s == null ? "" : s.stripTrailing();
  }

  private static String normalizeEmergencyMode(String mode) {
    if ("unlimited".equals(mode) || "once_penalty".equals(mode)) return mode;
    return "cooldown_24h";
  }

  private LockView toView(LockEntity l) {
    return new LockView(
        l.getId(),
        l.getWearerToken(),
        l.getKeyholderToken(),
        l.getStartedAt(),
        l.getDurationMs(),
        l.getEndsAt(),
        l.isAllowEmergency(),
        l.getEmergencyLimitMode(),
        l.getEmergencyPenaltyMs(),
        l.getEmergencyLastUsedAt(),
        l.getEmergencyUseCount(),
        l.isAllowHygiene(),
        l.getHygieneMaxMs(),
        l.getHygienePenaltyMode(),
        l.getHygienePenaltyFixedMs(),
        l.getHygienePenaltyMultiplier(),
        l.getEndPhrase(),
        l.isNotifyExpiry(),
        l.getHygieneStartedAt(),
        l.getFrozenAt(),
        l.getMinLockMs(),
        l.isPhotoRequestActive(),
        l.getPhotoSubmittedAt(),
        l.getPhotoThumb(),
        l.isObedienceEnabled(),
        l.getObedienceIntervalMs(),
        l.getObediencePhrase(),
        l.getLastClientNow(),
        l.getIntegrityPenaltyCount(),
        l.getSessionNonce(),
        l.getStatus());
  }

  private TaskView toTaskView(LockTaskEntity t) {
    return new TaskView(
        t.getId(),
        t.getLockId(),
        t.getTitle(),
        t.getRewardType(),
        t.getRewardMs(),
        t.getStatus(),
        t.getCreatedAt(),
        t.getCompletedAt());
  }
}
