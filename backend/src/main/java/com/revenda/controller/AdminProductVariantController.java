package com.revenda.controller;

import com.revenda.domain.ProductVariant;
import com.revenda.service.ProductVariantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/products/{productId}/variants")
public class AdminProductVariantController {

  private final ProductVariantService productVariantService;

  public AdminProductVariantController(ProductVariantService productVariantService) {
    this.productVariantService = productVariantService;
  }

  @PostMapping
  public ResponseEntity<VariantResponse> create(
      @PathVariable Long productId, @Valid @RequestBody VariantRequest request) {
    ProductVariant variant =
        productVariantService.create(
            productId,
            request.color(),
            request.size(),
            request.sku(),
            request.price(),
            request.stockQty());
    return ResponseEntity.status(HttpStatus.CREATED).body(VariantResponse.from(variant));
  }

  @GetMapping
  public ResponseEntity<List<VariantResponse>> list(@PathVariable Long productId) {
    List<VariantResponse> variants =
        productVariantService.listByProductId(productId).stream()
            .map(VariantResponse::from)
            .toList();
    return ResponseEntity.ok(variants);
  }

  public record VariantRequest(
      @NotBlank String color,
      @NotBlank String size,
      @NotBlank String sku,
      @NotNull @DecimalMin("0.00") BigDecimal price,
      @NotNull @Min(0) Integer stockQty) {}

  public record VariantResponse(
      Long id,
      Long productId,
      String color,
      String size,
      String sku,
      BigDecimal price,
      Integer stockQty) {

    static VariantResponse from(ProductVariant variant) {
      return new VariantResponse(
          variant.getId(),
          variant.getProductId(),
          variant.getColor(),
          variant.getSize(),
          variant.getSku(),
          variant.getPrice(),
          variant.getStockQty());
    }
  }
}
