package com.revenda.service;

import com.revenda.domain.User;
import com.revenda.exception.ResourceNotFoundException;
import com.revenda.repository.UserRepository;
import com.revenda.security.JwtPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

  private final UserRepository userRepository;

  public CurrentUserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public User requireUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getPrincipal() == null) {
      throw new ResourceNotFoundException("User not found");
    }
    Object principal = authentication.getPrincipal();
    if (principal instanceof JwtPrincipal jwtPrincipal) {
      if (jwtPrincipal.userId() != null) {
        return userRepository
            .findById(jwtPrincipal.userId())
            .orElseGet(() -> findByEmail(jwtPrincipal.email()));
      }
      return findByEmail(jwtPrincipal.email());
    }
    return findByEmail(principal.toString());
  }

  private User findByEmail(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
  }
}
