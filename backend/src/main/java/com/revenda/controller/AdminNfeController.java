package com.revenda.controller;

import com.revenda.service.NfeImportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/nfe")
public class AdminNfeController {

  private final NfeImportService nfeImportService;

  public AdminNfeController(NfeImportService nfeImportService) {
    this.nfeImportService = nfeImportService;
  }

  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public NfeImportService.Result importNfe(@RequestParam("file") MultipartFile file) {
    return nfeImportService.importXml(file);
  }
}
