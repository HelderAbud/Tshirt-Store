package com.revenda.controller;

import com.revenda.domain.Product;
import com.revenda.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

  private final ProductService productService;

  public AdminProductController(ProductService productService) {
    this.productService = productService;
  }

  @PostMapping
  public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
    Product product =
        productService.create(request.name(), request.sku(), request.price(), request.stockQty());
    return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(product));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
    return ResponseEntity.ok(ProductResponse.from(productService.getById(id)));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ProductResponse> update(
      @PathVariable Long id, @Valid @RequestBody ProductRequest request) {
    Product product =
        productService.update(
            id, request.name(), request.sku(), request.price(), request.stockQty());
    return ResponseEntity.ok(ProductResponse.from(product));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    productService.delete(id);
    return ResponseEntity.noContent().build();
  }

  public record ProductRequest(
      @NotBlank String name,
      @NotBlank String sku,
      @NotNull @DecimalMin("0.00") BigDecimal price,
      @NotNull @Min(0) Integer stockQty) {}

  public record ProductResponse(
      Long id, String name, String sku, BigDecimal price, Integer stockQty, Instant createdAt) {

    static ProductResponse from(Product product) {
      return new ProductResponse(
          product.getId(),
          product.getName(),
          product.getSku(),
          product.getPrice(),
          product.getStockQty(),
          product.getCreatedAt());
    }
  }
}
