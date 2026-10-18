package com.revenda.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActuatorEndpointsTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtService jwtService;

  @Test
  @DisplayName("GET /actuator/health é público e retorna UP")
  void healthIsPublic() throws Exception {
    mockMvc
        .perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  @DisplayName("GET /actuator/info é público")
  void infoIsPublic() throws Exception {
    mockMvc.perform(get("/actuator/info")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /actuator/metrics sem token retorna 401")
  void metricsRequiresAuth() throws Exception {
    mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("GET /actuator/metrics com CUSTOMER retorna 403")
  void metricsForbiddenForCustomer() throws Exception {
    String token =
        jwtService.generateToken(new User("customer@example.com", "encoded", UserRole.CUSTOMER));

    mockMvc
        .perform(get("/actuator/metrics").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("GET /actuator/metrics com ADMIN retorna 200")
  void metricsAllowedForAdmin() throws Exception {
    String token =
        jwtService.generateToken(new User("admin@example.com", "encoded", UserRole.ADMIN));

    mockMvc
        .perform(get("/actuator/metrics").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.names").isArray());
  }
}
