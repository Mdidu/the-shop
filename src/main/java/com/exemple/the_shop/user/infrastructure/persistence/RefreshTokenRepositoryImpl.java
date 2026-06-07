package com.exemple.the_shop.user.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.exemple.the_shop.user.domain.model.RefreshToken;
import com.exemple.the_shop.user.domain.port.out.RefreshTokenRepository;

@Repository
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

  private final RefreshTokenJpaRepository refreshTokenJpaRepository;

  public RefreshTokenRepositoryImpl(RefreshTokenJpaRepository refreshTokenJpaRepository) {
    this.refreshTokenJpaRepository = refreshTokenJpaRepository;
  }

  @Override
  public Optional<RefreshToken> findByToken(String token) {
    return refreshTokenJpaRepository.findByToken(token).map(RefreshTokenMapper::toDomain);
  }

  @Override
  public RefreshToken save(RefreshToken refreshToken) {
    RefreshTokenJpaEntity entity = RefreshTokenMapper.toJpaEntity(refreshToken);
    RefreshTokenJpaEntity entityPostSave = refreshTokenJpaRepository.save(entity);
    return RefreshTokenMapper.toDomain(entityPostSave);
  }

  @Override
  public void revokeAllByUserId(UUID userId) {
    refreshTokenJpaRepository.revokeAllByUserId(userId);
  }

  @Override
  public void deleteAllExpiredSince(Instant now) {
    refreshTokenJpaRepository.deleteAllExpiredSince(now);
  }

}
