package com.revenda.controller;

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
class AdminInventoryControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtService jwtService;

  private String adminToken() {
    return jwtService.generateToken(new User("admin-inv@example.com", "encoded", UserRole.ADMIN));
  }

  private String customerToken() {
    return jwtService.generateToken(
        new User("customer-inv@example.com", "encoded", UserRole.CUSTOMER));
  }

  private Long createVariant(String productSku, String variantSku, int stock) throws Exception {
    MvcResult product =
        mockMvc
            .perform(
                post("/api/admin/products")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "Camiseta Inv",
                          "sku": "%s",
                          "price": 40.00,
                          "stockQty": 0
                        }
                        """
                            .formatted(productSku)))
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
                        {
                          "color": "Azul",
                          "size": "M",
                          "sku": "%s",
                          "price": 40.00,
                          "stockQty": %d
                        }
                        """
                            .formatted(variantSku, stock)))
            .andExpect(status().isCreated())
            .andReturn();
    return Long.parseLong(
        variant
            .getResponse()
            .getContentAsString()
            .replaceAll("(?s).*\"id\"\\s*:\\s*(\\d+).*", "$1"));
  }

  @Test
  @DisplayName("ADMIN ajusta estoque e lista low-stock")
  void shouldAdjustStockAndListLowStock() throws Exception {
    Long variantId = createVariant("INV-P-01", "INV-V-01", 3);

    mockMvc
        .perform(
            post("/api/admin/inventory/adjust")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"variantId": %d, "delta": 2, "reason": "Repos loja"}
                    """
                        .formatted(variantId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stockQty").value(5));

    mockMvc
        .perform(
            get("/api/admin/inventory/low-stock?threshold=5")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$[?(@.sku=='INV-V-01')].stockQty").value(org.hamcrest.Matchers.hasItem(5)));
  }

  @Test
  @DisplayName("Ajuste que zera abaixo de zero retorna 409")
  void shouldConflictWhenAdjustGoesNegative() throws Exception {
    Long variantId = createVariant("INV-P-02", "INV-V-02", 1);

    mockMvc
        .perform(
            post("/api/admin/inventory/adjust")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"variantId": %d, "delta": -2, "reason": "acerto"}
                    """
                        .formatted(variantId)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("Insufficient stock"));
  }

  @Test
  @DisplayName("CUSTOMER não acessa inventory")
  void shouldForbidCustomer() throws Exception {
    mockMvc
        .perform(
            get("/api/admin/inventory/stock")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken()))
        .andExpect(status().isForbidden());
  }
}
