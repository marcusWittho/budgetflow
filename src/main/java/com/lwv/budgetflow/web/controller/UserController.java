package com.lwv.budgetflow.web.controller;

import com.lwv.budgetflow.domain.entity.UserEntity;
import com.lwv.budgetflow.domain.repository.UserRepository;
import com.lwv.budgetflow.security.userdetails.UserPrincipal;
import com.lwv.budgetflow.web.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.http.HttpStatus;

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
