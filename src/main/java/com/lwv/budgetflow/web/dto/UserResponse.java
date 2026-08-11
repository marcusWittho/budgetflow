package com.lwv.budgetflow.web.dto;

import java.util.UUID;

public record UserResponse(UUID id, String email, String fullName) {
}
