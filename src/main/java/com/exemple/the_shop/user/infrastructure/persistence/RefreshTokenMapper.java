package com.exemple.the_shop.user.infrastructure.persistence;

import com.exemple.the_shop.user.domain.model.RefreshToken;

public final class RefreshTokenMapper {

  private RefreshTokenMapper() {
  }

  public static RefreshTokenJpaEntity toJpaEntity(RefreshToken token) {
    RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity();
    entity.setId(token.getId());

    UserJpaEntity userRef = new UserJpaEntity();
    userRef.setId(token.getUserId());
    entity.setUser(userRef);

    entity.setToken(token.getToken());
    entity.setExpiresAt(token.getExpiresAt());
    entity.setRevoked(token.isRevoked());
    // createdAt géré par @PrePersist
    return entity;
  }

  public static RefreshToken toDomain(RefreshTokenJpaEntity entity) {
    return new RefreshToken(
        entity.getId(),
        entity.getUser().getId(),
        entity.getToken(),
        entity.getExpiresAt(),
        entity.isRevoked(),
        entity.getCreatedAt()
    );
  }
}
