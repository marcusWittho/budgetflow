package com.lwv.budgetflow.auth.service;

import com.lwv.budgetflow.auth.dto.AuthResponse;
import com.lwv.budgetflow.auth.dto.LoginRequest;
import com.lwv.budgetflow.auth.dto.RegisterRequest;
import com.lwv.budgetflow.auth.entity.RefreshTokenEntity;
import com.lwv.budgetflow.auth.entity.UserEntity;
import com.lwv.budgetflow.auth.entity.UserStatus;
import com.lwv.budgetflow.auth.repository.RefreshTokenRepository;
import com.lwv.budgetflow.auth.repository.UserRepository;
import com.lwv.budgetflow.auth.security.JwtService;
import com.lwv.budgetflow.auth.security.UserPrincipal;
import com.lwv.budgetflow.config.JwtProperties;
import com.lwv.budgetflow.shared.exception.EmailAlreadyExistsException;
import com.lwv.budgetflow.shared.exception.InvalidRefreshTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final JwtProperties jwtProperties;

  @Transactional
  public AuthResult register(RegisterRequest request) {
    userRepository.findByEmail(request.email()).ifPresent(existing -> {
      throw new EmailAlreadyExistsException(request.email());
    });

    UserEntity user = UserEntity.builder()
        .email(request.email())
        .passwordHash(passwordEncoder.encode(request.password()))
        .fullName(request.fullName())
        .status(UserStatus.ACTIVE)
        .build();
    user = userRepository.save(user);

    return issueTokens(new UserPrincipal(user));
  }

  @Transactional
  public AuthResult login(LoginRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.email(), request.password()));

    UserEntity user = userRepository.findByEmail(request.email())
        .orElseThrow(() -> new IllegalStateException("Authenticated user vanished: " + request.email()));
    user.setLastLoginAt(OffsetDateTime.now());
    userRepository.save(user);

    return issueTokens(new UserPrincipal(user));
  }

  @Transactional
  public AuthResult refresh(String refreshToken) {
    String hash = hash(refreshToken);
    RefreshTokenEntity stored = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash);

    if (stored == null || stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
      throw new InvalidRefreshTokenException();
    }

    stored.setRevokedAt(OffsetDateTime.now());
    refreshTokenRepository.save(stored);

    UserEntity user = userRepository.findById(stored.getUserId())
        .orElseThrow(InvalidRefreshTokenException::new);

    return issueTokens(new UserPrincipal(user));
  }

  @Transactional
  public void logout(String refreshToken) {
    String hash = hash(refreshToken);
    RefreshTokenEntity stored = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash);
    if (stored != null) {
      stored.setRevokedAt(OffsetDateTime.now());
      refreshTokenRepository.save(stored);
    }
  }

  private AuthResult issueTokens(UserPrincipal principal) {
    String accessToken = jwtService.generateAccessToken(principal);
    String refreshToken = jwtService.generateOpaqueRefreshToken();

    RefreshTokenEntity entity = RefreshTokenEntity.builder()
        .userId(principal.getId())
        .tokenHash(hash(refreshToken))
        .expiresAt(OffsetDateTime.now().plus(jwtProperties.refreshTokenTtl()))
        .build();
    refreshTokenRepository.save(entity);

    AuthResponse response = AuthResponse.of(accessToken, jwtService.getAccessTokenTtlSeconds());
    return new AuthResult(response, refreshToken);
  }

  private String hash(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(hashed);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }
}
