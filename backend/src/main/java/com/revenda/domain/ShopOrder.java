package com.revenda.domain;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "orders")
public class ShopOrder {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 50)
  private OrderStatus status;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal total;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderItem> items = new ArrayList<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected ShopOrder() {}

  public ShopOrder(User user, OrderStatus status, BigDecimal total) {
    this.user = user;
    this.status = status;
    this.total = total;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  public Long getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public List<OrderItem> getItems() {
    return items;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void addItem(OrderItem item) {
    items.add(item);
  }

  public void setTotal(BigDecimal total) {
    this.total = total;
    this.updatedAt = Instant.now();
  }

  public void cancel() {
    this.status = OrderStatus.CANCELLED;
    this.updatedAt = Instant.now();
  }

  public void markPaid() {
    this.status = OrderStatus.PAID;
    this.updatedAt = Instant.now();
  }
}
