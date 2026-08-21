package com.revenda.exception;

public class InsufficientStockException extends RuntimeException {

  public InsufficientStockException() {
    super("Insufficient stock");
  }
}
