package com.revenda.service;

import com.revenda.domain.Product;
import com.revenda.domain.ProductVariant;
import com.revenda.repository.ProductVariantRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductVariantService {

  private final ProductService productService;
  private final ProductVariantRepository productVariantRepository;

  public ProductVariantService(
      ProductService productService, ProductVariantRepository productVariantRepository) {
    this.productService = productService;
    this.productVariantRepository = productVariantRepository;
  }

  @Transactional
  public ProductVariant create(
      Long productId, String color, String size, String sku, BigDecimal price, Integer stockQty) {
    Product product = productService.getById(productId);
    productService.assertSkuAvailable(sku);
    return productVariantRepository.save(
        new ProductVariant(product, color, size, sku, price, stockQty));
  }

  @Transactional(readOnly = true)
  public List<ProductVariant> listByProductId(Long productId) {
    productService.getById(productId);
    return productVariantRepository.findByProduct_Id(productId);
  }
}
