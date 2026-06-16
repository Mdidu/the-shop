package com.exemple.the_shop.catalog.infrastructure.persistence.product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID> {
  Optional<ProductJpaEntity> findBySlug(String slug);

  List<ProductJpaEntity> findAllByCategoryId(UUID categoryId);
}
