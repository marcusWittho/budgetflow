package com.lwv.budgetflow.auth.controller;

import com.lwv.budgetflow.auth.dto.UserResponse;
import com.lwv.budgetflow.auth.entity.UserEntity;
import com.lwv.budgetflow.auth.repository.UserRepository;
import com.lwv.budgetflow.auth.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserRepository userRepository;

  @GetMapping("/me")
  public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
    UserEntity user = userRepository.findById(principal.getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    return new UserResponse(user.getId(), user.getEmail(), user.getFullName());
  }
}
