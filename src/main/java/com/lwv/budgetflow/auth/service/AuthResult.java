package com.lwv.budgetflow.auth.service;

import com.lwv.budgetflow.auth.dto.AuthResponse;

public record AuthResult(AuthResponse response, String refreshToken) {
}
