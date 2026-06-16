package com.exemple.the_shop.catalog.infrastructure.persistence.stock;

import com.exemple.the_shop.catalog.domain.model.ProductStock;

public final class ProductStockMapper {
  private ProductStockMapper() {
  }

  public static ProductStockJpaEntity toJpaEntity(ProductStock productStock) {
    ProductStockJpaEntity entity = new ProductStockJpaEntity();
    entity.setId(productStock.getId());
    entity.setProductId(productStock.getProductId());
    entity.setQuantity(productStock.getQuantity());
    return entity;
  }

  public static ProductStock toDomain(ProductStockJpaEntity entity) {
    return new ProductStock(
        entity.getId(),
        entity.getProductId(),
        entity.getQuantity(),
        entity.getVersion());
  }
}
