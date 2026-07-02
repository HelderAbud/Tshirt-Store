package com.revenda.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.revenda.domain.User;
import com.revenda.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.io.Encoders;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private static final String TEST_SECRET =
      Encoders.BASE64.encode(
          "test-jwt-secret-for-tshirt-store-minimum-32-bytes".getBytes(StandardCharsets.UTF_8));

  private final JwtService jwtService = new JwtService(TEST_SECRET);

  @Test
  @DisplayName("Deve gerar e ler token JWT contendo email e role")
  void shouldGenerateAndParseToken() {
    User user = new User("user@example.com", "encoded-password", UserRole.ADMIN);

    String token = jwtService.generateToken(user);
    Claims claims = jwtService.parseToken(token);

    assertThat(claims.getSubject()).isEqualTo("user@example.com");
    assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    assertThat(claims.getExpiration()).isNotNull();
  }

  @Test
  @DisplayName("Deve rejeitar segredo JWT fraco")
  void shouldRejectWeakSecret() {
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> new JwtService("weak-secret"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("JWT secret");
  }
}
