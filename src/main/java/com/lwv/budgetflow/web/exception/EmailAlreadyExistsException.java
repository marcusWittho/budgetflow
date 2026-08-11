package com.lwv.budgetflow.web.exception;

public class EmailAlreadyExistsException extends RuntimeException {

  public EmailAlreadyExistsException(String email) {
    super("Email already registered: " + email);
  }
}
