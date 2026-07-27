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
class CatalogProductControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtService jwtService;

  private String adminToken() {
    return jwtService.generateToken(
        new User("admin-catalog@example.com", "encoded", UserRole.ADMIN));
  }

  @Test
  @DisplayName("Catálogo lista e detalha produto sem autenticação")
  void shouldListAndGetProductWithoutAuth() throws Exception {
    String body =
        """
        {
          "name": "Camiseta Catálogo",
          "sku": "TSHIRT-CAT-01",
          "price": 39.90,
          "stockQty": 12
        }
        """;

    MvcResult created =
        mockMvc
            .perform(
                post("/api/admin/products")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andReturn();

    String response = created.getResponse().getContentAsString();
    Long id = Long.parseLong(response.replaceAll("(?s).*\"id\"\\s*:\\s*(\\d+).*", "$1"));

    mockMvc
        .perform(get("/api/catalog/products"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[?(@.sku=='TSHIRT-CAT-01')]").exists());

    mockMvc
        .perform(get("/api/catalog/products/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Camiseta Catálogo"))
        .andExpect(jsonPath("$.sku").value("TSHIRT-CAT-01"));
  }

  @Test
  @DisplayName("Catálogo retorna 404 para id inexistente")
  void shouldReturnNotFoundForMissingProduct() throws Exception {
    mockMvc.perform(get("/api/catalog/products/999999")).andExpect(status().isNotFound());
  }
}
