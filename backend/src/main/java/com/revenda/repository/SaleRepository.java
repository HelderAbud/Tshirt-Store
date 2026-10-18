package com.revenda.repository;

import com.revenda.domain.Sale;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleRepository extends JpaRepository<Sale, Long> {

  @Query(
      """
      SELECT DISTINCT s FROM Sale s
      LEFT JOIN FETCH s.items i
      LEFT JOIN FETCH i.variant
      WHERE s.createdAt >= :from AND s.createdAt < :to
      ORDER BY s.createdAt DESC
      """)
  List<Sale> findByCreatedAtBetweenWithItems(@Param("from") Instant from, @Param("to") Instant to);
}
