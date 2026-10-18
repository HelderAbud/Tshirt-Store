package com.revenda.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class OrderControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JwtService jwtService;

  private String adminToken() {
    return jwtService.generateToken(
        new User("admin-b3-order@example.com", "encoded", UserRole.ADMIN));
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

  private record CatalogIds(long productId, long variantId) {}

  private CatalogIds createVariant(int stockQty) throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    MvcResult created =
        mockMvc
            .perform(
                post("/api/admin/products")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Camiseta Pedido","sku":"OP-%s","price":20.00,"stockQty":3}
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
                        {"color":"Azul","size":"G","sku":"SKU-ORD-%s","price":25.00,"stockQty":%d}
                        """
                            .formatted(suffix, stockQty)))
            .andExpect(status().isCreated())
            .andReturn();
    long variantId =
        objectMapper.readTree(variant.getResponse().getContentAsString()).get("id").asLong();
    return new CatalogIds(productId, variantId);
  }

  @Test
  @DisplayName("POST /api/orders sem token retorna 401; carrinho vazio 400")
  void shouldRejectAnonymousAndEmptyCart() throws Exception {
    mockMvc.perform(post("/api/orders")).andExpect(status().isUnauthorized());

    String token = register("order-empty-" + UUID.randomUUID() + "@demo.local");
    mockMvc
        .perform(post("/api/orders").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Cart is empty"));
  }

  @Test
  @DisplayName("Checkout cria pedido com snapshot, limpa carrinho e não baixa estoque")
  void shouldCheckoutSnapshotClearCartAndKeepStock() throws Exception {
    String token = register("order-ok-" + UUID.randomUUID() + "@demo.local");
    CatalogIds ids = createVariant(8);

    mockMvc
        .perform(
            put("/api/cart/items")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"variantId\":" + ids.variantId() + ",\"qty\":2}"))
        .andExpect(status().isOk());

    MvcResult checkout =
        mockMvc
            .perform(post("/api/orders").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("CREATED"))
            .andExpect(jsonPath("$.total").value(50.00))
            .andExpect(jsonPath("$.items[0].color").value("Azul"))
            .andExpect(jsonPath("$.items[0].size").value("G"))
            .andExpect(jsonPath("$.items[0].unitPrice").value(25.00))
            .andExpect(jsonPath("$.items[0].qty").value(2))
            .andReturn();
    long orderId =
        objectMapper.readTree(checkout.getResponse().getContentAsString()).get("id").asLong();

    mockMvc
        .perform(get("/api/cart").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isEmpty());

    mockMvc
        .perform(
            get("/api/admin/products/" + ids.productId() + "/variants")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].stockQty").value(8));

    mockMvc
        .perform(get("/api/orders").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(orderId))
        .andExpect(jsonPath("$[0].items").doesNotExist());

    mockMvc
        .perform(get("/api/orders/" + orderId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].qty").value(2));

    String other = register("order-other-" + UUID.randomUUID() + "@demo.local");
    mockMvc
        .perform(get("/api/orders/" + orderId).header(HttpHeaders.AUTHORIZATION, "Bearer " + other))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("ADMIN cancela pedido CREATED; CUSTOMER 403; segunda cancelação 409")
  void shouldAllowAdminCancelOnlyFromCreated() throws Exception {
    String customer = register("order-cancel-" + UUID.randomUUID() + "@demo.local");
    CatalogIds ids = createVariant(4);
    mockMvc
        .perform(
            put("/api/cart/items")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"variantId\":" + ids.variantId() + ",\"qty\":1}"))
        .andExpect(status().isOk());
    long orderId =
        objectMapper
            .readTree(
                mockMvc
                    .perform(
                        post("/api/orders").header(HttpHeaders.AUTHORIZATION, "Bearer " + customer))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("id")
            .asLong();

    mockMvc
        .perform(
            patch("/api/admin/orders/" + orderId + "/status")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CANCELLED\"}"))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            patch("/api/admin/orders/" + orderId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CANCELLED\"}"))
        .andExpect(status().isUnauthorized());

    mockMvc
        .perform(
            patch("/api/admin/orders/" + orderId + "/status")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CANCELLED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));

    mockMvc
        .perform(
            patch("/api/admin/orders/" + orderId + "/status")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CANCELLED\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("Checkout com qty maior que estoque retorna 409 e não cria pedido")
  void shouldRejectCheckoutWhenStockIsInsufficient() throws Exception {
    String token = register("order-stock-" + UUID.randomUUID() + "@demo.local");
    CatalogIds ids = createVariant(1);
    mockMvc
        .perform(
            put("/api/cart/items")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"variantId\":" + ids.variantId() + ",\"qty\":2}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(post("/api/orders").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("Insufficient stock"));

    mockMvc
        .perform(get("/api/cart").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].qty").value(2));

    mockMvc
        .perform(
            get("/api/admin/products/" + ids.productId() + "/variants")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].stockQty").value(1));

    mockMvc
        .perform(get("/api/orders").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
  }

  private long checkoutQty(String token, long variantId, int qty) throws Exception {
    mockMvc
        .perform(
            put("/api/cart/items")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"variantId\":" + variantId + ",\"qty\":" + qty + "}"))
        .andExpect(status().isOk());
    return objectMapper
        .readTree(
            mockMvc
                .perform(post("/api/orders").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString())
        .get("id")
        .asLong();
  }

  @Test
  @DisplayName("Pay baixa estoque, replay da mesma chave não baixa de novo")
  void shouldPayDecrementStockAndReplayIdempotencyKey() throws Exception {
    String token = register("order-pay-" + UUID.randomUUID() + "@demo.local");
    CatalogIds ids = createVariant(8);
    long orderId = checkoutQty(token, ids.variantId(), 2);
    String key = "pay-" + UUID.randomUUID();

    mockMvc
        .perform(
            post("/api/orders/" + orderId + "/pay")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/api/orders/" + orderId + "/pay")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("Idempotency-Key", key))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAID"))
        .andExpect(jsonPath("$.items[0].qty").value(2));

    mockMvc
        .perform(
            get("/api/admin/products/" + ids.productId() + "/variants")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].stockQty").value(6));

    mockMvc
        .perform(get("/api/catalog/products/" + ids.productId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stockQty").value(6))
        .andExpect(jsonPath("$.variants[0].stockQty").value(6));

    mockMvc
        .perform(
            post("/api/orders/" + orderId + "/pay")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("Idempotency-Key", key))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAID"));

    mockMvc
        .perform(
            get("/api/admin/products/" + ids.productId() + "/variants")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].stockQty").value(6));

    mockMvc
        .perform(get("/api/catalog/products/" + ids.productId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stockQty").value(6));

    mockMvc
        .perform(
            post("/api/orders/" + orderId + "/pay")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("Idempotency-Key", "other-" + UUID.randomUUID()))
        .andExpect(status().isConflict());

    mockMvc
        .perform(
            patch("/api/admin/orders/" + orderId + "/status")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CANCELLED\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("Pay de pedido alheio 404; chave reusada noutro pedido 409; pay após cancel 409")
  void shouldIsolatePayAndRejectCancelled() throws Exception {
    String tokenA = register("order-pay-a-" + UUID.randomUUID() + "@demo.local");
    String tokenB = register("order-pay-b-" + UUID.randomUUID() + "@demo.local");
    CatalogIds ids = createVariant(5);
    long orderA = checkoutQty(tokenA, ids.variantId(), 1);
    long orderA2 = checkoutQty(tokenA, ids.variantId(), 1);
    String sharedKey = "shared-" + UUID.randomUUID();

    mockMvc
        .perform(
            post("/api/orders/" + orderA + "/pay")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                .header("Idempotency-Key", "b-on-a"))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            post("/api/orders/" + orderA + "/pay")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                .header("Idempotency-Key", sharedKey))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/orders/" + orderA2 + "/pay")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                .header("Idempotency-Key", sharedKey))
        .andExpect(status().isConflict());

    CatalogIds cancelIds = createVariant(3);
    long toCancel = checkoutQty(tokenA, cancelIds.variantId(), 1);
    mockMvc
        .perform(
            patch("/api/admin/orders/" + toCancel + "/status")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CANCELLED\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/orders/" + toCancel + "/pay")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                .header("Idempotency-Key", "pay-cancelled"))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("Dois checkouts no mesmo stock: segundo pay fica sem estoque")
  void shouldRejectSecondPayWhenStockConsumed() throws Exception {
    String tokenA = register("order-race-a-" + UUID.randomUUID() + "@demo.local");
    String tokenB = register("order-race-b-" + UUID.randomUUID() + "@demo.local");
    CatalogIds ids = createVariant(2);
    long orderA = checkoutQty(tokenA, ids.variantId(), 2);
    long orderB = checkoutQty(tokenB, ids.variantId(), 2);

    mockMvc
        .perform(
            post("/api/orders/" + orderA + "/pay")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                .header("Idempotency-Key", "a"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/orders/" + orderB + "/pay")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                .header("Idempotency-Key", "b"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("Insufficient stock"));
  }
}
