# Importação de NF-e Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Importar um XML de NF-e já autorizado e aplicar o estoque de uma vez, com um endpoint ADMIN e uma tela no painel.

**Architecture:** `NfeXmlParser` lê só nomes locais do XML e devolve chave, direção e quantidades já somadas por SKU. `NfeImportService` grava `nfe_imports` e chama `InventoryService.adjust` na mesma transação, então qualquer falha desfaz a chave e o estoque. O painel ganha o botão NF-e em `frontend/app.js`.

**Tech Stack:** Java 21, Spring Boot 3.2, MockMvc, Flyway V10, H2 em teste, MySQL 8 em runtime, HTML/JS do painel, Node.js `node:test` só para o contrato da tela.

## Global Constraints

- `POST /api/admin/nfe/import`, multipart campo `file`, só ADMIN. XML até 1 MB. Arquivo ausente: 400.
- Ler por nome local. Ignorar namespace, assinatura e SEFAZ. Sem certificado, emissão, cancelamento, CC-e ou DANFE.
- `chNFe` com exatamente 44 dígitos. Fora isso: 400.
- `tpNF` `0` = `ENTRADA` (delta positivo), `1` = `SAIDA` (delta negativo). Outro valor: 400.
- `cProd` igual a `product_variants.sku`. Sem outro identificador.
- `qCom` inteiro positivo. `10.0000` vale 10. `1.5`, `0`, negativo ou texto: 400. Linhas do mesmo SKU somam antes de gravar. Sem itens: 400.
- 201: `{ accessKey, direction, lines: [{ sku, qty, delta }] }`.
- SKU desconhecido: 404, `detail` exatamente `Variant not found: {sku}`, nada gravado.
- Saída que deixaria estoque negativo: 409, `detail` exatamente `Insufficient stock for {sku}`, nada gravado.
- `chNFe` repetida: 409, `detail` exatamente `NF-e already imported`, estoque intacto.
- Movimento continua `ADJUSTMENT`. Motivo exatamente `NF-e {chNFe}`. Sem enum novo. `InventoryService.adjust` segue sendo o único caminho que mexe no estoque da variante e do produto pai.
- Tabela nova só em `V10__create_nfe_imports.sql`. Não editar V9.
- Painel: botão `NF-e` em `frontend/app.js`. `index.html` continua só o shell. Não editar `frontend/store.html` nem `frontend/store.js`.
- `docs/BACKLOG.md` marca F3-01 só depois do smoke com API no ar. Se o MySQL ou a API estiverem fora, o checkbox fica aberto.
- Commit, push e PR só se o usuário pedir nesta sessão. Os comandos abaixo ficam prontos para esse momento. Ramo, quando for versionar: `feature/nfe-import` a partir de `main` (este repositório não tem `develop`).
- Não versionar `.env`.

**Spec:** `docs/superpowers/specs/2026-10-18-nfe-import-design.md`

---

### Task 1: Parser do XML

**Files:**
- Create: `backend/src/main/java/com/revenda/service/NfeXmlParser.java`
- Create: `backend/src/main/java/com/revenda/service/NfeDirection.java`
- Create: `backend/src/main/java/com/revenda/exception/NfeXmlException.java`
- Test: `backend/src/test/java/com/revenda/service/NfeXmlParserTest.java`

**Interfaces:**
- Consumes: bytes do XML.
- Produces: `NfeXmlParser.Parsed` com `accessKey`, `NfeDirection` e `List<Line>` (`sku`, `qty`). `NfeXmlException` leva a mensagem do 400.

- [ ] **Step 1: Write the failing test**

`backend/src/test/java/com/revenda/service/NfeXmlParserTest.java`

