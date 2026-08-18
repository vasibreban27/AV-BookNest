package com.avbooknest.auth.controller;

import com.avbooknest.auth.dto.AuthResponse;
import com.avbooknest.auth.dto.ChangePasswordRequest;
import com.avbooknest.auth.dto.EmailRequest;
import com.avbooknest.auth.dto.LoginRequest;
import com.avbooknest.auth.dto.MessageResponse;
import com.avbooknest.auth.dto.RefreshTokenRequest;
import com.avbooknest.auth.dto.RegisterRequest;
import com.avbooknest.auth.dto.ResetPasswordRequest;
import com.avbooknest.auth.dto.TokenRequest;
import com.avbooknest.auth.dto.UpdateProfileRequest;
import com.avbooknest.auth.dto.UserResponse;
import com.avbooknest.auth.service.AccountSecurityService;
import com.avbooknest.auth.service.AuthRateLimitService;
import com.avbooknest.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;
  private final AccountSecurityService accountSecurityService;
  private final AuthRateLimitService rateLimitService;

  public AuthController(
      AuthService authService,
      AccountSecurityService accountSecurityService,
      AuthRateLimitService rateLimitService) {
    this.authService = authService;
    this.accountSecurityService = accountSecurityService;
    this.rateLimitService = rateLimitService;
  }

  @PostMapping("/register")
  public ResponseEntity<MessageResponse> register(
      @Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
    rateLimitService.checkRegister(httpRequest.getRemoteAddr());
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
  }

  @PostMapping("/login")
  public AuthResponse login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    rateLimitService.checkLogin(httpRequest.getRemoteAddr(), request.email());
    AuthResponse response = authService.login(request);
    rateLimitService.loginSucceeded(request.email());
    return response;
  }

  @PostMapping("/verification-email")
  public MessageResponse resendVerification(
      @Valid @RequestBody EmailRequest request, HttpServletRequest httpRequest) {
    rateLimitService.checkEmailAction(
        httpRequest.getRemoteAddr(), request.email(), "verification-email");
    accountSecurityService.resendVerification(request.email());
    return new MessageResponse(
        "If the account exists and is not verified, a verification email was sent");
  }

  @PostMapping("/verify-email")
  public MessageResponse verifyEmail(@Valid @RequestBody TokenRequest request) {
    accountSecurityService.verifyEmail(request.token());
    return new MessageResponse("Email address verified. You can now sign in");
  }

  @PostMapping("/forgot-password")
  public MessageResponse forgotPassword(
      @Valid @RequestBody EmailRequest request, HttpServletRequest httpRequest) {
    rateLimitService.checkEmailAction(
        httpRequest.getRemoteAddr(), request.email(), "forgot-password");
    accountSecurityService.requestPasswordReset(request.email());
    return new MessageResponse("If the account exists, a password reset email was sent");
  }

  @PostMapping("/reset-password")
  public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    accountSecurityService.resetPassword(request.token(), request.password());
    return new MessageResponse("Password updated. You can now sign in");
  }

  @PostMapping("/refresh")
  public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return authService.refresh(request.refreshToken());
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
    authService.logout(request.refreshToken());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me")
  public UserResponse currentUser(Authentication authentication) {
    return authService.currentUser(authentication.getName());
  }

  @PatchMapping("/me/profile")
  public UserResponse updateProfile(
      @Valid @RequestBody UpdateProfileRequest request, Authentication authentication) {
    return authService.updateProfile(authentication.getName(), request);
  }

  @PatchMapping("/me/password")
  public MessageResponse changePassword(
      @Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
    return authService.changePassword(authentication.getName(), request);
  }
}
