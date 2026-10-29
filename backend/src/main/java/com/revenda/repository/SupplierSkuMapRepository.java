package com.revenda.repository;

import com.revenda.domain.SupplierSkuMap;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierSkuMapRepository extends JpaRepository<SupplierSkuMap, Long> {

  Optional<SupplierSkuMap> findBySupplierCnpjAndSupplierCode(
      String supplierCnpj, String supplierCode);

  boolean existsBySupplierCnpjAndSupplierCode(String supplierCnpj, String supplierCode);

  List<SupplierSkuMap> findAllByOrderByIdAsc();
}
