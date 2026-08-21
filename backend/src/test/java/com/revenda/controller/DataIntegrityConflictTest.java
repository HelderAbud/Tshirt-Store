package com.revenda.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.revenda.domain.User;
import com.revenda.domain.UserRole;
import com.revenda.service.JwtService;
import com.revenda.service.ProductService;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DataIntegrityConflictTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtService jwtService;

  @MockBean private ProductService productService;

  @Test
  @DisplayName("Unique no banco vira 409 genérico sem vazar SQL")
  void shouldReturnConflictWhenDatabaseRejectsUnique() throws Exception {
    when(productService.create(anyString(), anyString(), any(BigDecimal.class), anyInt()))
        .thenThrow(
            new DataIntegrityViolationException(
                "Duplicate entry 'TSHIRT-X' for key 'products.sku'"));

    String token =
        jwtService.generateToken(new User("admin@example.com", "encoded", UserRole.ADMIN));

    mockMvc
        .perform(
            post("/api/admin/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Camiseta",
                      "sku": "TSHIRT-X",
                      "price": 10.00,
                      "stockQty": 1
                    }
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.error").value("Resource already exists"))
        .andExpect(jsonPath("$.error").value(not(containsString("Duplicate entry"))));
  }
}
