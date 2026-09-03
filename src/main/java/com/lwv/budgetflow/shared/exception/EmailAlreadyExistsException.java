package com.lwv.budgetflow.shared.exception;

public class EmailAlreadyExistsException extends RuntimeException {

  public EmailAlreadyExistsException(String email) {
    super("Email already registered: " + email);
  }
}
