package com.revenda.repository;

import com.revenda.domain.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

  boolean existsBySku(String sku);

  boolean existsBySkuAndIdNot(String sku, Long id);

  Optional<Product> findBySku(String sku);
}
