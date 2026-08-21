package com.revenda.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revenda.domain.User;
import com.revenda.domain.UserRole;
import com.revenda.service.JwtService;
import java.util.UUID;
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
class CartControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JwtService jwtService;

  private String adminToken() {
    return jwtService.generateToken(
        new User("admin-b3-cart@example.com", "encoded", UserRole.ADMIN));
  }

  private String register(String email) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"email":"%s","password":"Demo12345!"}
                        """
                            .formatted(email)))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
  }

  private long createVariant(String sku) throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    MvcResult created =
        mockMvc
            .perform(
                post("/api/admin/products")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Camiseta Carrinho","sku":"P-%s","price":39.90,"stockQty":10}
                        """
                            .formatted(suffix)))
            .andExpect(status().isCreated())
            .andReturn();
    long productId =
        objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
    MvcResult variant =
        mockMvc
            .perform(
                post("/api/admin/products/" + productId + "/variants")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"color":"Preto","size":"M","sku":"%s","price":44.90,"stockQty":8}
                        """
                            .formatted(sku + "-" + suffix)))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper.readTree(variant.getResponse().getContentAsString()).get("id").asLong();
  }

  @Test
  @DisplayName("GET /api/cart sem token retorna 401")
  void shouldRejectAnonymousCart() throws Exception {
    mockMvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("PUT substitui qty, GET devolve item e DELETE remove; outro user isolado")
  void shouldUpsertGetDeleteAndIsolateCarts() throws Exception {
    String tokenA = register("cart-a-" + UUID.randomUUID() + "@demo.local");
    String tokenB = register("cart-b-" + UUID.randomUUID() + "@demo.local");
    long variantId = createVariant("SKU-CART");

    mockMvc
        .perform(
            put("/api/cart/items")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"variantId\":" + variantId + ",\"qty\":2}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].qty").value(2));

    mockMvc
        .perform(
            put("/api/cart/items")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"variantId\":" + variantId + ",\"qty\":5}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].qty").value(5))
        .andExpect(jsonPath("$.items.length()").value(1));

    mockMvc
        .perform(get("/api/cart").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].variantId").value(variantId))
        .andExpect(jsonPath("$.items[0].qty").value(5))
        .andExpect(jsonPath("$.items[0].color").value("Preto"));

    mockMvc
        .perform(get("/api/cart").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isEmpty());

    mockMvc
        .perform(
            delete("/api/cart/items/" + variantId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/cart").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isEmpty());
  }

  @Test
  @DisplayName("PUT com variante inexistente retorna 404; qty 0 retorna 400")
  void shouldValidateVariantAndQty() throws Exception {
    String token = register("cart-val-" + UUID.randomUUID() + "@demo.local");

    mockMvc
        .perform(
            put("/api/cart/items")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"variantId\":999999,\"qty\":1}"))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            put("/api/cart/items")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"variantId\":1,\"qty\":0}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("DELETE de item inexistente no carrinho retorna 404")
  void shouldReturnNotFoundWhenRemovingMissingItem() throws Exception {
    String token = register("cart-del-" + UUID.randomUUID() + "@demo.local");
    mockMvc
        .perform(delete("/api/cart/items/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isNotFound());
  }
}
