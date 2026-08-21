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
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "order_items")
public class OrderItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private ShopOrder order;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "variant_id", nullable = false)
  private ProductVariant variant;

  @Column(nullable = false, length = 100)
  private String sku;

  @Column(name = "product_name", nullable = false, length = 255)
  private String productName;

  @Column(nullable = false, length = 64)
  private String color;

  @Column(nullable = false, length = 32)
  private String size;

  @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal unitPrice;

  @Column(nullable = false)
  private Integer qty;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected OrderItem() {}

  public OrderItem(
      ShopOrder order,
      ProductVariant variant,
      String sku,
      String productName,
      String color,
      String size,
      BigDecimal unitPrice,
      Integer qty) {
    this.order = order;
    this.variant = variant;
    this.sku = sku;
    this.productName = productName;
    this.color = color;
    this.size = size;
    this.unitPrice = unitPrice;
    this.qty = qty;
    this.createdAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public ProductVariant getVariant() {
    return variant;
  }

  public String getSku() {
    return sku;
  }

  public String getProductName() {
    return productName;
  }

  public String getColor() {
    return color;
  }

  public String getSize() {
    return size;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public Integer getQty() {
    return qty;
  }
}
