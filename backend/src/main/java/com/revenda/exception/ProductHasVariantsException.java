package com.revenda.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ProductHasVariantsException extends RuntimeException {

  public ProductHasVariantsException() {
    super("Product has variants");
  }
}
