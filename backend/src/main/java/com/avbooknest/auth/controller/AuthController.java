package com.avbooknest.auth.controller;

import com.avbooknest.auth.dto.AuthResponse;
import com.avbooknest.auth.dto.ChangePasswordRequest;
import com.avbooknest.auth.dto.CsrfResponse;
import com.avbooknest.auth.dto.EmailRequest;
import com.avbooknest.auth.dto.LoginRequest;
import com.avbooknest.auth.dto.MessageResponse;
import com.avbooknest.auth.dto.RegisterRequest;
import com.avbooknest.auth.dto.ResetPasswordRequest;
import com.avbooknest.auth.dto.TokenRequest;
import com.avbooknest.auth.dto.UpdateProfileRequest;
import com.avbooknest.auth.dto.UserResponse;
import com.avbooknest.auth.service.AccountSecurityService;
import com.avbooknest.auth.service.AuthCookieService;
import com.avbooknest.auth.service.AuthRateLimitService;
import com.avbooknest.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
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
  private final AuthCookieService authCookieService;

  public AuthController(
      AuthService authService,
      AccountSecurityService accountSecurityService,
      AuthRateLimitService rateLimitService,
      AuthCookieService authCookieService) {
    this.authService = authService;
    this.accountSecurityService = accountSecurityService;
    this.rateLimitService = rateLimitService;
    this.authCookieService = authCookieService;
  }

  @PostMapping("/register")
  public ResponseEntity<MessageResponse> register(
      @Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
    rateLimitService.checkRegister(httpRequest.getRemoteAddr());
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
  }

  @PostMapping("/login")
  public AuthResponse login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    rateLimitService.checkLogin(httpRequest.getRemoteAddr(), request.email());
    AuthResponse response = authService.login(request);
    rateLimitService.loginSucceeded(request.email());
    authCookieService.write(httpResponse, response);
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
  public MessageResponse resetPassword(
      @Valid @RequestBody ResetPasswordRequest request, HttpServletResponse response) {
    accountSecurityService.resetPassword(request.token(), request.password());
    authCookieService.clear(response);
    return new MessageResponse("Password updated. You can now sign in");
  }

  @PostMapping("/refresh")
  public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
    try {
      AuthResponse authentication =
          authService.refresh(authCookieService.requireRefreshToken(request));
      authCookieService.write(response, authentication);
      return authentication;
    } catch (RuntimeException exception) {
      authCookieService.clear(response);
      throw exception;
    }
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
    authCookieService.refreshToken(request).ifPresent(authService::logout);
    authCookieService.clear(response);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/csrf")
  public CsrfResponse csrf(CsrfToken csrfToken) {
    csrfToken.getToken();
    return new CsrfResponse(csrfToken.getHeaderName());
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
      @Valid @RequestBody ChangePasswordRequest request,
      Authentication authentication,
      HttpServletResponse response) {
    MessageResponse result = authService.changePassword(authentication.getName(), request);
    authCookieService.clear(response);
    return result;
  }
}
