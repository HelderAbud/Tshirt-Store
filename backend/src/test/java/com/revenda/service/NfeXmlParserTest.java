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
  @DisplayName("sums the same product code and reads both parties")
  void sumsSameCode() {
    NfeXmlParser.Parsed parsed =
        parser.parse(
            xml(
                    "99888777000166",
                    "12.345.678/0001-95",
                    KEY,
                    "SKU-A",
                    "2",
                    "SKU-A",
                    "10.0000",
                    "SKU-B",
                    "1")
                .getBytes(StandardCharsets.UTF_8));

    assertEquals(KEY, parsed.accessKey());
    assertEquals("99888777000166", parsed.emitterCnpj());
    assertEquals("12345678000195", parsed.recipientCnpj());
    assertEquals(2, parsed.lines().size());
    assertEquals("SKU-A", parsed.lines().get(0).code());
    assertEquals(12, parsed.lines().get(0).qty());
    assertEquals("SKU-B", parsed.lines().get(1).code());
    assertEquals(1, parsed.lines().get(1).qty());
  }

  @Test
  @DisplayName("rejects an empty product code")
  void emptyCode() {
    NfeXmlException ex =
        assertThrows(
            NfeXmlException.class,
            () ->
                parser.parse(
                    xml("99888777000166", "12345678000195", KEY, " ", "1")
                        .getBytes(StandardCharsets.UTF_8)));
    assertEquals("cProd is required", ex.getMessage());
  }

  @Test
  @DisplayName("rejects a key that is not 44 digits")
  void badKey() {
    NfeXmlException ex =
        assertThrows(
            NfeXmlException.class,
            () ->
                parser.parse(
                    xml("99888777000166", "12345678000195", "1".repeat(43), "SKU-A", "1")
                        .getBytes(StandardCharsets.UTF_8)));
    assertEquals("chNFe must have 44 digits", ex.getMessage());
  }

  @Test
  @DisplayName("rejects a fractional quantity")
  void fractionalQty() {
    NfeXmlException ex =
        assertThrows(
            NfeXmlException.class,
            () ->
                parser.parse(
                    xml("99888777000166", "12345678000195", KEY, "SKU-A", "1.5")
                        .getBytes(StandardCharsets.UTF_8)));
    assertEquals("qCom must be a positive integer", ex.getMessage());
  }

  @Test
  @DisplayName("rejects zero, negative and text quantities")
  void badQuantities() {
    for (String qty : new String[] {"0", "-1", "abc"}) {
      assertThrows(
          NfeXmlException.class,
          () ->
              parser.parse(
                  xml("99888777000166", "12345678000195", KEY, "SKU-A", qty)
                      .getBytes(StandardCharsets.UTF_8)));
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
          <NFe><infNFe Id="NFe%s"><emit><CNPJ>%s</CNPJ></emit></infNFe></NFe>
          <protNFe><infProt><chNFe>%s</chNFe></infProt></protNFe>
        </nfeProc>
        """
            .formatted(KEY, "99888777000166", KEY);
    NfeXmlException ex =
        assertThrows(
            NfeXmlException.class, () -> parser.parse(body.getBytes(StandardCharsets.UTF_8)));
    assertEquals("NF-e has no items", ex.getMessage());
  }

  private static String xml(String emit, String dest, String key, String... codeQty) {
    StringBuilder items = new StringBuilder();
    for (int i = 0; i < codeQty.length; i += 2) {
      items.append(
          "<det><prod><cProd>%s</cProd><qCom>%s</qCom></prod></det>"
              .formatted(codeQty[i], codeQty[i + 1]));
    }
    return """
        <nfeProc xmlns="http://www.portalfiscal.inf.br/nfe">
          <NFe><infNFe>
            <emit><CNPJ>%s</CNPJ></emit>
            <dest><CNPJ>%s</CNPJ></dest>
            %s
          </infNFe></NFe>
          <protNFe><infProt><chNFe>%s</chNFe></infProt></protNFe>
        </nfeProc>
        """
        .formatted(emit, dest, items, key);
  }
}
