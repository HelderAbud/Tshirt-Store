package com.revenda.service;

import com.revenda.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JwtService {

  private final Key signingKey;

  public JwtService(@Value("${app.jwt.secret}") String secret) {
    this.signingKey = buildSigningKey(secret);
  }

  private Key buildSigningKey(String secret) {
    if (!StringUtils.hasText(secret)) {
      throw new IllegalArgumentException("JWT secret must be configured");
    }

    try {
      byte[] keyBytes = Decoders.BASE64.decode(secret);
      return Keys.hmacShaKeyFor(keyBytes);
    } catch (RuntimeException ex) {
      throw new IllegalArgumentException(
          "JWT secret must be Base64 encoded and at least 256 bits", ex);
    }
  }

  public String generateToken(User user) {
    Instant now = Instant.now();
    return Jwts.builder()
        .setSubject(user.getEmail())
        .claim("role", user.getRole().name())
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
        .signWith(signingKey, SignatureAlgorithm.HS256)
        .compact();
  }

  public Claims parseToken(String token) {
    return Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token).getBody();
  }
}
