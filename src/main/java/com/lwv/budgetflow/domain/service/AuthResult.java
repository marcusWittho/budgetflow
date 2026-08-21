package com.lwv.budgetflow.domain.service;

import com.lwv.budgetflow.web.dto.AuthResponse;

public record AuthResult(AuthResponse response, String refreshToken) {
}
