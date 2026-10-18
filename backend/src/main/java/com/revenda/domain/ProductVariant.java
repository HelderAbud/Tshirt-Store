package com.revenda.domain;

import com.revenda.exception.InsufficientStockException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "product_variants")
public class ProductVariant {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(nullable = false, length = 64)
  private String color;

  @Column(nullable = false, length = 32)
  private String size;

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

  protected ProductVariant() {}

  public ProductVariant(
      Product product, String color, String size, String sku, BigDecimal price, Integer stockQty) {
    this.product = product;
    this.color = color;
    this.size = size;
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

  public Long getProductId() {
    return product == null ? null : product.getId();
  }

  public Product getProduct() {
    return product;
  }

  public String getColor() {
    return color;
  }

  public String getSize() {
    return size;
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

  public void decreaseStock(int qty) {
    if (qty > this.stockQty) {
      throw new InsufficientStockException();
    }
    this.stockQty = this.stockQty - qty;
    this.updatedAt = Instant.now();
  }

  public void adjustStock(int delta) {
    int next = this.stockQty + delta;
    if (next < 0) {
      throw new InsufficientStockException();
    }
    this.stockQty = next;
    this.updatedAt = Instant.now();
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
