package com.exemple.the_shop.user.application.token;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.exemple.the_shop.user.domain.exception.InvalidRefreshTokenException;
import com.exemple.the_shop.user.domain.model.RefreshToken;
import com.exemple.the_shop.user.domain.port.out.RefreshTokenRepository;

/**
 * Cycle de vie des refresh tokens : émission, rotation, révocation, purge.
 *
 * <p>
 * Le token est une valeur opaque aléatoire (pas un JWT) — sa seule preuve de
 * validité est sa présence non révoquée et non expirée en base.
 */
@Service
public class RefreshTokenService {

  /** 32 octets = 256 bits d'entropie, encodés en base64url (~43 caractères). */
  private static final int TOKEN_BYTES = 32;

  private final RefreshTokenRepository refreshTokenRepository;
  private final long refreshExpirationMs;
  private final SecureRandom secureRandom = new SecureRandom();
  private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

  public RefreshTokenService(
      RefreshTokenRepository refreshTokenRepository,
      @Value("${the-shop.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
    this.refreshTokenRepository = refreshTokenRepository;
    this.refreshExpirationMs = refreshExpirationMs;
  }

  /** Crée et persiste un nouveau token pour l'utilisateur. */
  @Transactional
  public RefreshToken issue(UUID userId) {
    Instant expiresAt = Instant.now().plusMillis(refreshExpirationMs);
    RefreshToken token = RefreshToken.issue(userId, generateOpaqueToken(), expiresAt);
    return refreshTokenRepository.save(token);
  }

  /**
   * Rotation : révoque le token présenté et en émet un nouveau. Empêche le
   * rejeu d'un token volé — un token n'est utilisable qu'une fois pour
   * rafraîchir.
   */
  @Transactional
  public RefreshToken rotate(String presentedToken) {
    RefreshToken current = validate(presentedToken);
    current.revoke();
    refreshTokenRepository.save(current);
    return issue(current.getUserId());
  }

  /** Révoque le token présenté (logout du device courant). */
  @Transactional
  public void revoke(String presentedToken) {
    RefreshToken token = validate(presentedToken);
    token.revoke();
    refreshTokenRepository.save(token);
  }

  /** Révoque tous les tokens d'un utilisateur (logout global, compromission). */
  @Transactional
  public void revokeAll(UUID userId) {
    refreshTokenRepository.revokeAllByUserId(userId);
  }

  /** Supprime définitivement les tokens expirés (tâche de nettoyage). */
  @Transactional
  public void purgeExpired() {
    refreshTokenRepository.deleteAllExpiredSince(Instant.now());
  }

  /** Vérifie qu'un token existe et est actif, sinon lève une exception. */
  @Transactional(readOnly = true)
  public RefreshToken validate(String presentedToken) {
    RefreshToken token = refreshTokenRepository.findByToken(presentedToken)
        .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token introuvable"));
    if (!token.isActive(Instant.now())) {
      throw new InvalidRefreshTokenException("Refresh token révoqué ou expiré");
    }
    return token;
  }

  private String generateOpaqueToken() {
    byte[] bytes = new byte[TOKEN_BYTES];
    secureRandom.nextBytes(bytes);
    return encoder.encodeToString(bytes);
  }
}
