package com.revenda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class LojaRevendaApplication {

  public static void main(String[] args) {
    SpringApplication.run(LojaRevendaApplication.class, args);
  }
}
