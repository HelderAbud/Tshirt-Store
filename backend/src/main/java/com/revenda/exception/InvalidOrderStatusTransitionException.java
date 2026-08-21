package com.revenda.exception;

public class InvalidOrderStatusTransitionException extends RuntimeException {

  public InvalidOrderStatusTransitionException() {
    super("Invalid order status transition");
  }
}
