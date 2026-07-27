package com.revenda.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(nullable = false, unique = true, length = 100)
  private String sku;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal price;

  @Column(name = "stock_qty", nullable = false)
  private Integer stockQty;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Product() {}

  public Product(String name, String sku, BigDecimal price, Integer stockQty) {
    this.name = name;
    this.sku = sku;
    this.price = price;
    this.stockQty = stockQty;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getSku() {
    return sku;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public Integer getStockQty() {
    return stockQty;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void update(String name, String sku, BigDecimal price, Integer stockQty) {
    this.name = name;
    this.sku = sku;
    this.price = price;
    this.stockQty = stockQty;
    this.updatedAt = Instant.now();
  }
}