```java
package com.revenda.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.revenda.exception.NfeXmlException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NfeXmlParserTest {

  private final NfeXmlParser parser = new NfeXmlParser();
  private static final String KEY = "1".repeat(44);

  @Test
  @DisplayName("sums the same SKU and treats 10.0000 as 10")
  void sumsSameSku() {
    NfeXmlParser.Parsed parsed =
        parser.parse(
            xml("0", KEY, "SKU-A", "2", "SKU-A", "10.0000", "SKU-B", "1").getBytes(StandardCharsets.UTF_8));

    assertEquals(KEY, parsed.accessKey());
    assertEquals(NfeDirection.ENTRADA, parsed.direction());
    assertEquals(2, parsed.lines().size());
    assertEquals("SKU-A", parsed.lines().get(0).sku());
    assertEquals(12, parsed.lines().get(0).qty());
    assertEquals("SKU-B", parsed.lines().get(1).sku());
    assertEquals(1, parsed.lines().get(1).qty());
  }

  @Test
  @DisplayName("tpNF 1 is outbound")
  void outbound() {
    NfeXmlParser.Parsed parsed =
        parser.parse(xml("1", KEY, "SKU-A", "3").getBytes(StandardCharsets.UTF_8));
    assertEquals(NfeDirection.SAIDA, parsed.direction());
  }

  @Test
  @DisplayName("rejects a key that is not 44 digits")
  void badKey() {
    NfeXmlException ex =
        assertThrows(
            NfeXmlException.class,
            () -> parser.parse(xml("0", "1".repeat(43), "SKU-A", "1").getBytes(StandardCharsets.UTF_8)));
    assertEquals("chNFe must have 44 digits", ex.getMessage());
  }

  @Test
  @DisplayName("rejects tpNF other than 0 or 1")
  void badDirection() {
    NfeXmlException ex =
        assertThrows(
            NfeXmlException.class,
            () -> parser.parse(xml("2", KEY, "SKU-A", "1").getBytes(StandardCharsets.UTF_8)));
    assertEquals("tpNF must be 0 or 1", ex.getMessage());
  }

  @Test
  @DisplayName("rejects a fractional quantity")
  void fractionalQty() {
    NfeXmlException ex =
        assertThrows(
            NfeXmlException.class,
            () -> parser.parse(xml("0", KEY, "SKU-A", "1.5").getBytes(StandardCharsets.UTF_8)));
    assertEquals("qCom must be a positive integer", ex.getMessage());
  }

  @Test
  @DisplayName("rejects zero, negative and text quantities")
  void badQuantities() {
    for (String qty : new String[] {"0", "-1", "abc"}) {
      assertThrows(
          NfeXmlException.class,
          () -> parser.parse(xml("0", KEY, "SKU-A", qty).getBytes(StandardCharsets.UTF_8)));
    }
  }

  @Test
  @DisplayName("rejects xml that is not a document")
  void invalidXml() {
    NfeXmlException ex =
        assertThrows(
            NfeXmlException.class, () -> parser.parse("<nfe".getBytes(StandardCharsets.UTF_8)));
    assertEquals("Invalid NF-e XML", ex.getMessage());
  }

  @Test
  @DisplayName("rejects an invoice with no items")
  void noItems() {
    String body =
        """
        <nfeProc xmlns="http://www.portalfiscal.inf.br/nfe">
          <NFe><infNFe Id="NFe%s"><ide><tpNF>0</tpNF></ide></infNFe></NFe>
          <protNFe><infProt><chNFe>%s</chNFe></infProt></protNFe>
        </nfeProc>
        """
            .formatted(KEY, KEY);
    NfeXmlException ex =
        assertThrows(NfeXmlException.class, () -> parser.parse(body.getBytes(StandardCharsets.UTF_8)));
    assertEquals("NF-e has no items", ex.getMessage());
  }

  private static String xml(String tpNf, String key, String... skuQty) {
    StringBuilder items = new StringBuilder();
    for (int i = 0; i < skuQty.length; i += 2) {
      items.append(
          "<det><prod><cProd>%s</cProd><qCom>%s</qCom></prod></det>"
              .formatted(skuQty[i], skuQty[i + 1]));
    }
    return """
        <nfeProc xmlns="http://www.portalfiscal.inf.br/nfe">
          <NFe><infNFe><ide><tpNF>%s</tpNF></ide>%s</infNFe></NFe>
          <protNFe><infProt><chNFe>%s</chNFe></infProt></protNFe>
        </nfeProc>
        """
        .formatted(tpNf, items, key);
  }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd backend && mvn -q -Dtest=NfeXmlParserTest test`

