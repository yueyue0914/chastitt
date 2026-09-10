package com.yuelock.web;

import com.yuelock.dto.LockDtos.*;
import com.yuelock.service.LockService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class LockController {
  private final LockService locks;

  public LockController(LockService locks) {
    this.locks = locks;
  }

  @PostMapping("/locks")
  public LockView create(Authentication auth, @RequestBody CreateLockRequest req) {
    return locks.create(auth.getName(), req);
  }

  @GetMapping("/locks/by-wearer/{token}")
  public Object byWearer(@PathVariable String token) {
    LockView v = locks.byWearer(token);
    return v == null ? Map.of() : v;
  }

  @GetMapping("/locks/by-keyholder/{token}")
  public Object byKeyholder(@PathVariable String token) {
    LockView v = locks.byKeyholder(token);
    return v == null ? Map.of() : v;
  }

  @GetMapping("/locks/{token}/events")
  public List<EventView> events(
      @PathVariable String token, @RequestParam(defaultValue = "wearer") String role) {
    return locks.listEvents(token, role);
  }

  @PostMapping("/locks/unlock")
  public LockView unlock(@RequestBody UnlockRequest req) {
    return locks.unlock(req);
  }

  @PostMapping("/locks/hygiene/start")
  public LockView hygieneStart(@RequestBody HygieneRequest req) {
    return locks.startHygiene(req);
  }

  @PostMapping("/locks/hygiene/end")
  public LockView hygieneEnd(@RequestBody HygieneRequest req) {
    return locks.endHygiene(req);
  }

  @PostMapping("/locks/keyholder/add-time")
  public LockView addTime(@RequestBody TimeDeltaRequest req) {
    return locks.addTime(req);
  }

  @PostMapping("/locks/keyholder/sub-time")
  public LockView subTime(@RequestBody TimeDeltaRequest req) {
    return locks.subTime(req);
  }

  @PostMapping("/locks/keyholder/freeze")
  public LockView freeze(@RequestBody FreezeRequest req) {
    return locks.setFreeze(req);
  }

  @PostMapping("/locks/keyholder/min-lock")
  public LockView minLock(@RequestBody MinLockRequest req) {
    return locks.setMinLock(req);
  }

  @PostMapping("/locks/keyholder/photo-request")
  public LockView photoRequest(@RequestBody TokenRequest req) {
    return locks.requestPhoto(req);
  }

  @PostMapping("/locks/wearer/photo")
  public LockView photoSubmit(@RequestBody PhotoSubmitRequest req) {
    return locks.submitPhoto(req);
  }

  @PostMapping("/locks/tasks")
  public TaskView createTask(@RequestBody CreateTaskRequest req) {
    return locks.createTask(req);
  }

  @GetMapping("/locks/tasks")
  public List<TaskView> listTasks(
      @RequestParam String token, @RequestParam(defaultValue = "keyholder") String role) {
    return locks.listTasks(token, role);
  }

  @PostMapping("/locks/tasks/complete")
  public CompleteTaskResponse completeTask(@RequestBody CompleteTaskRequest req) {
    return locks.completeTask(req);
  }

  @PostMapping("/locks/integrity/sync")
  public IntegrityResponse integrity(@RequestBody IntegrityRequest req) {
    return locks.syncIntegrity(req);
  }

  @PostMapping("/locks/claim-keyholder")
  public LockView claim(Authentication auth, @RequestBody TokenRequest req) {
    return locks.claimKeyholder(auth.getName(), req.token());
  }

  @GetMapping("/me/locks/keyholder")
  public List<ManagedLockSummary> myKeyholder(Authentication auth) {
    return locks.listKeyholderLocks(auth.getName());
  }

  @GetMapping("/me/locks/wearer")
  public List<ManagedLockSummary> myWearer(Authentication auth) {
    return locks.listWearerLocks(auth.getName());
  }
}
