package com.revenda.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revenda.domain.User;
import com.revenda.domain.UserRole;
import com.revenda.service.AuthService;
import com.revenda.service.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private AuthService authService;
  @MockBean private JwtService jwtService;

  @Test
  @DisplayName("Deve retornar 200 e token quando credenciais forem válidas")
  void shouldReturnTokenWhenCredentialsAreValid() throws Exception {
    User user = new User("user@example.com", "encoded-password", UserRole.CUSTOMER);

    org.mockito.Mockito.when(authService.authenticate("user@example.com", "plain-password"))
        .thenReturn(Optional.of(user));
    org.mockito.Mockito.when(jwtService.generateToken(user)).thenReturn("fake-jwt-token");

    AuthController.LoginRequest request =
        new AuthController.LoginRequest("user@example.com", "plain-password");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("fake-jwt-token"))
        .andExpect(jsonPath("$.email").value("user@example.com"))
        .andExpect(jsonPath("$.role").value("CUSTOMER"));
  }

  @Test
  @DisplayName("Deve retornar 401 quando credenciais forem inválidas")
  void shouldReturnUnauthorizedWhenCredentialsAreInvalid() throws Exception {
    org.mockito.Mockito.when(authService.authenticate("user@example.com", "wrong-password"))
        .thenReturn(Optional.empty());

    AuthController.LoginRequest request =
        new AuthController.LoginRequest("user@example.com", "wrong-password");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }
}
