package com.revenda.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.revenda.domain.Product;
import com.revenda.domain.ProductVariant;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryTest {

  @Autowired private ProductRepository productRepository;
  @Autowired private ProductVariantRepository productVariantRepository;
  @Autowired private EntityManager entityManager;

  @Test
  @DisplayName("findByIdWithVariants carrega variantes em uma query")
  void shouldFetchVariantsInSingleQuery() {
    Product product =
        productRepository.save(new Product("Camiseta", "SKU-JOIN-P", new BigDecimal("10.00"), 3));
    productVariantRepository.save(
        new ProductVariant(product, "Preto", "P", "SKU-JOIN-V", new BigDecimal("12.00"), 1));
    entityManager.flush();
    entityManager.clear();

    Statistics statistics = statistics();
    statistics.clear();

    Product found = productRepository.findByIdWithVariants(product.getId()).orElseThrow();
    int variantCount = found.getVariants().size();

    assertThat(Hibernate.isInitialized(found.getVariants())).isTrue();
    assertThat(variantCount).isEqualTo(1);
    assertThat(found.getVariants().get(0).getSku()).isEqualTo("SKU-JOIN-V");
    assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("findById não inicializa variantes")
  void shouldNotInitializeVariantsOnFindById() {
    Product product =
        productRepository.save(new Product("Camiseta", "SKU-LAZY-P", new BigDecimal("10.00"), 3));
    productVariantRepository.save(
        new ProductVariant(product, "Preto", "P", "SKU-LAZY-V", new BigDecimal("12.00"), 1));
    entityManager.flush();
    entityManager.clear();

    Product found = productRepository.findById(product.getId()).orElseThrow();

    assertThat(Hibernate.isInitialized(found.getVariants())).isFalse();
  }

  private Statistics statistics() {
    SessionFactory sessionFactory =
        entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
    Statistics statistics = sessionFactory.getStatistics();
    statistics.setStatisticsEnabled(true);
    return statistics;
  }
}
