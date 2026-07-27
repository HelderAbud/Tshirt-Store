package com.revenda.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class AdminProductControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtService jwtService;

  private String adminToken() {
    return jwtService.generateToken(new User("admin@example.com", "encoded", UserRole.ADMIN));
  }

  @Test
  @DisplayName("ADMIN cria produto com sucesso")
  void shouldCreateProductAsAdmin() throws Exception {
    String body =
        """
        {
          "name": "Camiseta Branca",
          "sku": "TSHIRT-WHT-M",
          "price": 69.90,
          "stockQty": 5
        }
        """;

    mockMvc
        .perform(
            post("/api/admin/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.name").value("Camiseta Branca"))
        .andExpect(jsonPath("$.sku").value("TSHIRT-WHT-M"))
        .andExpect(jsonPath("$.price").value(69.90))
        .andExpect(jsonPath("$.stockQty").value(5));
  }

  @Test
  @DisplayName("SKU duplicado retorna 409")
  void shouldReturnConflictOnDuplicateSku() throws Exception {
    String body =
        """
        {
          "name": "Camiseta Azul",
          "sku": "TSHIRT-BLU-DUP",
          "price": 59.90,
          "stockQty": 3
        }
        """;

    mockMvc
        .perform(
            post("/api/admin/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/admin/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("Payload inválido retorna 400")
  void shouldReturnBadRequestOnInvalidPayload() throws Exception {
    String body =
        """
        {
          "name": "",
          "sku": "X",
          "price": -1,
          "stockQty": -5
        }
        """;

    mockMvc
        .perform(
            post("/api/admin/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("ADMIN atualiza e apaga produto")
  void shouldUpdateAndDeleteProduct() throws Exception {
    String createBody =
        """
        {
          "name": "Camiseta Verde",
          "sku": "TSHIRT-GRN-S",
          "price": 49.90,
          "stockQty": 2
        }
        """;

    MvcResult created =
        mockMvc
            .perform(
                post("/api/admin/products")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createBody))
            .andExpect(status().isCreated())
            .andReturn();

    String response = created.getResponse().getContentAsString();
    Long id = Long.parseLong(response.replaceAll("(?s).*\"id\"\\s*:\\s*(\\d+).*", "$1"));

    String updateBody =
        """
        {
          "name": "Camiseta Verde Escuro",
          "sku": "TSHIRT-GRN-S",
          "price": 54.90,
          "stockQty": 8
        }
        """;

    mockMvc
        .perform(
            put("/api/admin/products/" + id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Camiseta Verde Escuro"))
        .andExpect(jsonPath("$.stockQty").value(8));

    mockMvc
        .perform(
            get("/api/admin/products/" + id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sku").value("TSHIRT-GRN-S"));

    mockMvc
        .perform(
            delete("/api/admin/products/" + id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get("/api/admin/products/" + id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isNotFound());
  }
}
