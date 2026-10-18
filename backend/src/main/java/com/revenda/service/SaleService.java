package com.revenda.service;

import com.revenda.domain.InventoryMovement;
import com.revenda.domain.InventoryMovementType;
import com.revenda.domain.ProductVariant;
import com.revenda.domain.Sale;
import com.revenda.domain.SaleItem;
import com.revenda.exception.InvalidSaleException;
import com.revenda.exception.ResourceNotFoundException;
import com.revenda.repository.InventoryMovementRepository;
import com.revenda.repository.ProductVariantRepository;
import com.revenda.repository.SaleRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleService {

  public record SaleLine(Long variantId, int qty, BigDecimal unitPrice) {}

  public record DailyPoint(LocalDate date, BigDecimal revenue, long count) {}

  private final SaleRepository saleRepository;
  private final ProductVariantRepository productVariantRepository;
  private final InventoryMovementRepository inventoryMovementRepository;
  private final InventoryService inventoryService;

  public SaleService(
      SaleRepository saleRepository,
      ProductVariantRepository productVariantRepository,
      InventoryMovementRepository inventoryMovementRepository,
      InventoryService inventoryService) {
    this.saleRepository = saleRepository;
    this.productVariantRepository = productVariantRepository;
    this.inventoryMovementRepository = inventoryMovementRepository;
    this.inventoryService = inventoryService;
  }

  @Transactional
  public Sale create(List<SaleLine> lines) {
    if (lines == null || lines.isEmpty()) {
      throw new InvalidSaleException("Sale must have at least one item");
    }

    List<SaleLine> ordered =
        lines.stream().sorted(Comparator.comparing(SaleLine::variantId)).toList();

    Map<Long, ProductVariant> locked = new LinkedHashMap<>();
    BigDecimal total = BigDecimal.ZERO;
    TreeSet<Long> productIds = new TreeSet<>();

    for (SaleLine line : ordered) {
      if (line.qty() < 1) {
        throw new InvalidSaleException("Quantity must be >= 1");
      }
      if (line.unitPrice() == null || line.unitPrice().signum() < 0) {
        throw new InvalidSaleException("Unit price must be >= 0");
      }
      ProductVariant variant =
          locked.computeIfAbsent(
              line.variantId(),
              id ->
                  productVariantRepository
                      .findByIdForUpdate(id)
                      .orElseThrow(
                          () -> new ResourceNotFoundException("Variant not found: " + id)));
      variant.decreaseStock(line.qty());
      productIds.add(variant.getProductId());
      total = total.add(line.unitPrice().multiply(BigDecimal.valueOf(line.qty())));
    }

    Sale sale = new Sale(total);
    for (SaleLine line : ordered) {
      ProductVariant variant = locked.get(line.variantId());
      SaleItem item = new SaleItem(sale, variant, line.qty(), line.unitPrice());
      sale.addItem(item);
    }
    Sale saved = saleRepository.save(sale);

    for (SaleLine line : ordered) {
      ProductVariant variant = locked.get(line.variantId());
      inventoryMovementRepository.save(
          new InventoryMovement(
              variant, -line.qty(), "Counter sale", InventoryMovementType.SALE, saved.getId()));
    }

    inventoryService.syncParentStock(productIds);
    return saved;
  }

  @Transactional(readOnly = true)
  public List<Sale> list(Instant from, Instant to) {
    return saleRepository.findByCreatedAtBetweenWithItems(from, to);
  }

  @Transactional(readOnly = true)
  public List<DailyPoint> daily(Instant from, Instant to) {
    List<Sale> sales = saleRepository.findByCreatedAtBetweenWithItems(from, to);
    Map<LocalDate, DailyPointAcc> acc = new LinkedHashMap<>();
    for (Sale sale : sales) {
      LocalDate day = LocalDate.ofInstant(sale.getCreatedAt(), ZoneOffset.UTC);
      DailyPointAcc bucket = acc.computeIfAbsent(day, d -> new DailyPointAcc());
      bucket.revenue = bucket.revenue.add(sale.getTotalAmount());
      bucket.count++;
    }
    List<DailyPoint> points = new ArrayList<>();
    acc.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            e -> points.add(new DailyPoint(e.getKey(), e.getValue().revenue, e.getValue().count)));
    return points;
  }

  private static final class DailyPointAcc {
    private BigDecimal revenue = BigDecimal.ZERO;
    private long count;
  }
}
