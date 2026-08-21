package com.revenda.controller;

import com.revenda.domain.Cart;
import com.revenda.domain.CartItem;
import com.revenda.domain.ProductVariant;
import com.revenda.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {

  private final CartService cartService;

  public CartController(CartService cartService) {
    this.cartService = cartService;
  }

  @GetMapping
  public ResponseEntity<CartResponse> get() {
    return ResponseEntity.ok(CartResponse.from(cartService.getOrCreate()));
  }

  @PutMapping("/items")
  public ResponseEntity<CartResponse> upsert(@Valid @RequestBody CartItemRequest request) {
    return ResponseEntity.ok(
        CartResponse.from(cartService.upsertItem(request.variantId(), request.qty())));
  }

  @DeleteMapping("/items/{variantId}")
  public ResponseEntity<Void> remove(@PathVariable Long variantId) {
    cartService.removeItem(variantId);
    return ResponseEntity.noContent().build();
  }

  public record CartItemRequest(@NotNull Long variantId, @NotNull @Min(1) Integer qty) {}

  public record CartItemResponse(
      Long variantId,
      String sku,
      String productName,
      String color,
      String size,
      BigDecimal price,
      Integer qty) {

    static CartItemResponse from(CartItem item) {
      ProductVariant variant = item.getVariant();
      return new CartItemResponse(
          variant.getId(),
          variant.getSku(),
          variant.getProduct().getName(),
          variant.getColor(),
          variant.getSize(),
          variant.getPrice(),
          item.getQty());
    }
  }

  public record CartResponse(Long id, List<CartItemResponse> items) {

    static CartResponse from(Cart cart) {
      return new CartResponse(
          cart.getId(), cart.getItems().stream().map(CartItemResponse::from).toList());
    }
  }
}