Expected: falha de compilação porque `NfeXmlParser` ainda não existe.

- [ ] **Step 3: Write the minimal implementation**

`backend/src/main/java/com/revenda/exception/NfeXmlException.java`

```java
package com.revenda.exception;

public class NfeXmlException extends RuntimeException {

  public NfeXmlException(String message) {
    super(message);
  }
}
```

`backend/src/main/java/com/revenda/service/NfeDirection.java`

```java
package com.revenda.service;

public enum NfeDirection {
  ENTRADA,
  SAIDA
}
```

`backend/src/main/java/com/revenda/service/NfeXmlParser.java`

```java
package com.revenda.service;

import com.revenda.exception.NfeXmlException;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class NfeXmlParser {

  public Parsed parse(byte[] xml) {
    Document document = document(xml);
    String accessKey = text(document, "chNFe");
    if (!accessKey.matches("\\d{44}")) {
      throw new NfeXmlException("chNFe must have 44 digits");
    }
    NfeDirection direction = direction(text(document, "tpNF"));
    List<Line> lines = lines(document);
    if (lines.isEmpty()) {
      throw new NfeXmlException("NF-e has no items");
    }
    return new Parsed(accessKey, direction, lines);
  }

  private Document document(byte[] xml) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
      document.getDocumentElement().normalize();
      return document;
    } catch (NfeXmlException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new NfeXmlException("Invalid NF-e XML");
    }
  }

  private NfeDirection direction(String tpNf) {
    if ("0".equals(tpNf)) {
      return NfeDirection.ENTRADA;
    }
    if ("1".equals(tpNf)) {
      return NfeDirection.SAIDA;
    }
    throw new NfeXmlException("tpNF must be 0 or 1");
  }

  private List<Line> lines(Document document) {
    Map<String, Integer> qtyBySku = new LinkedHashMap<>();
    NodeList dets = document.getElementsByTagNameNS("*", "det");
    for (int i = 0; i < dets.getLength(); i++) {
      Element det = (Element) dets.item(i);
      String sku = text(det, "cProd");
      if (sku.isBlank()) {
        throw new NfeXmlException("qCom must be a positive integer");
      }
      int qty = quantity(text(det, "qCom"));
      qtyBySku.merge(sku, qty, Integer::sum);
    }
    return qtyBySku.entrySet().stream().map(e -> new Line(e.getKey(), e.getValue())).toList();
  }

  private int quantity(String raw) {
    try {
      BigDecimal value = new BigDecimal(raw.trim());
      if (value.signum() <= 0 || value.stripTrailingZeros().scale() > 0) {
        throw new NfeXmlException("qCom must be a positive integer");
      }
      return value.intValueExact();
    } catch (NfeXmlException ex) {
      throw ex;
    } catch (RuntimeException ex) {
      throw new NfeXmlException("qCom must be a positive integer");
    }
  }

  private String text(Document document, String localName) {
    return text(document.getDocumentElement(), localName);
  }

  private String text(Element parent, String localName) {
    NodeList nodes = parent.getElementsByTagNameNS("*", localName);
    if (nodes.getLength() == 0 || nodes.item(0).getTextContent() == null) {
      return "";
    }
    return nodes.item(0).getTextContent().trim();
  }

  public record Parsed(String accessKey, NfeDirection direction, List<Line> lines) {}

  public record Line(String sku, int qty) {}
}
```

