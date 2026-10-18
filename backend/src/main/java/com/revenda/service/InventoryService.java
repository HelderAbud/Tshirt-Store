package com.revenda.service;

import com.revenda.domain.InventoryMovement;
import com.revenda.domain.InventoryMovementType;
import com.revenda.domain.Product;
import com.revenda.domain.ProductVariant;
import com.revenda.exception.InvalidSaleException;
import com.revenda.exception.ResourceNotFoundException;
import com.revenda.repository.InventoryMovementRepository;
import com.revenda.repository.ProductRepository;
import com.revenda.repository.ProductVariantRepository;
import java.util.List;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

  private final ProductVariantRepository productVariantRepository;
  private final ProductRepository productRepository;
  private final InventoryMovementRepository inventoryMovementRepository;

  public InventoryService(
      ProductVariantRepository productVariantRepository,
      ProductRepository productRepository,
      InventoryMovementRepository inventoryMovementRepository) {
    this.productVariantRepository = productVariantRepository;
    this.productRepository = productRepository;
    this.inventoryMovementRepository = inventoryMovementRepository;
  }

  @Transactional
  public ProductVariant adjust(Long variantId, int delta, String reason) {
    if (delta == 0) {
      throw new InvalidSaleException("Delta must not be zero");
    }
    if (reason == null || reason.isBlank()) {
      throw new InvalidSaleException("Reason is required");
    }
    ProductVariant variant =
        productVariantRepository
            .findByIdForUpdate(variantId)
            .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantId));
    variant.adjustStock(delta);
    inventoryMovementRepository.save(
        new InventoryMovement(
            variant, delta, reason.trim(), InventoryMovementType.ADJUSTMENT, null));
    syncParentStock(variant.getProductId());
    return variant;
  }

  @Transactional(readOnly = true)
  public List<ProductVariant> lowStock(int threshold) {
    if (threshold < 0) {
      throw new InvalidSaleException("Threshold must be >= 0");
    }
    return productVariantRepository.findByStockQtyLessThanEqualOrderByStockQtyAsc(threshold);
  }

  @Transactional(readOnly = true)
  public List<ProductVariant> listStock() {
    return productVariantRepository.findAllByOrderBySkuAsc();
  }

  void syncParentStock(Long productId) {
    TreeSet<Long> ids = new TreeSet<>();
    ids.add(productId);
    syncParentStock(ids);
  }

  void syncParentStock(TreeSet<Long> productIds) {
    for (Long productId : productIds) {
      Product product =
          productRepository
              .findByIdForUpdate(productId)
              .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
      int remaining =
          productVariantRepository.findByProduct_Id(productId).stream()
              .mapToInt(ProductVariant::getStockQty)
              .sum();
      product.syncStockQty(remaining);
    }
  }
}
