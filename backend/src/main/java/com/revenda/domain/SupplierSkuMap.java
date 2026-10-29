package com.revenda.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "supplier_sku_maps",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_supplier_sku_maps",
            columnNames = {"supplier_cnpj", "supplier_code"}))
public class SupplierSkuMap {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "supplier_cnpj", nullable = false, length = 14)
  private String supplierCnpj;

  @Column(name = "supplier_code", nullable = false, length = 60)
  private String supplierCode;

  @Column(nullable = false, length = 100)
  private String sku;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected SupplierSkuMap() {}

  public SupplierSkuMap(String supplierCnpj, String supplierCode, String sku) {
    this.supplierCnpj = supplierCnpj;
    this.supplierCode = supplierCode;
    this.sku = sku;
    this.createdAt = Instant.now();
  }

  public String getSupplierCnpj() {
    return supplierCnpj;
  }

  public String getSupplierCode() {
    return supplierCode;
  }

  public String getSku() {
    return sku;
  }
}
