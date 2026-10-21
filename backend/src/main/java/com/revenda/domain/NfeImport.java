package com.revenda.domain;

import com.revenda.service.NfeDirection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "nfe_imports")
public class NfeImport {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "access_key", nullable = false, unique = true, length = 44)
  private String accessKey;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 10)
  private NfeDirection direction;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected NfeImport() {}

  public NfeImport(String accessKey, NfeDirection direction) {
    this.accessKey = accessKey;
    this.direction = direction;
    this.createdAt = Instant.now();
  }

  public String getAccessKey() {
    return accessKey;
  }
}
