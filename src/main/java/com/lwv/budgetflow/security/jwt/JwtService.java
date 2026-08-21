package com.lwv.budgetflow.security.jwt;

import com.lwv.budgetflow.security.userdetails.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JwtService {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final JwtProperties jwtProperties;

  private SecretKey signingKey() {
    return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
  }

  public String generateAccessToken(UserPrincipal principal) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(principal.getId().toString())
        .claim("email", principal.getEmail())
        .issuer(jwtProperties.issuer())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(jwtProperties.accessTokenTtl())))
        .signWith(signingKey(), Jwts.SIG.HS256)
        .compact();
  }

  public Optional<Claims> parseAndValidate(String token) {
    try {
      Claims claims = Jwts.parser()
          .verifyWith(signingKey())
          .build()
          .parseSignedClaims(token)
          .getPayload();
      return Optional.of(claims);
    } catch (JwtException | IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  public String generateOpaqueRefreshToken() {
    byte[] bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public long getAccessTokenTtlSeconds() {
    return jwtProperties.accessTokenTtl().getSeconds();
  }
}
