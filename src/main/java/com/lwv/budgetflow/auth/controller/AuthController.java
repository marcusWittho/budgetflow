package com.lwv.budgetflow.auth.controller;

import com.lwv.budgetflow.auth.dto.AuthResponse;
import com.lwv.budgetflow.auth.dto.LoginRequest;
import com.lwv.budgetflow.auth.dto.RegisterRequest;
import com.lwv.budgetflow.auth.service.AuthResult;
import com.lwv.budgetflow.auth.service.AuthService;
import com.lwv.budgetflow.config.JwtProperties;
import com.lwv.budgetflow.shared.exception.InvalidRefreshTokenException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private static final String ACCESS_COOKIE_NAME = "access_token";
  private static final String REFRESH_COOKIE_NAME = "refresh_token";

  private final AuthService authService;
  private final JwtProperties jwtProperties;

  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    AuthResult result = authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .header(HttpHeaders.SET_COOKIE, accessCookie(result.response().accessToken(), result.response().expiresIn()).toString())
        .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken()).toString())
        .body(result.response());
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    AuthResult result = authService.login(request);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, accessCookie(result.response().accessToken(), result.response().expiresIn()).toString())
        .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken()).toString())
        .body(result.response());
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(
      @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
    if (refreshToken == null) {
      throw new InvalidRefreshTokenException();
    }

    AuthResult result = authService.refresh(refreshToken);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, accessCookie(result.response().accessToken(), result.response().expiresIn()).toString())
        .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken()).toString())
        .body(result.response());
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
    if (refreshToken != null) {
      authService.logout(refreshToken);
    }

    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, expiredAccessCookie().toString())
        .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
        .build();
  }

  private ResponseCookie accessCookie(String accessToken, long expiresIn) {
    return ResponseCookie.from(ACCESS_COOKIE_NAME, accessToken)
        .httpOnly(true)
        .secure(jwtProperties.refreshCookieSecure())
        .sameSite("Lax")
        .path("/api")
        .maxAge(expiresIn)
        .build();
  }

  private ResponseCookie refreshCookie(String refreshToken) {
    return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
        .httpOnly(true)
        .secure(jwtProperties.refreshCookieSecure())
        .sameSite("Lax")
        .path("/api/auth")
        .maxAge(jwtProperties.refreshTokenTtl())
        .build();
  }

  private ResponseCookie expiredAccessCookie() {
    return ResponseCookie.from(ACCESS_COOKIE_NAME, "")
        .httpOnly(true)
        .secure(jwtProperties.refreshCookieSecure())
        .sameSite("Lax")
        .path("/api")
        .maxAge(0)
        .build();
  }

  private ResponseCookie expiredRefreshCookie() {
    return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
        .httpOnly(true)
        .secure(jwtProperties.refreshCookieSecure())
        .sameSite("Lax")
        .path("/api/auth")
        .maxAge(0)
        .build();
  }
}