Remover o import `ArrayList` se o compilador/Spotless acusar import não usado. `lines` devolve `List` via `stream().toList()`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `cd backend && mvn -q -Dtest=NfeXmlParserTest test`

Expected: exit 0.

- [ ] **Step 5: Commit — só se o usuário pedir nesta sessão**

```bash
git add backend/src/main/java/com/revenda/exception/NfeXmlException.java backend/src/main/java/com/revenda/service/NfeDirection.java backend/src/main/java/com/revenda/service/NfeXmlParser.java backend/src/test/java/com/revenda/service/NfeXmlParserTest.java
git commit -m "test(nfe): parse authorized invoice xml"
```

---

### Task 2: Endpoint, tabela e estoque

**Files:**
- Create: `backend/src/main/resources/db/migration/V10__create_nfe_imports.sql`
- Create: `backend/src/main/java/com/revenda/domain/NfeImport.java`
- Create: `backend/src/main/java/com/revenda/repository/NfeImportRepository.java`
- Create: `backend/src/main/java/com/revenda/exception/NfeImportConflictException.java`
- Create: `backend/src/main/java/com/revenda/service/NfeImportService.java`
- Create: `backend/src/main/java/com/revenda/controller/AdminNfeController.java`
- Modify: `backend/src/main/java/com/revenda/repository/ProductVariantRepository.java` — `findBySkuForUpdate`
- Modify: `backend/src/main/java/com/revenda/exception/GlobalExceptionHandler.java` — 400, 409 e arquivo grande
- Modify: `backend/src/main/resources/application.yml` — limite de 1 MB
- Test: `backend/src/test/java/com/revenda/controller/AdminNfeControllerTest.java`

**Interfaces:**
- Consumes: `NfeXmlParser.Parsed`, `ProductVariantRepository.findBySkuForUpdate`, `InventoryService.adjust(variantId, delta, reason)`, `NfeImportRepository.existsByAccessKey`.
- Produces: `POST /api/admin/nfe/import` 201 `NfeImportService.Result`.

- [ ] **Step 1: Write the failing test**

`backend/src/test/java/com/revenda/controller/AdminNfeControllerTest.java`

```java
package com.revenda.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.revenda.domain.InventoryMovement;
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
    String key = "35" + String.format("%042d", Math.abs(sku.hashCode()));

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
    String key = "32" + String.format("%042d", Math.abs(sku.hashCode()));

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
    String key = "33" + String.format("%042d", Math.abs(sku.hashCode()));
    MockMultipartFile file = xmlFile("0", key, sku, "1");

    mockMvc
        .perform(
            multipart("/api/admin/nfe/import")
                .file(file)
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
    String key = "31" + String.format("%042d", Math.abs(sku.hashCode()));

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
    return new MockMultipartFile("file", "nfe.xml", "application/xml", body.getBytes(StandardCharsets.UTF_8));
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
            product.getResponse().getContentAsString().replaceAll("(?s).*\"id\"\\s*:\\s*(\\d+).*", "$1"));
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
        variant.getResponse().getContentAsString().replaceAll("(?s).*\"id\"\\s*:\\s*(\\d+).*", "$1"));
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
```

`InventoryMovement` precisa de `getReason()`. Se o campo se chama `reason` e o getter já existe, usar esse. Se o teste não compilar, abrir a entidade e usar o getter real — não criar outro campo.

A chave de 44 dígitos do teste `inbound` usa `String.format("%042d", ...)`. Se o valor estourar 42 dígitos, trocar por `String.format("%042d", Math.floorMod(sku.hashCode(), 1_000_000_000))` para manter exatamente 44 caracteres. O mesmo vale para as outras chaves geradas.

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd backend && mvn -q -Dtest=AdminNfeControllerTest test`

Expected: falha de compilação porque o controller ainda não existe.

- [ ] **Step 3: Write the minimal implementation**

`backend/src/main/resources/db/migration/V10__create_nfe_imports.sql`

```sql
CREATE TABLE nfe_imports (
    id BIGINT NOT NULL AUTO_INCREMENT,
    access_key VARCHAR(44) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_nfe_imports_access_key UNIQUE (access_key)
);
```

`backend/src/main/java/com/revenda/domain/NfeImport.java`

```java
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

  @Column(name = "created_at", nullable = false)
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
```

`backend/src/main/java/com/revenda/repository/NfeImportRepository.java`

```java
package com.revenda.repository;

