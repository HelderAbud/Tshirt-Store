package com.revenda.controller;

import com.revenda.domain.OrderItem;
import com.revenda.domain.ShopOrder;
import com.revenda.exception.MissingIdempotencyKeyException;
import com.revenda.service.OrderService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

  private final OrderService orderService;

  public OrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  @PostMapping
  public ResponseEntity<OrderDetailResponse> checkout() {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(OrderDetailResponse.from(orderService.checkout()));
  }

  @GetMapping
  public ResponseEntity<List<OrderSummaryResponse>> listMine() {
    List<OrderSummaryResponse> orders =
        orderService.listMine().stream().map(OrderSummaryResponse::from).toList();
    return ResponseEntity.ok(orders);
  }

  @GetMapping("/{id}")
  public ResponseEntity<OrderDetailResponse> getMine(@PathVariable Long id) {
    return ResponseEntity.ok(OrderDetailResponse.from(orderService.getMine(id)));
  }

  @PostMapping("/{id}/pay")
  public ResponseEntity<OrderDetailResponse> pay(
      @PathVariable Long id,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 128) {
      throw new MissingIdempotencyKeyException();
    }
    return ResponseEntity.ok(OrderDetailResponse.from(orderService.pay(id, idempotencyKey.trim())));
  }

  public record OrderSummaryResponse(Long id, String status, BigDecimal total) {

    static OrderSummaryResponse from(ShopOrder order) {
      return new OrderSummaryResponse(order.getId(), order.getStatus().name(), order.getTotal());
    }
  }

  public record OrderItemResponse(
      Long variantId,
      String sku,
      String productName,
      String color,
      String size,
      BigDecimal unitPrice,
      Integer qty) {

    static OrderItemResponse from(OrderItem item) {
      return new OrderItemResponse(
          item.getVariant().getId(),
          item.getSku(),
          item.getProductName(),
          item.getColor(),
          item.getSize(),
          item.getUnitPrice(),
          item.getQty());
    }
  }

  public record OrderDetailResponse(
      Long id, String status, BigDecimal total, List<OrderItemResponse> items) {

    static OrderDetailResponse from(ShopOrder order) {
      return new OrderDetailResponse(
          order.getId(),
          order.getStatus().name(),
          order.getTotal(),
          order.getItems().stream().map(OrderItemResponse::from).toList());
    }
  }
}
