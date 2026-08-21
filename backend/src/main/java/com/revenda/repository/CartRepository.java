package com.revenda.repository;

import com.revenda.domain.Cart;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartRepository extends JpaRepository<Cart, Long> {

  @Query(
      """
      SELECT DISTINCT c FROM Cart c
      LEFT JOIN FETCH c.items i
      LEFT JOIN FETCH i.variant v
      LEFT JOIN FETCH v.product
      WHERE c.user.id = :userId
      """)
  Optional<Cart> findByUserIdWithItems(@Param("userId") Long userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT c FROM Cart c WHERE c.user.id = :userId")
  Optional<Cart> findByUserIdForUpdate(@Param("userId") Long userId);
}
