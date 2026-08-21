package com.revenda.controller;

import com.revenda.domain.OrderStatus;
import com.revenda.domain.ShopOrder;
import com.revenda.exception.InvalidOrderStatusTransitionException;
import com.revenda.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

  private final OrderService orderService;

  public AdminOrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  @PatchMapping("/{id}/status")
  public ResponseEntity<AdminOrderResponse> updateStatus(
      @PathVariable Long id, @Valid @RequestBody StatusRequest request) {
    if (request.status() != OrderStatus.CANCELLED) {
      throw new InvalidOrderStatusTransitionException();
    }
    ShopOrder order = orderService.cancelAsAdmin(id);
    return ResponseEntity.ok(AdminOrderResponse.from(order));
  }

  public record StatusRequest(@NotNull OrderStatus status) {}

  public record AdminOrderResponse(Long id, String status, BigDecimal total) {

    static AdminOrderResponse from(ShopOrder order) {
      return new AdminOrderResponse(order.getId(), order.getStatus().name(), order.getTotal());
    }
  }
}
