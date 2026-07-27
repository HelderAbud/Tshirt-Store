package com.revenda.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminProductAuthorizationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtService jwtService;

  private static final String CREATE_BODY =
      """
      {
        "name": "Camiseta Preta",
        "sku": "TSHIRT-BLK-M",
        "price": 79.90,
        "stockQty": 10
      }
      """;

  @Test
  @DisplayName("POST /api/admin/products sem token retorna 401")
  void shouldReturnUnauthorizedWithoutToken() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("POST /api/admin/products com CUSTOMER retorna 403")
  void shouldReturnForbiddenForCustomer() throws Exception {
    User customer = new User("customer@example.com", "encoded", UserRole.CUSTOMER);
    String token = jwtService.generateToken(customer);

    mockMvc
        .perform(
            post("/api/admin/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
        .andExpect(status().isForbidden());
  }
}
