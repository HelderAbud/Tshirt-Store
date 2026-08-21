package com.revenda.controller;

import com.revenda.domain.Product;
import com.revenda.domain.ProductVariant;
import com.revenda.service.ProductService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog/products")
public class CatalogProductController {

  private final ProductService productService;

  public CatalogProductController(ProductService productService) {
    this.productService = productService;
  }

  @GetMapping
  public ResponseEntity<Page<CatalogProductResponse>> list(
      @PageableDefault(size = 20) Pageable pageable) {
    Page<CatalogProductResponse> page =
        productService.list(pageable).map(CatalogProductResponse::from);
    return ResponseEntity.ok(page);
  }

  @GetMapping("/{id}")
  public ResponseEntity<CatalogProductDetailResponse> getById(@PathVariable Long id) {
    return ResponseEntity.ok(
        CatalogProductDetailResponse.from(productService.getByIdWithVariants(id)));
  }

  public record CatalogProductResponse(
      Long id, String name, String sku, BigDecimal price, Integer stockQty) {

    static CatalogProductResponse from(Product product) {
      return new CatalogProductResponse(
          product.getId(),
          product.getName(),
          product.getSku(),
          product.getPrice(),
          product.getStockQty());
    }
  }

  public record CatalogVariantResponse(
      Long id, String color, String size, String sku, BigDecimal price, Integer stockQty) {

    static CatalogVariantResponse from(ProductVariant variant) {
      return new CatalogVariantResponse(
          variant.getId(),
          variant.getColor(),
          variant.getSize(),
          variant.getSku(),
          variant.getPrice(),
          variant.getStockQty());
    }
  }

  public record CatalogProductDetailResponse(
      Long id,
      String name,
      String sku,
      BigDecimal price,
      Integer stockQty,
      List<CatalogVariantResponse> variants) {

    static CatalogProductDetailResponse from(Product product) {
      List<CatalogVariantResponse> variants =
          product.getVariants().stream().map(CatalogVariantResponse::from).toList();
      return new CatalogProductDetailResponse(
          product.getId(),
          product.getName(),
          product.getSku(),
          product.getPrice(),
          product.getStockQty(),
          variants);
    }
  }
}
