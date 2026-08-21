package com.revenda.repository;

import com.revenda.domain.PaymentIdempotencyKey;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentIdempotencyKeyRepository
    extends JpaRepository<PaymentIdempotencyKey, Long> {

  Optional<PaymentIdempotencyKey> findByUser_IdAndIdempotencyKey(
      Long userId, String idempotencyKey);
}
