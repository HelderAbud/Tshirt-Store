package com.revenda.repository;

import com.revenda.domain.ProductVariant;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

  boolean existsBySku(String sku);

  boolean existsByProduct_Id(Long productId);

  List<ProductVariant> findByProduct_Id(Long productId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT v FROM ProductVariant v WHERE v.id = :id")
  Optional<ProductVariant> findByIdForUpdate(@Param("id") Long id);
}
