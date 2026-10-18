package com.revenda.controller;

import com.revenda.domain.Sale;
import com.revenda.domain.SaleItem;
import com.revenda.service.SaleService;
import com.revenda.service.SaleService.DailyPoint;
import com.revenda.service.SaleService.SaleLine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/sales")
public class AdminSaleController {

  private final SaleService saleService;

  public AdminSaleController(SaleService saleService) {
    this.saleService = saleService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SaleResponse create(@Valid @RequestBody CreateSaleRequest request) {
    List<SaleLine> lines =
        request.items().stream()
            .map(i -> new SaleLine(i.variantId(), i.qty(), i.unitPrice()))
            .toList();
    return SaleResponse.from(saleService.create(lines));
  }

  @GetMapping
  public List<SaleResponse> list(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    Instant fromInstant = from.atStartOfDay().toInstant(ZoneOffset.UTC);
    Instant toInstant = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    return saleService.list(fromInstant, toInstant).stream().map(SaleResponse::from).toList();
  }

  @GetMapping("/daily")
  public List<DailyResponse> daily(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    Instant fromInstant = from.atStartOfDay().toInstant(ZoneOffset.UTC);
    Instant toInstant = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    return saleService.daily(fromInstant, toInstant).stream().map(DailyResponse::from).toList();
  }

  public record CreateSaleRequest(@NotEmpty List<@Valid SaleItemRequest> items) {}

  public record SaleItemRequest(
      @NotNull Long variantId,
      @NotNull @Min(1) Integer qty,
      @NotNull @DecimalMin("0.0") BigDecimal unitPrice) {}

  public record SaleResponse(
      Long id, BigDecimal totalAmount, Instant createdAt, List<SaleItemResponse> items) {
    static SaleResponse from(Sale sale) {
      return new SaleResponse(
          sale.getId(),
          sale.getTotalAmount(),
          sale.getCreatedAt(),
          sale.getItems().stream().map(SaleItemResponse::from).toList());
    }
  }

  public record SaleItemResponse(
      Long variantId, Integer qty, BigDecimal unitPrice, BigDecimal lineTotal) {
    static SaleItemResponse from(SaleItem item) {
      return new SaleItemResponse(
          item.getVariant().getId(), item.getQty(), item.getUnitPrice(), item.getLineTotal());
    }
  }

  public record DailyResponse(LocalDate date, BigDecimal revenue, long count) {
    static DailyResponse from(DailyPoint point) {
      return new DailyResponse(point.date(), point.revenue(), point.count());
    }
  }
}
