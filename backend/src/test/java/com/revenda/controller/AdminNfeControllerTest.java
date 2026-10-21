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
    String key = keyOf("35", sku);

    mockMvc
        .perform(
            multipart("/api/admin/nfe/import")
                .file(xmlFile("0", key, sku, "2.0000", sku, "3"))
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
                .file(xmlFile("1", key, sku, "4"))
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
    String key = keyOf("33", sku);

    mockMvc
        .perform(
            multipart("/api/admin/nfe/import")
                .file(xmlFile("0", key, sku, "1"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            multipart("/api/admin/nfe/import")
                .file(xmlFile("0", key, sku, "1"))
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
                .file(xmlFile("0", key, "SKU-QUE-NAO-EXISTE", "1"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Variant not found: SKU-QUE-NAO-EXISTE"));

    String sku = "NFE-AFTER-404-" + System.nanoTime();
    createVariant(sku, 1);
    mockMvc
        .perform(
            multipart("/api/admin/nfe/import")
                .file(xmlFile("0", key, sku, "1"))
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
                .file(xmlFile("1", key, sku, "2"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("Insufficient stock for " + sku));

    assertEquals(1, stock(sku));
    mockMvc
        .perform(
            multipart("/api/admin/nfe/import")
                .file(xmlFile("0", key, sku, "1"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("customer cannot import")
  void customerForbidden() throws Exception {
    mockMvc
        .perform(
            multipart("/api/admin/nfe/import")
                .file(xmlFile("0", "1".repeat(44), "SKU", "1"))
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

  private static String keyOf(String prefix, String sku) {
    int value = Math.floorMod(sku.hashCode(), 1_000_000_000);
    return prefix + String.format("%042d", value);
  }

  private MockMultipartFile xmlFile(String tpNf, String key, String... skuQty) {
    StringBuilder items = new StringBuilder();
    for (int i = 0; i < skuQty.length; i += 2) {
      items.append(
          "<det><prod><cProd>%s</cProd><qCom>%s</qCom></prod></det>"
              .formatted(skuQty[i], skuQty[i + 1]));
    }
    String body =
        """
        <nfeProc xmlns="http://www.portalfiscal.inf.br/nfe">
          <NFe><infNFe><ide><tpNF>%s</tpNF></ide>%s</infNFe></NFe>
          <protNFe><infProt><chNFe>%s</chNFe></infProt></protNFe>
        </nfeProc>
        """
            .formatted(tpNf, items, key);
    return new MockMultipartFile(
        "file", "nfe.xml", "application/xml", body.getBytes(StandardCharsets.UTF_8));
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
