package com.exemple.the_shop.catalog.application.product;

import java.math.BigDecimal;
import java.util.UUID;

import com.exemple.the_shop.catalog.domain.model.Product;
import com.exemple.the_shop.catalog.domain.model.ProductStatus;

public record ProductResponse(
    UUID id,
    String name,
    String slug,
    String description,
    UUID categoryId,
    BigDecimal price,
    ProductStatus status,
    int quantity) {

  public static ProductResponse from(Product product, int quantity) {
    return new ProductResponse(
        product.getId(),
        product.getName(),
        product.getSlug().value(),
        product.getDescription(),
        product.getCategoryId(),
        product.getPrice().amount(),
        product.getStatus(),
        quantity);
  }
}
