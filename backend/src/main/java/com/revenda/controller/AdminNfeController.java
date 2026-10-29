package com.revenda.controller;

import com.revenda.service.NfeImportService;
import com.revenda.service.SupplierSkuMapService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/nfe")
public class AdminNfeController {

  private final NfeImportService nfeImportService;
  private final SupplierSkuMapService supplierSkuMapService;

  public AdminNfeController(
      NfeImportService nfeImportService, SupplierSkuMapService supplierSkuMapService) {
    this.nfeImportService = nfeImportService;
    this.supplierSkuMapService = supplierSkuMapService;
  }

  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public NfeImportService.Result importNfe(@RequestParam("file") MultipartFile file) {
    return nfeImportService.importXml(file);
  }

  @PostMapping("/supplier-codes")
  @ResponseStatus(HttpStatus.CREATED)
  public SupplierSkuMapService.Code createSupplierCode(
      @Valid @RequestBody SupplierCodeRequest request) {
    return supplierSkuMapService.create(
        request.supplierCnpj(), request.supplierCode(), request.sku());
  }

  @GetMapping("/supplier-codes")
  public List<SupplierSkuMapService.Code> listSupplierCodes() {
    return supplierSkuMapService.list();
  }

  public record SupplierCodeRequest(
      @NotBlank @Pattern(regexp = "\\d{14}") String supplierCnpj,
      @NotBlank @Size(max = 60) String supplierCode,
      @NotBlank @Size(max = 100) String sku) {}
}
