package com.revenda.exception;

import java.util.HashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
    problem.setProperty("errors", errors);
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(MissingIdempotencyKeyException.class)
  public ResponseEntity<ProblemDetail> handleMissingIdempotencyKey(
      MissingIdempotencyKeyException ex) {
    return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(IdempotencyConflictException.class)
  public ResponseEntity<ProblemDetail> handleIdempotencyConflict(IdempotencyConflictException ex) {
    return problem(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(DuplicateEmailException.class)
  public ResponseEntity<ProblemDetail> handleDuplicateEmail(DuplicateEmailException ex) {
    return problem(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(DuplicateSkuException.class)
  public ResponseEntity<ProblemDetail> handleDuplicateSku(DuplicateSkuException ex) {
    return problem(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(EmptyCartException.class)
  public ResponseEntity<ProblemDetail> handleEmptyCart(EmptyCartException ex) {
    return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(InvalidSaleException.class)
  public ResponseEntity<ProblemDetail> handleInvalidSale(InvalidSaleException ex) {
    return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(InvalidOrderStatusTransitionException.class)
  public ResponseEntity<ProblemDetail> handleInvalidTransition(
      InvalidOrderStatusTransitionException ex) {
    return problem(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(InsufficientStockException.class)
  public ResponseEntity<ProblemDetail> handleInsufficientStock(InsufficientStockException ex) {
    return problem(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(ProductHasVariantsException.class)
  public ResponseEntity<ProblemDetail> handleProductHasVariants(ProductHasVariantsException ex) {
    return problem(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex) {
    return problem(HttpStatus.CONFLICT, "Resource already exists");
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException ex) {
    return problem(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(NfeXmlException.class)
  public ResponseEntity<ProblemDetail> handleNfeXml(NfeXmlException ex) {
    return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(NfeImportConflictException.class)
  public ResponseEntity<ProblemDetail> handleNfeConflict(NfeImportConflictException ex) {
    return problem(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ProblemDetail> handleMaxUpload(MaxUploadSizeExceededException ex) {
    return problem(HttpStatus.BAD_REQUEST, "XML file exceeds 1MB");
  }

  @ExceptionHandler(MissingServletRequestPartException.class)
  public ResponseEntity<ProblemDetail> handleMissingPart(MissingServletRequestPartException ex) {
    return problem(HttpStatus.BAD_REQUEST, "XML file is required");
  }

  private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
    return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
  }
}
