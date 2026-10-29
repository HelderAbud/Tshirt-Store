package com.revenda.service;

import com.revenda.domain.NfeImport;
import com.revenda.domain.ProductVariant;
import com.revenda.exception.NfeImportConflictException;
import com.revenda.exception.NfeXmlException;
import com.revenda.exception.ResourceNotFoundException;
import com.revenda.repository.NfeImportRepository;
import com.revenda.repository.ProductVariantRepository;
import com.revenda.repository.SupplierSkuMapRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class NfeImportService {

  private final NfeXmlParser parser;
  private final NfeImportRepository imports;
  private final ProductVariantRepository variants;
  private final SupplierSkuMapRepository supplierCodes;
  private final InventoryService inventoryService;
  private final String storeCnpj;

  public NfeImportService(
      NfeXmlParser parser,
      NfeImportRepository imports,
      ProductVariantRepository variants,
      SupplierSkuMapRepository supplierCodes,
      InventoryService inventoryService,
      @Value("${app.store.cnpj:}") String storeCnpj) {
    this.parser = parser;
    this.imports = imports;
    this.variants = variants;
    this.supplierCodes = supplierCodes;
    this.inventoryService = inventoryService;
    this.storeCnpj = digits(storeCnpj);
  }

  @Transactional
  public Result importXml(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new NfeXmlException("XML file is required");
    }
    if (file.getSize() > 1_000_000) {
      throw new NfeXmlException("XML file exceeds 1MB");
    }
    NfeXmlParser.Parsed parsed;
    try {
      parsed = parser.parse(file.getBytes());
    } catch (NfeXmlException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new NfeXmlException("Invalid NF-e XML");
    }
    NfeDirection direction = direction(parsed);
    if (imports.existsByAccessKey(parsed.accessKey())) {
      throw new NfeImportConflictException("NF-e already imported");
    }
    Map<String, Integer> qtyBySku = new LinkedHashMap<>();
    for (NfeXmlParser.Line line : parsed.lines()) {
      String sku = resolveSku(direction, parsed.emitterCnpj(), line.code());
      qtyBySku.merge(sku, line.qty(), Integer::sum);
    }
    List<ResultLine> lines = new ArrayList<>();
    for (Map.Entry<String, Integer> entry : qtyBySku.entrySet()) {
      ProductVariant variant =
          variants
              .findBySkuForUpdate(entry.getKey())
              .orElseThrow(
                  () -> new ResourceNotFoundException("Variant not found: " + entry.getKey()));
      int delta = direction == NfeDirection.ENTRADA ? entry.getValue() : -entry.getValue();
      if (variant.getStockQty() + delta < 0) {
        throw new NfeImportConflictException("Insufficient stock for " + entry.getKey());
      }
      lines.add(new ResultLine(entry.getKey(), entry.getValue(), delta, variant.getId()));
    }
    imports.save(new NfeImport(parsed.accessKey(), direction));
    for (ResultLine line : lines) {
      inventoryService.adjust(line.variantId(), line.delta(), "NF-e " + parsed.accessKey());
    }
    return new Result(
        parsed.accessKey(),
        direction.name(),
        lines.stream().map(line -> new Line(line.sku(), line.qty(), line.delta())).toList());
  }

  private NfeDirection direction(NfeXmlParser.Parsed parsed) {
    if (storeCnpj.length() != 14) {
      throw new NfeXmlException("Store CNPJ is not configured");
    }
    boolean emitter = storeCnpj.equals(parsed.emitterCnpj());
    boolean recipient = storeCnpj.equals(parsed.recipientCnpj());
    if (recipient && !emitter) {
      return NfeDirection.ENTRADA;
    }
    if (emitter && !recipient) {
      return NfeDirection.SAIDA;
    }
    throw new NfeXmlException("NF-e does not belong to the store");
  }

  private String resolveSku(NfeDirection direction, String emitterCnpj, String code) {
    if (direction == NfeDirection.SAIDA) {
      return code;
    }
    return supplierCodes
        .findBySupplierCnpjAndSupplierCode(emitterCnpj, code)
        .map(map -> map.getSku())
        .orElseThrow(() -> new ResourceNotFoundException("Supplier code not mapped: " + code));
  }

  private static String digits(String raw) {
    return raw == null ? "" : raw.replaceAll("\\D", "");
  }

  public record Result(String accessKey, String direction, List<Line> lines) {}

  public record Line(String sku, int qty, int delta) {}

  private record ResultLine(String sku, int qty, int delta, Long variantId) {}
}
