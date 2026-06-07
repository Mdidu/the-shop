package com.exemple.the_shop.user.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Génération et validation des access tokens JWT (signés HS256).
 *
 * <p>Volontairement ignorant du domaine : il ne connaît ni {@code User} ni
 * {@code Role}, seulement un identifiant et un rôle sous forme de chaîne. Toute
 * la logique métier reste dans la couche application.
 */
@Component
public class JwtUtil {

  private static final String CLAIM_ROLE = "role";

  private final SecretKey key;
  private final long accessExpirationMs;

  public JwtUtil(
      @Value("${the-shop.jwt.secret}") String secret,
      @Value("${the-shop.jwt.access-expiration-ms}") long accessExpirationMs) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.accessExpirationMs = accessExpirationMs;
  }

  public String generateAccessToken(UUID userId, String role) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(userId.toString())
        .claim(CLAIM_ROLE, role)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusMillis(accessExpirationMs)))
        .signWith(key)
        .compact();
  }

  /** Valide la signature et l'expiration. Renvoie {@code false} si invalide. */
  public boolean isValid(String token) {
    try {
      parse(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  public UUID extractUserId(String token) {
    return UUID.fromString(parse(token).getSubject());
  }

  public String extractRole(String token) {
    return parse(token).get(CLAIM_ROLE, String.class);
  }

  /** Parse + vérifie la signature ; lève {@link JwtException} si invalide. */
  private Claims parse(String token) {
    return Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }
}
