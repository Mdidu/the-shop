package com.exemple.the_shop.catalog.infrastructure.persistence.category;

import com.exemple.the_shop.catalog.domain.model.Category;
import com.exemple.the_shop.shared.domain.Slug;

public final class CategoryMapper {
  private CategoryMapper() {
  }

  public static CategoryJpaEntity toJpaEntity(Category category) {
    CategoryJpaEntity entity = new CategoryJpaEntity();
    entity.setId(category.getId());
    entity.setName(category.getName());
    entity.setSlug(category.getSlug().value());
    entity.setParentId(category.getParentId());
    return entity;
  }

  public static Category toDomain(CategoryJpaEntity entity) {
    return new Category(
        entity.getId(),
        entity.getName(),
        Slug.of(entity.getSlug()),
        entity.getParentId(),
        entity.getCreatedAt());
  }
}
