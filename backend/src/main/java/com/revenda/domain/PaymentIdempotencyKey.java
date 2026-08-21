package com.revenda.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "payment_idempotency_keys")
public class PaymentIdempotencyKey {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "idempotency_key", nullable = false, length = 128)
  private String idempotencyKey;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private ShopOrder order;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected PaymentIdempotencyKey() {}

  public PaymentIdempotencyKey(User user, String idempotencyKey, ShopOrder order) {
    this.user = user;
    this.idempotencyKey = idempotencyKey;
    this.order = order;
    this.createdAt = Instant.now();
  }

  public ShopOrder getOrder() {
    return order;
  }
}
