package com.exemple.the_shop.user.domain.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.exemple.the_shop.user.domain.model.RefreshToken;

/**
 * Port de persistence des refresh tokens. Parle en types domaine
 * ({@link RefreshToken}) — l'implémentation JPA vit dans l'infrastructure.
 */
public interface RefreshTokenRepository {

  Optional<RefreshToken> findByToken(String token);

  RefreshToken save(RefreshToken refreshToken);

  void revokeAllByUserId(UUID userId);

  void deleteAllExpiredSince(Instant now);
}
