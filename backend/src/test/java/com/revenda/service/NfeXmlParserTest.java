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
            xml("0", KEY, "SKU-A", "2", "SKU-A", "10.0000", "SKU-B", "1")
                .getBytes(StandardCharsets.UTF_8));

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
            () ->
                parser.parse(
                    xml("0", "1".repeat(43), "SKU-A", "1").getBytes(StandardCharsets.UTF_8)));
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
        assertThrows(
            NfeXmlException.class, () -> parser.parse(body.getBytes(StandardCharsets.UTF_8)));
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
