package com.revenda.service;

import com.revenda.domain.Product;
import com.revenda.exception.DuplicateSkuException;
import com.revenda.exception.ProductHasVariantsException;
import com.revenda.exception.ResourceNotFoundException;
import com.revenda.repository.ProductRepository;
import com.revenda.repository.ProductVariantRepository;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

  private final ProductRepository productRepository;
  private final ProductVariantRepository productVariantRepository;

  public ProductService(
      ProductRepository productRepository, ProductVariantRepository productVariantRepository) {
    this.productRepository = productRepository;
    this.productVariantRepository = productVariantRepository;
  }

  @Transactional
  public Product create(String name, String sku, BigDecimal price, Integer stockQty) {
    assertSkuAvailable(sku);
    return productRepository.save(new Product(name, sku, price, stockQty));
  }

  @Transactional(readOnly = true)
  public Product getById(Long id) {
    return productRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
  }

  @Transactional(readOnly = true)
  public Page<Product> list(Pageable pageable) {
    return productRepository.findAll(pageable);
  }

  @Transactional
  public Product update(Long id, String name, String sku, BigDecimal price, Integer stockQty) {
    Product product = getById(id);
    if (productRepository.existsBySkuAndIdNot(sku, id)
        || productVariantRepository.existsBySku(sku)) {
      throw new DuplicateSkuException(sku);
    }
    product.update(name, sku, price, stockQty);
    return productRepository.save(product);
  }

  @Transactional
  public void delete(Long id) {
    if (!productRepository.existsById(id)) {
      throw new ResourceNotFoundException("Product not found: " + id);
    }
    if (productVariantRepository.existsByProductId(id)) {
      throw new ProductHasVariantsException();
    }
    productRepository.deleteById(id);
  }

  public void assertSkuAvailable(String sku) {
    if (productRepository.existsBySku(sku) || productVariantRepository.existsBySku(sku)) {
      throw new DuplicateSkuException(sku);
    }
  }
}
