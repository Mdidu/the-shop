package com.exemple.the_shop.catalog.infrastructure.persistence.category;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, UUID> {
  Optional<CategoryJpaEntity> findBySlug(String slug);
}
