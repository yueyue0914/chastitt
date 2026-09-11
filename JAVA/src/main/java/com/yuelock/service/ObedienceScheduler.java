package com.yuelock.service;

import com.yuelock.domain.LockEntity;
import com.yuelock.repo.LockRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies obedience timeout penalties even when the wearer never polls
 * (closed browser / backgrounded tab).
 */
@Component
public class ObedienceScheduler {
  private static final Logger log = LoggerFactory.getLogger(ObedienceScheduler.class);

  private final LockRepository locks;
  private final LockService lockService;

  public ObedienceScheduler(LockRepository locks, LockService lockService) {
    this.locks = locks;
    this.lockService = lockService;
  }

  @Scheduled(fixedDelayString = "${yuelock.obedience.settle-ms:15000}")
  @Transactional
  public void settleOverdueChallenges() {
    long now = System.currentTimeMillis();
    List<LockEntity> overdue =
        locks.findByStatusAndObedienceChallengeDueAtLessThanEqual("active", now);
    if (overdue.isEmpty()) return;
    int n = 0;
    for (LockEntity lock : overdue) {
      if (lockService.settleOverdueObedience(lock, now)) {
        n += 1;
      }
    }
    if (n > 0) {
      log.info("Settled {} overdue obedience challenge(s)", n);
    }
  }
}
