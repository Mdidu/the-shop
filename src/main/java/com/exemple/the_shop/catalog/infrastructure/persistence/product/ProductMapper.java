package com.exemple.the_shop.catalog.infrastructure.persistence.product;

import com.exemple.the_shop.catalog.domain.model.Product;
import com.exemple.the_shop.shared.domain.Money;
import com.exemple.the_shop.shared.domain.Slug;

public final class ProductMapper {
  private ProductMapper() {
  }

  public static ProductJpaEntity toJpaEntity(Product product) {
    ProductJpaEntity entity = new ProductJpaEntity();
    entity.setId(product.getId());
    entity.setName(product.getName());
    entity.setDescription(product.getDescription());
    entity.setCategoryId(product.getCategoryId());
    entity.setPrice(product.getPrice().amount());
    entity.setSlug(product.getSlug().value());
    entity.setStatus(product.getStatus());
    return entity;
  }

  public static Product toDomain(ProductJpaEntity entity) {
    return new Product(
        entity.getId(),
        entity.getName(),
        Slug.of(entity.getSlug()),
        entity.getDescription(),
        entity.getCategoryId(),
        Money.of(entity.getPrice()),
        entity.getStatus(),
        entity.getVersion(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
