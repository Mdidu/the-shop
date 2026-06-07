package com.exemple.the_shop.user.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Refresh token opaque (pas un JWT) rattaché à un utilisateur.
 *
 * <p>C'est une entité de domaine : elle a une identité ({@code id}) et un état
 * mutable ({@code revoked}). La logique de cycle de vie vit ici, pas dans le
 * service ni dans l'entité JPA.
 */
public class RefreshToken {

  private final UUID id;
  private final UUID userId;
  private final String token;
  private final Instant expiresAt;
  private boolean revoked;
  private final Instant createdAt;

  /** Reconstitution depuis la persistence (tous les champs connus). */
  public RefreshToken(UUID id, UUID userId, String token, Instant expiresAt,
      boolean revoked, Instant createdAt) {
    this.id = id;
    this.userId = userId;
    this.token = token;
    this.expiresAt = expiresAt;
    this.revoked = revoked;
    this.createdAt = createdAt;
  }

  /**
   * Émet un nouveau token. L'identité ({@code id}) est générée ici, dans le
   * domaine. {@code createdAt} reste nul : il est attribué par la couche de
   * persistence à l'insertion ({@code @PrePersist}).
   */
  public static RefreshToken issue(UUID userId, String token, Instant expiresAt) {
    return new RefreshToken(UUID.randomUUID(), userId, token, expiresAt, false, null);
  }

  public void revoke() {
    this.revoked = true;
  }

  public boolean isExpired(Instant now) {
    return expiresAt.isBefore(now);
  }

  /** Utilisable seulement s'il n'est ni révoqué ni expiré. */
  public boolean isActive(Instant now) {
    return !revoked && !isExpired(now);
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getToken() {
    return token;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public boolean isRevoked() {
    return revoked;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
