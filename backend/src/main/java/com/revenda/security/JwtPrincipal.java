package com.revenda.security;

public record JwtPrincipal(String email, Long userId) {}
