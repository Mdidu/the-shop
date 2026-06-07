package com.exemple.the_shop.user.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {
  Optional<RefreshTokenJpaEntity> findByToken(String token);

  @Modifying
  @Transactional
  @Query("UPDATE RefreshTokenJpaEntity r SET r.revoked = true WHERE r.user.id = :userId")
  void revokeAllByUserId(@Param("userId") UUID userId);

  @Modifying
  @Transactional
  @Query("DELETE FROM RefreshTokenJpaEntity r WHERE r.expiresAt < :now")
  int deleteAllExpiredSince(@Param("now") Instant now);
}
