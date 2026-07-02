package com.revenda.controller;

import com.revenda.service.AuthService;
import com.revenda.service.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;
  private final JwtService jwtService;

  public AuthController(AuthService authService, JwtService jwtService) {
    this.authService = authService;
    this.jwtService = jwtService;
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return authService
        .authenticate(request.email(), request.password())
        .map(
            user ->
                ResponseEntity.ok(
                    new LoginResponse(
                        jwtService.generateToken(user), user.getEmail(), user.getRole().name())))
        .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
  }

  public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

  public record LoginResponse(String token, String email, String role) {}
}
