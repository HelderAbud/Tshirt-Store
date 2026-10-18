package com.revenda.controller;

import com.revenda.domain.ProductVariant;
import com.revenda.service.InventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/inventory")
public class AdminInventoryController {

  private final InventoryService inventoryService;

  public AdminInventoryController(InventoryService inventoryService) {
    this.inventoryService = inventoryService;
  }

  @PostMapping("/adjust")
  @ResponseStatus(HttpStatus.OK)
  public AdjustResponse adjust(@Valid @RequestBody AdjustRequest request) {
    ProductVariant variant =
        inventoryService.adjust(request.variantId(), request.delta(), request.reason());
    return AdjustResponse.from(variant);
  }

  @GetMapping("/low-stock")
  public java.util.List<LowStockItem> lowStock(@RequestParam(defaultValue = "5") int threshold) {
    return inventoryService.lowStock(threshold).stream().map(LowStockItem::from).toList();
  }

  @GetMapping("/stock")
  public java.util.List<LowStockItem> stock() {
    return inventoryService.listStock().stream().map(LowStockItem::from).toList();
  }

  public record AdjustRequest(
      @NotNull Long variantId, @NotNull Integer delta, @NotBlank @Size(max = 255) String reason) {}

  public record AdjustResponse(Long variantId, Long productId, String sku, Integer stockQty) {
    static AdjustResponse from(ProductVariant variant) {
      return new AdjustResponse(
          variant.getId(), variant.getProductId(), variant.getSku(), variant.getStockQty());
    }
  }

  public record LowStockItem(
      Long variantId,
      Long productId,
      String sku,
      String color,
      String size,
      Integer stockQty,
      java.math.BigDecimal price) {
    static LowStockItem from(ProductVariant variant) {
      return new LowStockItem(
          variant.getId(),
          variant.getProductId(),
          variant.getSku(),
          variant.getColor(),
          variant.getSize(),
          variant.getStockQty(),
          variant.getPrice());
    }
  }
}
