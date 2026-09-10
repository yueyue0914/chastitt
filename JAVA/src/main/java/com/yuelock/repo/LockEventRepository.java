package com.yuelock.repo;

import com.yuelock.domain.LockEventEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LockEventRepository extends JpaRepository<LockEventEntity, String> {
  List<LockEventEntity> findTop100ByLockIdOrderByCreatedAtDesc(String lockId);
}