import com.revenda.domain.NfeImport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NfeImportRepository extends JpaRepository<NfeImport, Long> {

  boolean existsByAccessKey(String accessKey);
}
```

Em `ProductVariantRepository`, acrescentar:

```java
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT v FROM ProductVariant v WHERE v.sku = :sku")
  Optional<ProductVariant> findBySkuForUpdate(@Param("sku") String sku);
```

`backend/src/main/java/com/revenda/exception/NfeImportConflictException.java`

```java
package com.revenda.exception;

public class NfeImportConflictException extends RuntimeException {

  public NfeImportConflictException(String message) {
    super(message);
  }
}
```

`backend/src/main/java/com/revenda/service/NfeImportService.java`

```java
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
              .orElseThrow(
                  () -> new ResourceNotFoundException("Variant not found: " + line.sku()));
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
```

`NfeXmlParser` precisa ser bean. Anotar a classe com `@Component`.

`backend/src/main/java/com/revenda/controller/AdminNfeController.java`

```java
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
```

Em `GlobalExceptionHandler`, acrescentar handlers que devolvem o `detail` da exceção:

- `NfeXmlException` → 400
- `NfeImportConflictException` → 409
- `MaxUploadSizeExceededException` → 400, detail `XML file exceeds 1MB`
- `MissingServletRequestPartException` → 400, detail `XML file is required`

Usar o mesmo formato Problem Details dos handlers que já existem. Não mudar a mensagem genérica de `InsufficientStockException` (`Insufficient stock`), porque o ajuste manual do painel depende dela.

Em `application.yml`, dentro de `spring:`:

```yaml
  servlet:
    multipart:
      max-file-size: 1MB
      max-request-size: 1MB
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `cd backend && mvn -q -Dtest=AdminNfeControllerTest,NfeXmlParserTest test && mvn -q spotless:check && mvn -q test`

Expected: os três comandos saem 0. Se o teste de 1 MB voltar 500, o handler de `MaxUploadSizeExceededException` não está sendo alcançado: o `if (file.getSize() > 1_000_000)` do serviço cobre o MockMvc, e o handler cobre o Tomcat. Ajustar só o caminho que falhou, sem subir o limite.

Se `spotless:check` falhar, rodar `mvn -q spotless:apply` e repetir o check.

- [ ] **Step 5: Commit — só se o usuário pedir nesta sessão**

```bash
git add backend/src/main/resources/db/migration/V10__create_nfe_imports.sql backend/src/main/java/com/revenda/domain/NfeImport.java backend/src/main/java/com/revenda/repository/NfeImportRepository.java backend/src/main/java/com/revenda/repository/ProductVariantRepository.java backend/src/main/java/com/revenda/exception/NfeImportConflictException.java backend/src/main/java/com/revenda/exception/GlobalExceptionHandler.java backend/src/main/java/com/revenda/service/NfeImportService.java backend/src/main/java/com/revenda/service/NfeXmlParser.java backend/src/main/java/com/revenda/controller/AdminNfeController.java backend/src/main/resources/application.yml backend/src/test/java/com/revenda/controller/AdminNfeControllerTest.java
git commit -m "feat(nfe): import an authorized invoice into stock"
```

---

### Task 3: Botão NF-e no painel

**Files:**
- Modify: `frontend/app.js` — botão, rota e upload
- Test: `frontend/nfe-page.test.mjs`
- Modify: `docs/BACKLOG.md` — F3-01 só depois do smoke

