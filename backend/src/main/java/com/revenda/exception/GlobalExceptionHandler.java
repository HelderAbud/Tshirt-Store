package com.revenda.exception;

import java.util.HashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> fields = new HashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> fields.put(error.getField(), error.getDefaultMessage()));

    Map<String, Object> body = new HashMap<>();
    body.put("status", HttpStatus.BAD_REQUEST.value());
    body.put("error", "Validation failed");
    body.put("fields", fields);
    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler(DuplicateEmailException.class)
  public ResponseEntity<Map<String, Object>> handleDuplicateEmail(DuplicateEmailException ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("status", HttpStatus.CONFLICT.value());
    body.put("error", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
  }

  @ExceptionHandler(DuplicateSkuException.class)
  public ResponseEntity<Map<String, Object>> handleDuplicateSku(DuplicateSkuException ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("status", HttpStatus.CONFLICT.value());
    body.put("error", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
  }

  @ExceptionHandler(EmptyCartException.class)
  public ResponseEntity<Map<String, Object>> handleEmptyCart(EmptyCartException ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("status", HttpStatus.BAD_REQUEST.value());
    body.put("error", ex.getMessage());
    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler(InvalidOrderStatusTransitionException.class)
  public ResponseEntity<Map<String, Object>> handleInvalidTransition(
      InvalidOrderStatusTransitionException ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("status", HttpStatus.CONFLICT.value());
    body.put("error", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
  }

  @ExceptionHandler(InsufficientStockException.class)
  public ResponseEntity<Map<String, Object>> handleInsufficientStock(
      InsufficientStockException ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("status", HttpStatus.CONFLICT.value());
    body.put("error", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
  }

  @ExceptionHandler(ProductHasVariantsException.class)
  public ResponseEntity<Map<String, Object>> handleProductHasVariants(
      ProductHasVariantsException ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("status", HttpStatus.CONFLICT.value());
    body.put("error", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, Object>> handleDataIntegrity(
      DataIntegrityViolationException ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("status", HttpStatus.CONFLICT.value());
    body.put("error", "Resource already exists");
    return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("status", HttpStatus.NOT_FOUND.value());
    body.put("error", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
  }
}
