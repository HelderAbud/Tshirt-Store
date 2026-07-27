package com.revenda.service;

import com.revenda.domain.Product;
import com.revenda.exception.DuplicateSkuException;
import com.revenda.exception.ResourceNotFoundException;
import com.revenda.repository.ProductRepository;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

  private final ProductRepository productRepository;

  public ProductService(ProductRepository productRepository) {
    this.productRepository = productRepository;
  }

  @Transactional
  public Product create(String name, String sku, BigDecimal price, Integer stockQty) {
    if (productRepository.existsBySku(sku)) {
      throw new DuplicateSkuException(sku);
    }
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
    if (productRepository.existsBySkuAndIdNot(sku, id)) {
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
    productRepository.deleteById(id);
  }
}
