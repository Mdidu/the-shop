package com.exemple.the_shop.catalog.infrastructure.persistence.stock;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductStockJpaRepository extends JpaRepository<ProductStockJpaEntity, UUID> {
  Optional<ProductStockJpaEntity> findByProductId(UUID productId);
}
