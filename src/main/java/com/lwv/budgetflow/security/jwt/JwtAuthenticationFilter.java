package com.lwv.budgetflow.security.jwt;

import com.lwv.budgetflow.security.userdetails.UserPrincipal;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtService jwtService;

  @Override
  protected void doFilterInternal(
      @Nonnull HttpServletRequest request,
      @Nonnull HttpServletResponse response,
      @Nonnull FilterChain filterChain) throws ServletException, IOException {

    extractToken(request)
        .flatMap(jwtService::parseAndValidate)
        .ifPresent(claims -> authenticate(claims, request));

    filterChain.doFilter(request, response);
  }

  private Optional<String> extractToken(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith(BEARER_PREFIX)) {
      return Optional.of(header.substring(BEARER_PREFIX.length()));
    }
    return Optional.empty();
  }

  private void authenticate(Claims claims, HttpServletRequest request) {
    UUID userId = UUID.fromString(claims.getSubject());
    String email = claims.get("email", String.class);
    UserPrincipal principal = UserPrincipal.fromToken(userId, email);

    var authentication = new UsernamePasswordAuthenticationToken(
        principal, null, principal.getAuthorities());
    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
