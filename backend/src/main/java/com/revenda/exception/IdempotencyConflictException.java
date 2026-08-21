package com.revenda.exception;

public class IdempotencyConflictException extends RuntimeException {

  public IdempotencyConflictException() {
    super("Idempotency key already used");
  }
}
