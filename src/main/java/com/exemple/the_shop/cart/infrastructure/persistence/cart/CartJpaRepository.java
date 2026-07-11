package com.exemple.the_shop.cart.infrastructure.persistence.cart;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CartJpaRepository extends JpaRepository<CartJpaEntity, UUID> {
  Optional<CartJpaEntity> findByUserId(UUID userId);
}
