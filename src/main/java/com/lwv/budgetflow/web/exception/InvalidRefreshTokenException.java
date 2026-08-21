package com.lwv.budgetflow.web.exception;

public class InvalidRefreshTokenException extends RuntimeException {

  public InvalidRefreshTokenException() {
    super("Invalid or expired refresh token");
  }
}
