package com.revenda.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.revenda.domain.User;
import com.revenda.domain.UserRole;
import com.revenda.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminProductVariantControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtService jwtService;

  private String adminToken() {
    return jwtService.generateToken(
        new User("admin-variant@example.com", "encoded", UserRole.ADMIN));
  }

  private String customerToken() {
    return jwtService.generateToken(
        new User("customer-variant@example.com", "encoded", UserRole.CUSTOMER));
  }

  private Long createProduct(String sku) throws Exception {
    MvcResult created =
        mockMvc
            .perform(
                post("/api/admin/products")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "Camiseta Base",
                          "sku": "%s",
                          "price": 49.90,
                          "stockQty": 10
                        }
                        """
                            .formatted(sku)))
            .andExpect(status().isCreated())
            .andReturn();
    String response = created.getResponse().getContentAsString();
    return Long.parseLong(response.replaceAll("(?s).*\"id\"\\s*:\\s*(\\d+).*", "$1"));
  }

  @Test
  @DisplayName("ADMIN cria variante com sucesso")
  void shouldCreateVariantAsAdmin() throws Exception {
    Long productId = createProduct("TSHIRT-VAR-BASE-01");

    mockMvc
        .perform(
            post("/api/admin/products/" + productId + "/variants")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "color": "Preto",
                      "size": "M",
                      "sku": "TSHIRT-BLK-M-01",
                      "price": 59.90,
                      "stockQty": 4
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.productId").value(productId))
        .andExpect(jsonPath("$.color").value("Preto"))
        .andExpect(jsonPath("$.size").value("M"))
        .andExpect(jsonPath("$.sku").value("TSHIRT-BLK-M-01"))
        .andExpect(jsonPath("$.price").value(59.90))
        .andExpect(jsonPath("$.stockQty").value(4));
  }

  @Test
  @DisplayName("POST variante sem token retorna 401")
  void shouldReturnUnauthorizedWithoutToken() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/products/1/variants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "color": "Preto",
                      "size": "M",
                      "sku": "TSHIRT-NO-AUTH",
                      "price": 10.00,
                      "stockQty": 1
                    }
                    """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("CUSTOMER não cria variante")
  void shouldReturnForbiddenForCustomer() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/products/1/variants")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "color": "Preto",
                      "size": "M",
                      "sku": "TSHIRT-CUST",
                      "price": 10.00,
                      "stockQty": 1
                    }
                    """))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Variante em produto inexistente retorna 404")
  void shouldReturnNotFoundWhenProductMissing() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/products/999999/variants")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "color": "Preto",
                      "size": "M",
                      "sku": "TSHIRT-MISS",
                      "price": 10.00,
                      "stockQty": 1
                    }
                    """))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("SKU de variante duplicado retorna 409")
  void shouldReturnConflictOnDuplicateVariantSku() throws Exception {
    Long productId = createProduct("TSHIRT-VAR-BASE-DUP");
    String body =
        """
        {
          "color": "Azul",
          "size": "P",
          "sku": "TSHIRT-BLU-P-DUP",
          "price": 39.90,
          "stockQty": 2
        }
        """;

    mockMvc
        .perform(
            post("/api/admin/products/" + productId + "/variants")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/admin/products/" + productId + "/variants")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("Preço negativo na variante retorna 400")
  void shouldReturnBadRequestOnNegativePrice() throws Exception {
    Long productId = createProduct("TSHIRT-VAR-BASE-400");

    mockMvc
        .perform(
            post("/api/admin/products/" + productId + "/variants")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "color": "Verde",
                      "size": "G",
                      "sku": "TSHIRT-GRN-G",
                      "price": -1.00,
                      "stockQty": 1
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("ADMIN lista variantes do produto")
  void shouldListVariantsForProduct() throws Exception {
    Long productId = createProduct("TSHIRT-VAR-BASE-LIST");

    mockMvc
        .perform(
            post("/api/admin/products/" + productId + "/variants")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "color": "Branco",
                      "size": "G",
                      "sku": "TSHIRT-WHT-G-LIST",
                      "price": 44.90,
                      "stockQty": 3
                    }
                    """))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            get("/api/admin/products/" + productId + "/variants")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].sku").value("TSHIRT-WHT-G-LIST"))
        .andExpect(jsonPath("$[0].color").value("Branco"));
  }

  @Test
  @DisplayName("Não apaga produto que ainda tem variantes")
  void shouldConflictWhenDeletingProductWithVariants() throws Exception {
    Long productId = createProduct("TSHIRT-VAR-BASE-DEL");
    mockMvc
        .perform(
            post("/api/admin/products/" + productId + "/variants")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "color": "Preto",
                      "size": "M",
                      "sku": "TSHIRT-DEL-M",
                      "price": 20.00,
                      "stockQty": 1
                    }
                    """))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            delete("/api/admin/products/" + productId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("Product has variants"));
  }

  @Test
  @DisplayName("SKU da variante igual ao do produto retorna 409")
  void shouldConflictWhenVariantSkuMatchesProductSku() throws Exception {
    Long productId = createProduct("TSHIRT-SHARED-SKU");

    mockMvc
        .perform(
            post("/api/admin/products/" + productId + "/variants")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "color": "Preto",
                      "size": "M",
                      "sku": "TSHIRT-SHARED-SKU",
                      "price": 20.00,
                      "stockQty": 1
                    }
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("SKU already exists: TSHIRT-SHARED-SKU"));
  }

  @Test
  @DisplayName("SKU de produto igual ao de variante existente retorna 409")
  void shouldConflictWhenProductSkuMatchesVariantSku() throws Exception {
    Long productId = createProduct("TSHIRT-VAR-BASE-XSKU");
    mockMvc
        .perform(
            post("/api/admin/products/" + productId + "/variants")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "color": "Preto",
                      "size": "M",
                      "sku": "TSHIRT-TAKEN-BY-VAR",
                      "price": 20.00,
                      "stockQty": 1
                    }
                    """))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/admin/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Outra",
                      "sku": "TSHIRT-TAKEN-BY-VAR",
                      "price": 10.00,
                      "stockQty": 1
                    }
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("SKU already exists: TSHIRT-TAKEN-BY-VAR"));
  }
}
