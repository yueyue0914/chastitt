package com.yuelock.repo;

import com.yuelock.domain.LockEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LockRepository extends JpaRepository<LockEntity, String> {
  Optional<LockEntity> findByWearerToken(String wearerToken);

  Optional<LockEntity> findByKeyholderToken(String keyholderToken);

  List<LockEntity> findTop40ByWearerUserIdOrderByStartedAtDesc(String wearerUserId);

  List<LockEntity> findTop40ByKeyholderUserIdOrderByStartedAtDesc(String keyholderUserId);

  List<LockEntity> findByStatusAndObedienceChallengeDueAtLessThanEqual(
      String status, long dueAtInclusive);
}
