package com.revenda.config;

import com.revenda.domain.User;
import com.revenda.domain.UserRole;
import com.revenda.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
public class LocalDemoUserSeeder implements ApplicationRunner {

  static final String ADMIN_EMAIL = "admin@demo.local";
  static final String CUSTOMER_EMAIL = "customer@demo.local";
  static final String PASSWORD = "Demo12345!";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public LocalDemoUserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    seed(ADMIN_EMAIL, UserRole.ADMIN);
    seed(CUSTOMER_EMAIL, UserRole.CUSTOMER);
  }

  private void seed(String email, UserRole role) {
    if (userRepository.existsByEmail(email)) {
      return;
    }
    userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), role));
  }
}
