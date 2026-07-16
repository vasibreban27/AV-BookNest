package com.avbooknest.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.avbooknest.auth.dto.AuthResponse;
import com.avbooknest.auth.dto.RegisterRequest;
import com.avbooknest.auth.model.RefreshToken;
import com.avbooknest.auth.model.Role;
import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.RefreshTokenRepository;
import com.avbooknest.auth.repository.RoleRepository;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.common.exception.ConflictException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
  @Mock private UserRepository userRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private AuthenticationManager authenticationManager;
  @Mock private UserSecurityService userSecurityService;
  @Mock private JwtService jwtService;
  private AuthService authService;

  @BeforeEach
  void setUp() {
    authService =
        new AuthService(
            userRepository,
            roleRepository,
            refreshTokenRepository,
            passwordEncoder,
            authenticationManager,
            userSecurityService,
            jwtService);
  }

  private void stubTokenIssuance() {
    when(refreshTokenRepository.save(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(userSecurityService.toUserDetails(any(User.class))).thenReturn(userDetails());
    when(jwtService.generateAccessToken(any(UserDetails.class))).thenReturn("access-token");
    when(jwtService.generateRefreshToken(any(UserDetails.class))).thenReturn("refresh-token");
    when(jwtService.extractExpiration(anyString())).thenReturn(Instant.now().plusSeconds(900));
  }

  @Test
  void registerNormalizesEmailPersistsUserAndIssuesTokens() {
    stubTokenIssuance();
    Role role = Role.builder().id(1L).name("USER").build();
    when(userRepository.existsByEmail("ana@example.com")).thenReturn(false);
    when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
    when(passwordEncoder.encode("Secret123")).thenReturn("encoded-password");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    AuthResponse response =
        authService.register(
            new RegisterRequest(" Ana ", " Pop ", " ANA@EXAMPLE.COM ", "Secret123"));

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());
    assertEquals("ana@example.com", userCaptor.getValue().getEmail());
    assertEquals("Ana", userCaptor.getValue().getFirstName());
    assertEquals("encoded-password", userCaptor.getValue().getPasswordHash());
    assertEquals("access-token", response.accessToken());
    assertEquals("refresh-token", response.refreshToken());
    verify(refreshTokenRepository).save(any(RefreshToken.class));
  }

  @Test
  void registerRejectsAnExistingEmailAddress() {
    when(userRepository.existsByEmail("ana@example.com")).thenReturn(true);

    assertThrows(
        ConflictException.class,
        () ->
            authService.register(
                new RegisterRequest("Ana", "Pop", "ana@example.com", "Secret123")));
  }

  @Test
  void refreshRevokesOldTokenAndIssuesNewPair() throws Exception {
    stubTokenIssuance();
    String rawToken = "old-refresh-token";
    User user = user(7L);
    RefreshToken stored =
        RefreshToken.builder()
            .user(user)
            .token(sha256(rawToken))
            .expiresAt(Instant.now().plusSeconds(300))
            .revoked(false)
            .createdAt(Instant.now())
            .build();
    when(jwtService.extractUsername(rawToken)).thenReturn("ana@example.com");
    when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(user));
    when(refreshTokenRepository.findByToken(sha256(rawToken))).thenReturn(Optional.of(stored));

    AuthResponse response = authService.refresh(rawToken);

    assertFalse(response.accessToken().isBlank());
    assertEquals("refresh-token", response.refreshToken());
    assertEquals(true, stored.isRevoked());
    verify(refreshTokenRepository).save(any(RefreshToken.class));
  }

  private User user(Long id) {
    return User.builder()
        .id(id)
        .firstName("Ana")
        .lastName("Pop")
        .email("ana@example.com")
        .passwordHash("encoded-password")
        .role(Role.builder().id(1L).name("USER").build())
        .enabled(true)
        .emailVerified(false)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }

  private UserDetails userDetails() {
    return org.springframework.security.core.userdetails.User.withUsername("ana@example.com")
        .password("encoded-password")
        .authorities("ROLE_USER")
        .build();
  }

  private String sha256(String value) throws Exception {
    return HexFormat.of()
        .formatHex(
            MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
  }
}
