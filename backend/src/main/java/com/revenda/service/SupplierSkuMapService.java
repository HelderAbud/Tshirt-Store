package com.revenda.service;

import com.revenda.domain.SupplierSkuMap;
import com.revenda.exception.NfeImportConflictException;
import com.revenda.exception.ResourceNotFoundException;
import com.revenda.repository.ProductVariantRepository;
import com.revenda.repository.SupplierSkuMapRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierSkuMapService {

  private final SupplierSkuMapRepository maps;
  private final ProductVariantRepository variants;

  public SupplierSkuMapService(SupplierSkuMapRepository maps, ProductVariantRepository variants) {
    this.maps = maps;
    this.variants = variants;
  }

  @Transactional
  public Code create(String supplierCnpj, String supplierCode, String sku) {
    if (!variants.existsBySku(sku)) {
      throw new ResourceNotFoundException("Variant not found: " + sku);
    }
    if (maps.existsBySupplierCnpjAndSupplierCode(supplierCnpj, supplierCode)) {
      throw new NfeImportConflictException("Supplier code already mapped");
    }
    maps.save(new SupplierSkuMap(supplierCnpj, supplierCode, sku));
    return new Code(supplierCnpj, supplierCode, sku);
  }

  @Transactional(readOnly = true)
  public List<Code> list() {
    return maps.findAllByOrderByIdAsc().stream()
        .map(map -> new Code(map.getSupplierCnpj(), map.getSupplierCode(), map.getSku()))
        .toList();
  }

  public record Code(String supplierCnpj, String supplierCode, String sku) {}
}
