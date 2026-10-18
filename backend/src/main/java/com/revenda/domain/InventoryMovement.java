package com.revenda.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "inventory_movements")
public class InventoryMovement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "variant_id", nullable = false)
  private ProductVariant variant;

  @Column(nullable = false)
  private Integer delta;

  @Column(nullable = false, length = 255)
  private String reason;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(name = "movement_type", nullable = false, length = 32)
  private InventoryMovementType movementType;

  @Column(name = "reference_id")
  private Long referenceId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected InventoryMovement() {}

  public InventoryMovement(
      ProductVariant variant,
      Integer delta,
      String reason,
      InventoryMovementType movementType,
      Long referenceId) {
    this.variant = variant;
    this.delta = delta;
    this.reason = reason;
    this.movementType = movementType;
    this.referenceId = referenceId;
    this.createdAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public ProductVariant getVariant() {
    return variant;
  }

  public Integer getDelta() {
    return delta;
  }

  public String getReason() {
    return reason;
  }

  public InventoryMovementType getMovementType() {
    return movementType;
  }

  public Long getReferenceId() {
    return referenceId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
