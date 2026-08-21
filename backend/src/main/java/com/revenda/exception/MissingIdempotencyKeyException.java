package com.revenda.exception;

public class MissingIdempotencyKeyException extends RuntimeException {

  public MissingIdempotencyKeyException() {
    super("Idempotency-Key header is required");
  }
}
