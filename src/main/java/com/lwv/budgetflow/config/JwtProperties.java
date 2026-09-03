package com.lwv.budgetflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("security.jwt")
public record JwtProperties(
    String secret,
    String issuer,
    Duration accessTokenTtl,
    Duration refreshTokenTtl,
    boolean refreshCookieSecure) {
}
