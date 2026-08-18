package com.avbooknest.auth.repository;

import com.avbooknest.auth.model.EmailVerificationToken;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailVerificationTokenRepository
    extends JpaRepository<EmailVerificationToken, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select token from EmailVerificationToken token where token.token = :token")
  Optional<EmailVerificationToken> findByTokenForUpdate(@Param("token") String token);

  List<EmailVerificationToken> findAllByUserIdAndUsedAtIsNull(Long userId);
}
