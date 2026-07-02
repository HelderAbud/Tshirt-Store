package com.revenda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.revenda.domain.User;
import com.revenda.domain.UserRole;
import com.revenda.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

  private final UserRepository userRepository = Mockito.mock(UserRepository.class);
  private final PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
  private final AuthService authService = new AuthService(userRepository, passwordEncoder);

  @Test
  @DisplayName("Deve autenticar usuário quando email e senha estiverem corretos")
  void shouldAuthenticateWhenCredentialsAreValid() {
    User user = new User("user@example.com", "encoded-password", UserRole.CUSTOMER);
    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("plain-password", "encoded-password")).thenReturn(true);

    Optional<User> result = authService.authenticate("user@example.com", "plain-password");

    assertThat(result).isPresent();
    assertThat(result.get().getEmail()).isEqualTo("user@example.com");
  }

  @Test
  @DisplayName("Não deve autenticar quando usuário não existir")
  void shouldNotAuthenticateWhenUserDoesNotExist() {
    when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

    Optional<User> result = authService.authenticate("unknown@example.com", "any-password");

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Não deve autenticar quando senha estiver incorreta")
  void shouldNotAuthenticateWhenPasswordIsInvalid() {
    User user = new User("user@example.com", "encoded-password", UserRole.CUSTOMER);
    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

    Optional<User> result = authService.authenticate("user@example.com", "wrong-password");

    assertThat(result).isEmpty();
  }
}
