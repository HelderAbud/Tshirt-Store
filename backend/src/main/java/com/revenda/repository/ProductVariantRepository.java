package com.revenda.repository;

import com.revenda.domain.ProductVariant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

  boolean existsBySku(String sku);

  boolean existsByProductId(Long productId);

  List<ProductVariant> findByProductId(Long productId);
}
