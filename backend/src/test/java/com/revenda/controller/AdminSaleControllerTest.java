package com.revenda.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.revenda.domain.User;
import com.revenda.domain.UserRole;
import com.revenda.service.JwtService;
import java.time.LocalDate;
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
class AdminSaleControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtService jwtService;

  private String adminToken() {
    return jwtService.generateToken(new User("admin-sale@example.com", "encoded", UserRole.ADMIN));
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
                          "name": "Camiseta Sale",
                          "sku": "%s",
                          "price": 50.00,
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
                          "color": "Verde",
                          "size": "G",
                          "sku": "%s",
                          "price": 50.00,
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
  @DisplayName("ADMIN registra venda, baixa estoque e aparece no daily")
  void shouldCreateSaleAndDailySeries() throws Exception {
    Long variantId = createVariant("SALE-P-01", "SALE-V-01", 5);
    LocalDate today = LocalDate.now(java.time.ZoneOffset.UTC);

    mockMvc
        .perform(
            post("/api/admin/sales")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "items": [
                        {"variantId": %d, "qty": 2, "unitPrice": 50.00}
                      ]
                    }
                    """
                        .formatted(variantId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.totalAmount").value(100.00))
        .andExpect(jsonPath("$.items[0].qty").value(2));

    mockMvc
        .perform(
            get("/api/admin/inventory/stock")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$[?(@.sku=='SALE-V-01')].stockQty").value(org.hamcrest.Matchers.hasItem(3)));

    mockMvc
        .perform(
            get("/api/admin/sales/daily")
                .param("from", today.toString())
                .param("to", today.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].revenue").value(100.00))
        .andExpect(jsonPath("$[0].count").value(1));
  }

  @Test
  @DisplayName("Venda sem estoque retorna 409 Problem Detail")
  void shouldConflictOnInsufficientStock() throws Exception {
    Long variantId = createVariant("SALE-P-02", "SALE-V-02", 1);

    mockMvc
        .perform(
            post("/api/admin/sales")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "items": [
                        {"variantId": %d, "qty": 3, "unitPrice": 50.00}
                      ]
                    }
                    """
                        .formatted(variantId)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("Insufficient stock"));
  }
}
