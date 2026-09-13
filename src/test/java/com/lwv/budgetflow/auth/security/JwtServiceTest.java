package com.lwv.budgetflow.auth.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.lwv.budgetflow.auth.entity.UserEntity;
import com.lwv.budgetflow.auth.entity.UserStatus;
import com.lwv.budgetflow.config.JwtProperties;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService")
class JwtServiceTest {

  @Mock
  private JwtProperties jwtProperties;

  @InjectMocks
  private JwtService jwtService;

  private UserEntity testUser;
  private UserPrincipal testPrincipal;
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

    testPrincipal = new UserPrincipal(testUser);

    lenient().when(jwtProperties.secret()).thenReturn("test-secret-key-with-minimum-32-characters-long-enough");
    lenient().when(jwtProperties.issuer()).thenReturn("budgetflow");
    lenient().when(jwtProperties.accessTokenTtl()).thenReturn(Duration.ofMinutes(15));
    lenient().when(jwtProperties.refreshTokenTtl()).thenReturn(Duration.ofDays(7));
  }

  @Test
  @DisplayName("deve gerar access token válido")
  void testGenerateAccessToken() {
    String token = jwtService.generateAccessToken(testPrincipal);

    assertNotNull(token);
    assertFalse(token.isBlank());
    assertTrue(token.contains("."));

    Optional<Claims> claims = jwtService.parseAndValidate(token);
    assertTrue(claims.isPresent());
    assertEquals(testUserId.toString(), claims.get().getSubject());
    assertEquals("teste@example.com", claims.get().get("email"));
    assertEquals("budgetflow", claims.get().getIssuer());
  }

  @Test
  @DisplayName("deve validar access token correto")
  void testParseAndValidateValidToken() {
    String token = jwtService.generateAccessToken(testPrincipal);

    Optional<Claims> result = jwtService.parseAndValidate(token);

    assertTrue(result.isPresent());
    Claims claims = result.get();
    assertEquals(testUserId.toString(), claims.getSubject());
    assertEquals("teste@example.com", claims.get("email"));
  }

  @Test
  @DisplayName("deve rejeitar token inválido")
  void testParseAndValidateInvalidToken() {
    String invalidToken = "invalid.token.here";

    Optional<Claims> result = jwtService.parseAndValidate(invalidToken);

    assertFalse(result.isPresent());
  }

  @Test
  @DisplayName("deve rejeitar token vazio")
  void testParseAndValidateEmptyToken() {
    Optional<Claims> result = jwtService.parseAndValidate("");

    assertFalse(result.isPresent());
  }

  @Test
  @DisplayName("deve gerar refresh token opaco único")
  void testGenerateOpaqueRefreshToken() {
    String token1 = jwtService.generateOpaqueRefreshToken();
    String token2 = jwtService.generateOpaqueRefreshToken();

    assertNotNull(token1);
    assertNotNull(token2);
    assertFalse(token1.isBlank());
    assertFalse(token2.isBlank());
    assertNotEquals(token1, token2);
    assertTrue(token1.length() > 0);
  }

  @Test
  @DisplayName("deve retornar TTL do access token em segundos")
  void testGetAccessTokenTtlSeconds() {
    long ttl = jwtService.getAccessTokenTtlSeconds();

    assertEquals(900, ttl); // 15 minutos = 900 segundos
  }

  @Test
  @DisplayName("token com secret errado deve ser rejeitado")
  void testParseWithWrongSecret() {
    String token = jwtService.generateAccessToken(testPrincipal);

    when(jwtProperties.secret()).thenReturn("wrong-secret-key-with-minimum-32-characters-length");

    Optional<Claims> result = jwtService.parseAndValidate(token);

    assertFalse(result.isPresent());
  }

  @Test
  @DisplayName("refresh token deve ser URL-safe")
  void testRefreshTokenIsUrlSafe() {
    String token = jwtService.generateOpaqueRefreshToken();

    // URL-safe base64 deve conter apenas chars: A-Z, a-z, 0-9, -, _
    assertTrue(token.matches("[A-Za-z0-9_-]+"));
  }

  @Test
  @DisplayName("access token deve conter todas as claims necessárias")
  void testAccessTokenContainsAllRequiredClaims() {
    String token = jwtService.generateAccessToken(testPrincipal);

    Optional<Claims> claims = jwtService.parseAndValidate(token);
    assertTrue(claims.isPresent());

    Claims claimsObj = claims.get();
    assertNotNull(claimsObj.getSubject());
    assertNotNull(claimsObj.get("email"));
    assertNotNull(claimsObj.getIssuer());
    assertNotNull(claimsObj.getIssuedAt());
    assertNotNull(claimsObj.getExpiration());
  }
}
