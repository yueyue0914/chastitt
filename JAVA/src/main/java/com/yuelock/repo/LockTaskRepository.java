package com.yuelock.repo;

import com.yuelock.domain.LockTaskEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LockTaskRepository extends JpaRepository<LockTaskEntity, String> {
  List<LockTaskEntity> findTop50ByLockIdOrderByCreatedAtDesc(String lockId);

  Optional<LockTaskEntity> findByIdAndLockId(String id, String lockId);
}
