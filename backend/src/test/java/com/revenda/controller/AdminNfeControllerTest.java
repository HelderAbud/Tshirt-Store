package com.revenda.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.revenda.domain.InventoryMovementType;
import com.revenda.domain.User;
import com.revenda.domain.UserRole;
import com.revenda.repository.InventoryMovementRepository;
import com.revenda.service.JwtService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminNfeControllerTest {

  private static final String STORE = "12345678000195";
  private static final String SUPPLIER = "99888777000166";

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtService jwtService;
  @Autowired private InventoryMovementRepository movements;

  private String adminToken() {
    return jwtService.generateToken(new User("admin-nfe@example.com", "encoded", UserRole.ADMIN));
  }

  private String customerToken() {
    return jwtService.generateToken(
        new User("customer-nfe@example.com", "encoded", UserRole.CUSTOMER));
  }

  @Test
  @DisplayName("imports an inbound invoice, sums the SKU and records an adjustment")
  void inbound() throws Exception {
    String sku = "NFE-IN-" + System.nanoTime();
    createVariant(sku, 4);
    map(SUPPLIER, sku, sku);
    String key = keyOf("35", sku);

    mockMvc
        .perform(
            multipart("/api/admin/nfe/import")
                .file(xmlFile(SUPPLIER, STORE, key, null, sku, "2.0000", sku, "3"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accessKey").value(key))
        .andExpect(jsonPath("$.direction").value("ENTRADA"))
        .andExpect(jsonPath("$.lines[0].sku").value(sku))
        .andExpect(jsonPath("$.lines[0].qty").value(5))
        .andExpect(jsonPath("$.lines[0].delta").value(5));

    assertEquals(9, stock(sku));
    assertTrue(
        movements.findAll().stream()
            .anyMatch(
                movement ->
                    movement.getMovementType() == InventoryMovementType.ADJUSTMENT
                        && ("NF-e " + key).equals(movement.getReason())));
  }

  @Test
  @DisplayName("outbound import decreases stock")
  void outbound() throws Exception {
    String sku = "NFE-OUT-" + System.nanoTime();
    createVariant(sku, 10);
    String key = keyOf("32", sku);

    mockMvc
        .perform(
            multipart("/api/admin/nfe/import")
                .file(xmlFile(STORE, SUPPLIER, key, null, sku, "4"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.direction").value("SAIDA"))
        .andExpect(jsonPath("$.lines[0].delta").value(-4));

    assertEquals(6, stock(sku));
  }

  @Test
  @DisplayName("duplicate key does not change stock")
  void duplicate() throws Exception {
    String sku = "NFE-DUP-" + System.nanoTime();
    createVariant(sku, 3);
    map(SUPPLIER, sku, sku);
    String key = keyOf("33", sku);

    mockMvc
        .perform(
            multipart("/api/admin/nfe/import")
                .file(xmlFile(SUPPLIER, STORE, key, null, sku, "1"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            multipart("/api/admin/nfe/import")
                .file(xmlFile(SUPPLIER, STORE, key, null, sku, "1"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("NF-e already imported"));

    assertEquals(4, stock(sku));
  }

  @Test
  @DisplayName("unknown SKU saves nothing")
  void unknownSku() throws Exception {
    String key = "34" + "0".repeat(42);
    mockMvc
        .perform(
            multipart("/api/admin/nfe/import")
                .file(xmlFile(STORE, SUPPLIER, key, null, "SKU-QUE-NAO-EXISTE", "1"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Variant not found: SKU-QUE-NAO-EXISTE"));

    String sku = "NFE-AFTER-404-" + System.nanoTime();
    createVariant(sku, 1);
    mockMvc
        .perform(
            multipart("/api/admin/nfe/import")
                .file(xmlFile(STORE, SUPPLIER, key, null, sku, "1"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("insufficient stock saves nothing")
  void insufficient() throws Exception {
    String sku = "NFE-LOW-" + System.nanoTime();
    createVariant(sku, 1);
    String key = keyOf("31", sku);

    mockMvc
        .perform(
            multipart("/api/admin/nfe/import")
                .file(xmlFile(STORE, SUPPLIER, key, null, sku, "2"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("Insufficient stock for " + sku));

    assertEquals(1, stock(sku));
    mockMvc
        .perform(
            multipart("/api/admin/nfe/import")
                .file(xmlFile(STORE, SUPPLIER, key, null, sku, "1"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("customer cannot import")
  void customerForbidden() throws Exception {
    mockMvc
        .perform(
            multipart("/api/admin/nfe/import")
                .file(xmlFile(SUPPLIER, STORE, "1".repeat(44), null, "SKU", "1"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken()))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("missing file is 400")
  void missingFile() throws Exception {
    mockMvc
        .perform(
            multipart("/api/admin/nfe/import")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("XML file is required"));
  }

  @Test
  @DisplayName("file over 1MB is 400")
  void tooLarge() throws Exception {
    byte[] big = new byte[1_000_001];
    MockMultipartFile file = new MockMultipartFile("file", "big.xml", "application/xml", big);
    mockMvc
        .perform(
            multipart("/api/admin/nfe/import")
                .file(file)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("XML file exceeds 1MB"));
  }

  @Test
  @DisplayName("supplier tpNF 1 still increases stock when the store is the recipient")
  void supplierOutboundIsStoreInbound() throws Exception {
    String sku = "NFE-TPNF-" + System.nanoTime();
    String code = "FORN-" + sku;
    createVariant(sku, 2);
    map(SUPPLIER, code, sku);
    String key = keyOf("36", sku);

    mockMvc
        .perform(
            multipart("/api/admin/nfe/import")
                .file(xmlFile(SUPPLIER, STORE, key, "1", code, "3"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.direction").value("ENTRADA"))
        .andExpect(jsonPath("$.lines[0].sku").value(sku))
        .andExpect(jsonPath("$.lines[0].delta").value(3));

    assertEquals(5, stock(sku));
  }

  @Test
  @DisplayName("inbound without a supplier map saves nothing")
  void unmappedSupplierCode() throws Exception {
    String sku = "NFE-NOMAP-" + System.nanoTime();
    createVariant(sku, 2);
    String key = keyOf("37", sku);

    mockMvc
        .perform(
            multipart("/api/admin/nfe/import")
                .file(xmlFile(SUPPLIER, STORE, key, null, "CODIGO-FORN", "1"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Supplier code not mapped: CODIGO-FORN"));

    assertEquals(2, stock(sku));
  }

  @Test
  @DisplayName("invoice that is not the store's is rejected")
  void invoiceNotForStore() throws Exception {
    String key = keyOf("38", "fora");
    mockMvc
        .perform(
            multipart("/api/admin/nfe/import")
                .file(xmlFile("11111111000191", "22222222000191", key, null, "SKU", "1"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("NF-e does not belong to the store"));

    mockMvc
        .perform(
            multipart("/api/admin/nfe/import")
                .file(xmlFile(STORE, STORE, keyOf("40", "ambos"), null, "SKU", "1"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("NF-e does not belong to the store"));
  }

  @Test
  @DisplayName("empty product code is 400")
  void emptyProductCode() throws Exception {
    String key = keyOf("39", "vazio");
    mockMvc
        .perform(
            multipart("/api/admin/nfe/import")
                .file(xmlFile(SUPPLIER, STORE, key, null, " ", "1"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("cProd is required"));
  }

  @Test
  @DisplayName("admin maps a supplier code and can list it")
  void mapSupplierCode() throws Exception {
    String sku = "NFE-MAP-" + System.nanoTime();
    createVariant(sku, 1);

    mockMvc
        .perform(
            post("/api/admin/nfe/supplier-codes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"supplierCnpj":"%s","supplierCode":"COD-1","sku":"%s"}
                    """
                        .formatted(SUPPLIER, sku)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.supplierCode").value("COD-1"))
        .andExpect(jsonPath("$.sku").value(sku));

    mockMvc
        .perform(
            get("/api/admin/nfe/supplier-codes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.sku == '%s')].supplierCode".formatted(sku)).value("COD-1"));
  }

  @Test
  @DisplayName("duplicate supplier code and unknown sku are rejected")
  void mapConflicts() throws Exception {
    String sku = "NFE-MAPDUP-" + System.nanoTime();
    createVariant(sku, 1);
    map(SUPPLIER, "COD-DUP", sku);

    mockMvc
        .perform(
            post("/api/admin/nfe/supplier-codes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"supplierCnpj":"%s","supplierCode":"COD-DUP","sku":"%s"}
                    """
                        .formatted(SUPPLIER, sku)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("Supplier code already mapped"));

    mockMvc
        .perform(
            post("/api/admin/nfe/supplier-codes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"supplierCnpj":"%s","supplierCode":"OUTRO","sku":"SKU-NENHUM"}
                    """
                        .formatted(SUPPLIER)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Variant not found: SKU-NENHUM"));

    mockMvc
        .perform(
            post("/api/admin/nfe/supplier-codes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"supplierCnpj":"123","supplierCode":"X","sku":"%s"}
                    """
                        .formatted(sku)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Validation failed"));
  }

  @Test
  @DisplayName("customer cannot map a supplier code")
  void customerCannotMap() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/nfe/supplier-codes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"supplierCnpj":"%s","supplierCode":"X","sku":"SKU"}
                    """
                        .formatted(SUPPLIER)))
        .andExpect(status().isForbidden());
  }

  private static String keyOf(String prefix, String sku) {
    int value = Math.floorMod(sku.hashCode(), 1_000_000_000);
    return prefix + String.format("%042d", value);
  }

  private MockMultipartFile xmlFile(
      String emit, String dest, String key, String tpNf, String... codeQty) {
    StringBuilder items = new StringBuilder();
    for (int i = 0; i < codeQty.length; i += 2) {
      items.append(
          "<det><prod><cProd>%s</cProd><qCom>%s</qCom></prod></det>"
              .formatted(codeQty[i], codeQty[i + 1]));
    }
    String ide = tpNf == null || tpNf.isBlank() ? "" : "<ide><tpNF>" + tpNf + "</tpNF></ide>";
    String body =
        """
        <nfeProc xmlns="http://www.portalfiscal.inf.br/nfe">
          <NFe><infNFe>%s<emit><CNPJ>%s</CNPJ></emit><dest><CNPJ>%s</CNPJ></dest>%s</infNFe></NFe>
          <protNFe><infProt><chNFe>%s</chNFe></infProt></protNFe>
        </nfeProc>
        """
            .formatted(ide, emit, dest, items, key);
    return new MockMultipartFile(
        "file", "nfe.xml", "application/xml", body.getBytes(StandardCharsets.UTF_8));
  }

  private void map(String supplierCnpj, String supplierCode, String sku) throws Exception {
    mockMvc
        .perform(
            post("/api/admin/nfe/supplier-codes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"supplierCnpj":"%s","supplierCode":"%s","sku":"%s"}
                    """
                        .formatted(supplierCnpj, supplierCode, sku)))
        .andExpect(status().isCreated());
  }

  private Long createVariant(String sku, int stock) throws Exception {
    MvcResult product =
        mockMvc
            .perform(
                post("/api/admin/products")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Camiseta NFe","sku":"P-%s","price":40.00,"stockQty":0}
                        """
                            .formatted(sku)))
            .andExpect(status().isCreated())
            .andReturn();
    Long productId =
        Long.parseLong(
            product
                .getResponse()
                .getContentAsString()
                .replaceAll("(?s).*\"id\"\\s*:\\s*(\\d+).*", "$1"));
    MvcResult variant =
        mockMvc
            .perform(
                post("/api/admin/products/" + productId + "/variants")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"color":"Azul","size":"M","sku":"%s","price":40.00,"stockQty":%d}
                        """
                            .formatted(sku, stock)))
            .andExpect(status().isCreated())
            .andReturn();
    return Long.parseLong(
        variant
            .getResponse()
            .getContentAsString()
            .replaceAll("(?s).*\"id\"\\s*:\\s*(\\d+).*", "$1"));
  }

  private int stock(String sku) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/admin/inventory/stock")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
            .andExpect(status().isOk())
            .andReturn();
    String body = result.getResponse().getContentAsString();
    int at = body.indexOf("\"sku\":\"" + sku + "\"");
    int qtyAt = body.indexOf("\"stockQty\":", at);
    int end = body.indexOf(",", qtyAt);
    return Integer.parseInt(body.substring(qtyAt + "\"stockQty\":".length(), end).trim());
  }
}
