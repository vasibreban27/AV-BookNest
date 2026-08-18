package com.avbooknest.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.avbooknest.auth.model.EmailVerificationToken;
import com.avbooknest.auth.model.PasswordResetToken;
import com.avbooknest.auth.model.RefreshToken;
import com.avbooknest.auth.model.Role;
import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.EmailVerificationTokenRepository;
import com.avbooknest.auth.repository.PasswordResetTokenRepository;
import com.avbooknest.auth.repository.RefreshTokenRepository;
import com.avbooknest.auth.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AccountSecurityServiceTest {
  @Mock private UserRepository userRepository;
  @Mock private EmailVerificationTokenRepository verificationTokenRepository;
  @Mock private PasswordResetTokenRepository resetTokenRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private SecureTokenService secureTokenService;
  @Mock private AccountEmailService accountEmailService;
  @Mock private PasswordEncoder passwordEncoder;
  private AccountSecurityService service;

  @BeforeEach
  void setUp() {
    service =
        new AccountSecurityService(
            userRepository,
            verificationTokenRepository,
            resetTokenRepository,
            refreshTokenRepository,
            secureTokenService,
            accountEmailService,
            passwordEncoder,
            Duration.ofHours(24),
            Duration.ofMinutes(30));
  }

  @Test
  void verificationTokenIsSingleUseAndMarksEmailVerified() {
    User user = user(false);
    EmailVerificationToken token =
        new EmailVerificationToken(user, "hashed", Instant.now().plusSeconds(300), Instant.now());
    when(secureTokenService.hash("raw-token")).thenReturn("hashed");
    when(verificationTokenRepository.findByTokenForUpdate("hashed")).thenReturn(Optional.of(token));
    when(verificationTokenRepository.findAllByUserIdAndUsedAtIsNull(7L)).thenReturn(List.of(token));

    service.verifyEmail("raw-token");

    assertTrue(user.isEmailVerified());
    assertNotNull(token.getUsedAt());
  }

  @Test
  void passwordResetChangesPasswordConsumesTokensAndRevokesSessions() {
    User user = user(true);
    PasswordResetToken token =
        new PasswordResetToken(user, "hashed", Instant.now().plusSeconds(300), Instant.now());
    RefreshToken refreshToken =
        RefreshToken.builder()
            .user(user)
            .token("refresh-hash")
            .expiresAt(Instant.now().plusSeconds(300))
            .createdAt(Instant.now())
            .build();
    when(secureTokenService.hash("raw-token")).thenReturn("hashed");
    when(resetTokenRepository.findByTokenForUpdate("hashed")).thenReturn(Optional.of(token));
    when(resetTokenRepository.findAllByUserIdAndUsedAtIsNull(7L)).thenReturn(List.of(token));
    when(refreshTokenRepository.findAllByUserIdAndRevokedFalse(7L))
        .thenReturn(List.of(refreshToken));
    when(passwordEncoder.encode("NewSecret123")).thenReturn("new-password-hash");

    service.resetPassword("raw-token", "NewSecret123");

    assertEquals("new-password-hash", user.getPasswordHash());
    assertNotNull(token.getUsedAt());
    assertTrue(refreshToken.isRevoked());
    verify(passwordEncoder).encode("NewSecret123");
  }

  private User user(boolean verified) {
    Instant now = Instant.now();
    return User.builder()
        .id(7L)
        .firstName("Ana")
        .lastName("Pop")
        .email("ana@example.com")
        .passwordHash("old-password-hash")
        .role(Role.builder().id(1L).name("USER").build())
        .enabled(true)
        .emailVerified(verified)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }
}
