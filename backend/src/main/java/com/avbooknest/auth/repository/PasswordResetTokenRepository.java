package com.avbooknest.auth.repository;

import com.avbooknest.auth.model.PasswordResetToken;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select token from PasswordResetToken token where token.token = :token")
  Optional<PasswordResetToken> findByTokenForUpdate(@Param("token") String token);

  List<PasswordResetToken> findAllByUserIdAndUsedAtIsNull(Long userId);
}
