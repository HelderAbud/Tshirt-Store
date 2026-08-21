package com.revenda.repository;

import com.revenda.domain.ProductVariant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

  boolean existsBySku(String sku);

  boolean existsByProduct_Id(Long productId);

  List<ProductVariant> findByProduct_Id(Long productId);
}
