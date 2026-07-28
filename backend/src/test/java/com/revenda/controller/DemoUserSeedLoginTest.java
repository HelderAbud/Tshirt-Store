package com.revenda.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemoUserSeedLoginTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("Login com admin seed (Flyway V4) retorna token ADMIN")
  void shouldLoginSeededAdmin() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"admin@demo.local","password":"Demo12345!"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("admin@demo.local"))
        .andExpect(jsonPath("$.role").value("ADMIN"))
        .andExpect(jsonPath("$.token").isNotEmpty());
  }

  @Test
  @DisplayName("Login com customer seed (Flyway V4) retorna token CUSTOMER")
  void shouldLoginSeededCustomer() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"customer@demo.local","password":"Demo12345!"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("customer@demo.local"))
        .andExpect(jsonPath("$.role").value("CUSTOMER"))
        .andExpect(jsonPath("$.token").isNotEmpty());
  }
}
