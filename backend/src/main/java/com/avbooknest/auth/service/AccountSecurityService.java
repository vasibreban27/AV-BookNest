package com.avbooknest.auth.service;

import com.avbooknest.auth.model.EmailVerificationToken;
import com.avbooknest.auth.model.PasswordResetToken;
import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.EmailVerificationTokenRepository;
import com.avbooknest.auth.repository.PasswordResetTokenRepository;
import com.avbooknest.auth.repository.RefreshTokenRepository;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.common.exception.BadRequestException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AccountSecurityService {
  private final UserRepository userRepository;
  private final EmailVerificationTokenRepository verificationTokenRepository;
  private final PasswordResetTokenRepository resetTokenRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final SecureTokenService secureTokenService;
  private final AccountEmailService accountEmailService;
  private final PasswordEncoder passwordEncoder;
  private final Duration verificationExpiration;
  private final Duration resetExpiration;

  public AccountSecurityService(
      UserRepository userRepository,
      EmailVerificationTokenRepository verificationTokenRepository,
      PasswordResetTokenRepository resetTokenRepository,
      RefreshTokenRepository refreshTokenRepository,
      SecureTokenService secureTokenService,
      AccountEmailService accountEmailService,
      PasswordEncoder passwordEncoder,
      @Value("${app.auth.email-verification-expiration:24h}") Duration verificationExpiration,
      @Value("${app.auth.password-reset-expiration:30m}") Duration resetExpiration) {
    this.userRepository = userRepository;
    this.verificationTokenRepository = verificationTokenRepository;
    this.resetTokenRepository = resetTokenRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.secureTokenService = secureTokenService;
    this.accountEmailService = accountEmailService;
    this.passwordEncoder = passwordEncoder;
    this.verificationExpiration = verificationExpiration;
    this.resetExpiration = resetExpiration;
  }

  public void sendVerification(User user) {
    if (user.isEmailVerified()) return;
    Instant now = Instant.now();
    verificationTokenRepository
        .findAllByUserIdAndUsedAtIsNull(user.getId())
        .forEach(token -> token.use(now));
    String rawToken = secureTokenService.generate();
    verificationTokenRepository.save(
        new EmailVerificationToken(
            user, secureTokenService.hash(rawToken), now.plus(verificationExpiration), now));
    accountEmailService.sendVerification(user, rawToken);
  }

  public void resendVerification(String email) {
    userRepository
        .findByEmail(normalizeEmail(email))
        .filter(user -> !user.isEmailVerified())
        .ifPresent(this::sendVerification);
  }

  public void verifyEmail(String rawToken) {
    Instant now = Instant.now();
    EmailVerificationToken token =
        verificationTokenRepository
            .findByTokenForUpdate(secureTokenService.hash(rawToken))
            .orElseThrow(() -> invalidToken("verification"));
    requireUsable(token.getUsedAt(), token.getExpiresAt(), now, "verification");
    token.use(now);
    token.getUser().verifyEmail(now);
    verificationTokenRepository
        .findAllByUserIdAndUsedAtIsNull(token.getUser().getId())
        .forEach(other -> other.use(now));
  }

  public void requestPasswordReset(String email) {
    userRepository.findByEmail(normalizeEmail(email)).ifPresent(this::sendPasswordReset);
  }

  public void resetPassword(String rawToken, String newPassword) {
    Instant now = Instant.now();
    PasswordResetToken token =
        resetTokenRepository
            .findByTokenForUpdate(secureTokenService.hash(rawToken))
            .orElseThrow(() -> invalidToken("password reset"));
    requireUsable(token.getUsedAt(), token.getExpiresAt(), now, "password reset");
    User user = token.getUser();
    user.changePassword(passwordEncoder.encode(newPassword), now);
    resetTokenRepository
        .findAllByUserIdAndUsedAtIsNull(user.getId())
        .forEach(other -> other.use(now));
    refreshTokenRepository
        .findAllByUserIdAndRevokedFalse(user.getId())
        .forEach(tokenValue -> tokenValue.revoke());
  }

  private void sendPasswordReset(User user) {
    Instant now = Instant.now();
    resetTokenRepository
        .findAllByUserIdAndUsedAtIsNull(user.getId())
        .forEach(token -> token.use(now));
    String rawToken = secureTokenService.generate();
    resetTokenRepository.save(
        new PasswordResetToken(
            user, secureTokenService.hash(rawToken), now.plus(resetExpiration), now));
    accountEmailService.sendPasswordReset(user, rawToken);
  }

  private void requireUsable(Instant usedAt, Instant expiresAt, Instant now, String purpose) {
    if (usedAt != null || !expiresAt.isAfter(now)) throw invalidToken(purpose);
  }

  private BadRequestException invalidToken(String purpose) {
    return new BadRequestException("The " + purpose + " link is invalid or has expired");
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
