package com.lwv.budgetflow.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private AuthenticationManager authenticationManager;

  @Mock
  private JwtService jwtService;

  @Mock
  private JwtProperties jwtProperties;

  @InjectMocks
  private AuthService authService;

  private RegisterRequest validRegisterRequest;
  private LoginRequest validLoginRequest;
  private UserEntity testUser;
  private UUID testUserId;

  @BeforeEach
  void setUp() {
    testUserId = UUID.randomUUID();
    testUser = UserEntity.builder()
        .id(testUserId)
        .email("teste@example.com")
        .passwordHash("hashed_password")
        .fullName("Teste User")
        .status(UserStatus.ACTIVE)
        .build();

    validRegisterRequest = new RegisterRequest("novo@example.com", "senha123456", "Novo User");
    validLoginRequest = new LoginRequest("teste@example.com", "senha123456");
  }

  @Test
  @DisplayName("deve registrar novo usuário com sucesso")
  void testRegisterSuccess() {
    var newUser = UserEntity.builder()
        .id(testUserId)
        .email(validRegisterRequest.email())
        .passwordHash("hashed_password")
        .fullName(validRegisterRequest.fullName())
        .status(UserStatus.ACTIVE)
        .build();

    when(userRepository.findByEmail(validRegisterRequest.email())).thenReturn(Optional.empty());
    when(passwordEncoder.encode(validRegisterRequest.password())).thenReturn("hashed_password");
    when(userRepository.save(any(UserEntity.class))).thenReturn(newUser);
    when(jwtService.generateAccessToken(any(UserPrincipal.class))).thenReturn("access_token");
    when(jwtService.generateOpaqueRefreshToken()).thenReturn("refresh_token");
    when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
    when(jwtProperties.refreshTokenTtl()).thenReturn(Duration.ofDays(7));

    AuthResult result = authService.register(validRegisterRequest);

    assertNotNull(result);
    assertNotNull(result.response());
    assertNotNull(result.refreshToken());
    assertEquals("access_token", result.response().accessToken());
    assertEquals(900, result.response().expiresIn());

    verify(userRepository).findByEmail(validRegisterRequest.email());
    verify(passwordEncoder).encode(validRegisterRequest.password());
    verify(userRepository).save(argThat(u ->
        u.getEmail().equals(validRegisterRequest.email()) &&
            u.getFullName().equals(validRegisterRequest.fullName())
    ));
    verify(refreshTokenRepository).save(any(RefreshTokenEntity.class));
  }

  @Test
  @DisplayName("deve falhar ao registrar com email já existente")
  void testRegisterFailsDuplicateEmail() {
    when(userRepository.findByEmail(validRegisterRequest.email()))
        .thenReturn(Optional.of(testUser));

    assertThrows(EmailAlreadyExistsException.class, () ->
        authService.register(validRegisterRequest)
    );

    verify(userRepository).findByEmail(validRegisterRequest.email());
    verify(passwordEncoder, never()).encode(anyString());
  }

  @Test
  @DisplayName("deve fazer login com sucesso")
  void testLoginSuccess() {
    when(userRepository.findByEmail(validLoginRequest.email())).thenReturn(Optional.of(testUser));
    when(jwtService.generateAccessToken(any(UserPrincipal.class))).thenReturn("access_token");
    when(jwtService.generateOpaqueRefreshToken()).thenReturn("refresh_token");
    when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
    when(jwtProperties.refreshTokenTtl()).thenReturn(Duration.ofDays(7));

    AuthResult result = authService.login(validLoginRequest);

    assertNotNull(result);
    assertEquals("access_token", result.response().accessToken());
    assertEquals(900, result.response().expiresIn());

    ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
    verify(userRepository).save(userCaptor.capture());
    assertNotNull(userCaptor.getValue().getLastLoginAt());
    verify(refreshTokenRepository).save(any(RefreshTokenEntity.class));
  }

  @Test
  @DisplayName("deve refrescar token com sucesso")
  void testRefreshSuccess() {
    String refreshToken = "valid_refresh_token";
    String tokenHash = hashToken(refreshToken);

    RefreshTokenEntity storedToken = RefreshTokenEntity.builder()
        .id(UUID.randomUUID())
        .userId(testUserId)
        .tokenHash(tokenHash)
        .expiresAt(OffsetDateTime.now().plusDays(7))
        .revokedAt(null)
        .build();

    when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(anyString()))
        .thenReturn(storedToken);
    when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
    when(jwtService.generateAccessToken(any(UserPrincipal.class))).thenReturn("new_access_token");
    when(jwtService.generateOpaqueRefreshToken()).thenReturn("new_refresh_token");
    when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
    when(jwtProperties.refreshTokenTtl()).thenReturn(Duration.ofDays(7));

    AuthResult result = authService.refresh(refreshToken);

    assertNotNull(result);
    assertEquals("new_access_token", result.response().accessToken());
    assertEquals("new_refresh_token", result.refreshToken());
    verify(refreshTokenRepository, atLeastOnce()).save(any(RefreshTokenEntity.class));
  }

  @Test
  @DisplayName("deve falhar ao refrescar token expirado")
  void testRefreshExpiredToken() {
    String refreshToken = "expired_refresh_token";
    RefreshTokenEntity expiredToken = RefreshTokenEntity.builder()
        .id(UUID.randomUUID())
        .userId(testUserId)
        .tokenHash(hashToken(refreshToken))
        .expiresAt(OffsetDateTime.now().minusDays(1))
        .revokedAt(null)
        .build();

    when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(anyString()))
        .thenReturn(expiredToken);

    assertThrows(InvalidRefreshTokenException.class, () ->
        authService.refresh(refreshToken)
    );
  }

  @Test
  @DisplayName("deve falhar ao refrescar token revogado")
  void testRefreshRevokedToken() {
    String refreshToken = "revoked_refresh_token";

    when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(anyString()))
        .thenReturn(null);

    assertThrows(InvalidRefreshTokenException.class, () ->
        authService.refresh(refreshToken)
    );
  }

  @Test
  @DisplayName("deve fazer logout e revogar refresh token")
  void testLogoutSuccess() {
    String refreshToken = "valid_refresh_token";
    RefreshTokenEntity storedToken = RefreshTokenEntity.builder()
        .id(UUID.randomUUID())
        .userId(testUserId)
        .tokenHash(hashToken(refreshToken))
        .expiresAt(OffsetDateTime.now().plusDays(7))
        .revokedAt(null)
        .build();

    when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(anyString()))
        .thenReturn(storedToken);

    authService.logout(refreshToken);

    ArgumentCaptor<RefreshTokenEntity> tokenCaptor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
    verify(refreshTokenRepository).save(tokenCaptor.capture());
    assertNotNull(tokenCaptor.getValue().getRevokedAt());
  }

  @Test
  @DisplayName("logout não deve falhar se token não existir")
  void testLogoutNonExistentToken() {
    String refreshToken = "non_existent_token";

    when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(anyString()))
        .thenReturn(null);

    authService.logout(refreshToken);

    verify(refreshTokenRepository, never()).save(any());
  }

  private String hashToken(String token) {
    try {
      java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return java.util.Base64.getEncoder().encodeToString(hashed);
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }
}
