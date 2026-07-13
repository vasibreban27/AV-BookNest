package com.avbooknest.auth.service;

import com.avbooknest.auth.dto.AuthResponse;
import com.avbooknest.auth.dto.LoginRequest;
import com.avbooknest.auth.dto.RegisterRequest;
import com.avbooknest.auth.dto.UserResponse;
import com.avbooknest.auth.model.RefreshToken;
import com.avbooknest.auth.model.Role;
import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.RefreshTokenRepository;
import com.avbooknest.auth.repository.RoleRepository;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.common.exception.ConflictException;
import com.avbooknest.common.exception.UnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

  private static final String USER_ROLE = "USER";

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final UserSecurityService userSecurityService;
  private final JwtService jwtService;

  public AuthService(
      UserRepository userRepository,
      RoleRepository roleRepository,
      RefreshTokenRepository refreshTokenRepository,
      PasswordEncoder passwordEncoder,
      AuthenticationManager authenticationManager,
      UserSecurityService userSecurityService,
      JwtService jwtService) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.authenticationManager = authenticationManager;
    this.userSecurityService = userSecurityService;
    this.jwtService = jwtService;
  }

  public AuthResponse register(RegisterRequest request) {
    String email = normalizeEmail(request.email());
    if (userRepository.existsByEmail(email)) {
      throw new ConflictException("An account already exists for this email address");
    }

    Role userRole =
        roleRepository
            .findByName(USER_ROLE)
            .orElseThrow(() -> new IllegalStateException("The default USER role is missing"));
    Instant now = Instant.now();
    User user =
        User.builder()
            .firstName(request.firstName().trim())
            .lastName(request.lastName().trim())
            .email(email)
            .passwordHash(passwordEncoder.encode(request.password()))
            .role(userRole)
            .emailVerified(false)
            .enabled(true)
            .createdAt(now)
            .updatedAt(now)
            .build();

    return issueTokens(userRepository.save(user));
  }

  public AuthResponse login(LoginRequest request) {
    String email = normalizeEmail(request.email());
    try {
      authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(email, request.password()));
    } catch (AuthenticationException exception) {
      throw new UnauthorizedException("Invalid email or password");
    }

    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
    return issueTokens(user);
  }

  public AuthResponse refresh(String rawRefreshToken) {
    jwtService.requireRefreshToken(rawRefreshToken);
    String email = jwtService.extractUsername(rawRefreshToken);
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

    RefreshToken storedToken =
        refreshTokenRepository
            .findByToken(hashToken(rawRefreshToken))
            .filter(token -> !token.isRevoked())
            .filter(token -> token.getExpiresAt().isAfter(Instant.now()))
            .filter(token -> token.getUser().getId().equals(user.getId()))
            .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

    storedToken.revoke();
    return issueTokens(user);
  }

  public void logout(String rawRefreshToken) {
    refreshTokenRepository.findByToken(hashToken(rawRefreshToken)).ifPresent(RefreshToken::revoke);
  }

  @Transactional(readOnly = true)
  public UserResponse currentUser(String email) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UnauthorizedException("User not found"));
    return UserResponse.from(user);
  }

  private AuthResponse issueTokens(User user) {
    UserDetails userDetails = userSecurityService.toUserDetails(user);
    String accessToken = jwtService.generateAccessToken(userDetails);
    String refreshToken = jwtService.generateRefreshToken(userDetails);

    refreshTokenRepository.save(
        RefreshToken.builder()
            .user(user)
            .token(hashToken(refreshToken))
            .expiresAt(jwtService.extractExpiration(refreshToken))
            .revoked(false)
            .createdAt(Instant.now())
            .build());

    return new AuthResponse(
        accessToken,
        refreshToken,
        "Bearer",
        jwtService.extractExpiration(accessToken).getEpochSecond() - Instant.now().getEpochSecond(),
        UserResponse.from(user));
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private String hashToken(String token) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available", exception);
    }
  }
}
