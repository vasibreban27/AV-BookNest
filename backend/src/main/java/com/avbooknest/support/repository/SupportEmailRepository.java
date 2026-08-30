package com.avbooknest.support.repository;

import com.avbooknest.support.model.SupportEmailDelivery;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SupportEmailRepository extends JpaRepository<SupportEmailDelivery, Long> {
  List<SupportEmailDelivery> findAllByMessageIdIn(List<Long> messageIds);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from SupportEmailDelivery e where e.id = :id")
  Optional<SupportEmailDelivery> findByIdForUpdate(@Param("id") Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from SupportEmailDelivery e where e.message.id = :messageId")
  Optional<SupportEmailDelivery> findByMessageIdForUpdate(@Param("messageId") Long messageId);

  @Query(
      """
      select e.id from SupportEmailDelivery e where e.status in (
        com.avbooknest.support.model.SupportEmailStatus.PENDING,
        com.avbooknest.support.model.SupportEmailStatus.FAILED)
        and e.attempts < 5 and e.nextAttemptAt <= :now order by e.nextAttemptAt, e.id
      """)
  List<Long> findDueIds(@Param("now") Instant now, Pageable pageable);
}
