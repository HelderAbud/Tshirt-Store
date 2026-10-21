package com.revenda.service;

import com.revenda.domain.NfeImport;
import com.revenda.domain.ProductVariant;
import com.revenda.exception.NfeImportConflictException;
import com.revenda.exception.NfeXmlException;
import com.revenda.exception.ResourceNotFoundException;
import com.revenda.repository.NfeImportRepository;
import com.revenda.repository.ProductVariantRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class NfeImportService {

  private final NfeXmlParser parser;
  private final NfeImportRepository imports;
  private final ProductVariantRepository variants;
  private final InventoryService inventoryService;

  public NfeImportService(
      NfeXmlParser parser,
      NfeImportRepository imports,
      ProductVariantRepository variants,
      InventoryService inventoryService) {
    this.parser = parser;
    this.imports = imports;
    this.variants = variants;
    this.inventoryService = inventoryService;
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
    if (imports.existsByAccessKey(parsed.accessKey())) {
      throw new NfeImportConflictException("NF-e already imported");
    }
    List<ResultLine> lines = new ArrayList<>();
    for (NfeXmlParser.Line line : parsed.lines()) {
      ProductVariant variant =
          variants
              .findBySkuForUpdate(line.sku())
              .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + line.sku()));
      int delta = parsed.direction() == NfeDirection.ENTRADA ? line.qty() : -line.qty();
      if (variant.getStockQty() + delta < 0) {
        throw new NfeImportConflictException("Insufficient stock for " + line.sku());
      }
      lines.add(new ResultLine(line.sku(), line.qty(), delta, variant.getId()));
    }
    imports.save(new NfeImport(parsed.accessKey(), parsed.direction()));
    for (ResultLine line : lines) {
      inventoryService.adjust(line.variantId(), line.delta(), "NF-e " + parsed.accessKey());
    }
    return new Result(
        parsed.accessKey(),
        parsed.direction().name(),
        lines.stream().map(line -> new Line(line.sku(), line.qty(), line.delta())).toList());
  }

  public record Result(String accessKey, String direction, List<Line> lines) {}

  public record Line(String sku, int qty, int delta) {}

  private record ResultLine(String sku, int qty, int delta, Long variantId) {}
}
