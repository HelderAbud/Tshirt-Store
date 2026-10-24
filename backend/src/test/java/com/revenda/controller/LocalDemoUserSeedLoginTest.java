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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "local"})
class LocalDemoUserSeedLoginTest {

  @DynamicPropertySource
  static void isolatedDatabase(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.datasource.url", () -> "jdbc:h2:mem:localdemo;DB_CLOSE_DELAY=-1;MODE=MySQL");
  }

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("local profile logs in the demo admin")
  void localProfileLogsInDemoAdmin() throws Exception {
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
  @DisplayName("local profile logs in the demo customer")
  void localProfileLogsInDemoCustomer() throws Exception {
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