**Interfaces:**
- Consumes: `POST http://localhost:8084/api/admin/nfe/import` com `FormData` campo `file` e `Authorization: Bearer` de `tshirt_admin_token`.
- Produces: tela com chave e direção, ou o `detail` do erro.

- [ ] **Step 1: Write the failing test**

`frontend/nfe-page.test.mjs`

```javascript
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const app = readFileSync(new URL("./app.js", import.meta.url), "utf8");
const store = readFileSync(new URL("./store.js", import.meta.url), "utf8");

test("admin nav has an NF-e screen that posts the xml", () => {
  assert.match(app, /data-go="nfe"/);
  assert.match(app, /NF-e/);
  assert.match(app, /\/api\/admin\/nfe\/import/);
  assert.match(app, /FormData/);
  assert.match(app, /instanceof FormData/);
});

test("storefront stays untouched", () => {
  assert.equal(store.includes("/api/admin/nfe/import"), false);
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd frontend && node --test nfe-page.test.mjs`

Expected: FAIL porque `app.js` ainda não tem `data-go="nfe"`.

- [ ] **Step 3: Write the minimal implementation**

Em `api()`, não forçar JSON quando o corpo for `FormData`:

```javascript
  if (options.body && !(options.body instanceof FormData) && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json";
  }
```

No `shell()`, o terceiro botão da nav fica seguido de:

```html
<button class="nav-btn ${active === "nfe" ? "active" : ""}" data-go="nfe">NF-e</button>
```

Em `route()`:

```javascript
  if (name === "nfe") return renderNfe();
```

Nova função, no mesmo estilo de `renderInventory`:

```javascript
function renderNfe() {
  app.innerHTML = shell(
    "nfe",
    `
    <h2>NF-e</h2>
    <p class="muted">XML já autorizado. A chave repetida não mexe no estoque.</p>
    <form id="nfe-form" class="row">
      <input id="nfe-file" type="file" accept=".xml,text/xml,application/xml" required />
      <button type="submit">Importar</button>
    </form>
    <p id="msg" class="muted"></p>
  `,
  );
  bindNav();
  document.getElementById("nfe-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const msg = document.getElementById("msg");
    const input = document.getElementById("nfe-file");
    const body = new FormData();
    body.append("file", input.files[0]);
    msg.className = "muted";
    msg.textContent = "Importando…";
    try {
      const result = await api("/api/admin/nfe/import", { method: "POST", body });
      msg.className = "ok";
      msg.textContent = `${result.accessKey} · ${result.direction}`;
    } catch (error) {
      msg.className = "err";
      msg.textContent = error.message;
    }
  });
}
```

Não criar `nfe.html`. Não alterar `store.js`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `cd frontend && node --test nfe-page.test.mjs`

Expected: 2 testes passando.

- [ ] **Step 5: Smoke e checkbox**

Com a API em `http://localhost:8084` e o estático em `http://localhost:3000`:

1. Entrar no painel com `admin@demo.local` / `Demo12345!`.
2. Abrir NF-e e enviar um XML de entrada com um SKU que já existe. A tela mostra a chave e `ENTRADA`. O estoque dessa variante sobe.
3. Enviar o mesmo arquivo de novo. A tela mostra `NF-e already imported`. O estoque não muda.

Só então marcar em `docs/BACKLOG.md`:

```markdown
- [x] **F3-01** Import/emissão mínima + impacto em estoque
```

Se a API não estiver no ar, deixar o checkbox aberto e dizer isso no resumo.

- [ ] **Step 6: Commit — só se o usuário pedir nesta sessão**

```bash
git add frontend/app.js frontend/nfe-page.test.mjs docs/BACKLOG.md
git commit -m "feat(admin): upload an invoice xml from the backoffice"
```

Não incluir `docs/BACKLOG.md` neste commit se o checkbox não tiver sido marcado.
