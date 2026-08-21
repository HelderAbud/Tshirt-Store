package com.revenda.repository;

import com.revenda.domain.ShopOrder;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {

  List<ShopOrder> findByUser_IdOrderByCreatedAtDesc(Long userId);

  @Query(
      """
      SELECT DISTINCT o FROM ShopOrder o
      LEFT JOIN FETCH o.items
      WHERE o.id = :id AND o.user.id = :userId
      """)
  Optional<ShopOrder> findByIdAndUserIdWithItems(
      @Param("id") Long id, @Param("userId") Long userId);

  Optional<ShopOrder> findByIdAndUser_Id(Long id, Long userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT o FROM ShopOrder o WHERE o.id = :id AND o.user.id = :userId")
  Optional<ShopOrder> findByIdAndUserIdForUpdate(
      @Param("id") Long id, @Param("userId") Long userId);
}
