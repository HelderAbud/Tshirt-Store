package com.revenda.service;

import com.revenda.exception.NfeXmlException;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Component
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
