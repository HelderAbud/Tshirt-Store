package com.revenda.service;

import com.revenda.domain.User;
import com.revenda.repository.UserRepository;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public Optional<User> authenticate(String email, String rawPassword) {
    return userRepository
        .findByEmail(email)
        .filter(user -> passwordEncoder.matches(rawPassword, user.getPasswordHash()));
  }
}
