package com.revenda.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.revenda.service.JwtService;
import io.jsonwebtoken.Claims;
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
class AuthRegisterTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtService jwtService;

  @Test
  @DisplayName("POST /api/auth/register cria CUSTOMER e devolve 201 com token")
  void shouldRegisterCustomerAndReturnCreated() throws Exception {
    MvcResult registered =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"email":"novo@demo.local","password":"Demo12345!"}
                        """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("novo@demo.local"))
            .andExpect(jsonPath("$.role").value("CUSTOMER"))
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andReturn();

    String token =
        registered
            .getResponse()
            .getContentAsString()
            .replaceAll("(?s).*\"token\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    Claims claims = jwtService.parseToken(token);
    org.assertj.core.api.Assertions.assertThat(claims.get("userId", Number.class)).isNotNull();
  }

  @Test
  @DisplayName("POST /api/auth/register com email duplicado retorna 409")
  void shouldReturnConflictWhenEmailAlreadyExists() throws Exception {
    String body = """
        {"email":"duplicado@demo.local","password":"Demo12345!"}
        """;

    mockMvc
        .perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated());

    mockMvc
        .perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("POST /api/auth/register com senha curta retorna 400")
  void shouldReturnBadRequestWhenPasswordTooShort() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"curto@demo.local","password":"short"}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Token de CUSTOMER recém-cadastrado não cria produto admin")
  void shouldForbidAdminProductCreateForRegisteredCustomer() throws Exception {
    MvcResult registered =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"email":"cliente-novo@demo.local","password":"Demo12345!"}
                        """))
            .andExpect(status().isCreated())
            .andReturn();

    String response = registered.getResponse().getContentAsString();
    String token = response.replaceAll("(?s).*\"token\"\\s*:\\s*\"([^\"]+)\".*", "$1");

    mockMvc
        .perform(
            post("/api/admin/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Camiseta Preta",
                      "sku": "TSHIRT-REG-BLK",
                      "price": 79.90,
                      "stockQty": 10
                    }
                    """))
        .andExpect(status().isForbidden());
  }
}
