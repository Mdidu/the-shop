package com.exemple.the_shop.catalog.application.stock;

import java.util.UUID;

import com.exemple.the_shop.catalog.domain.model.ProductStock;

public record ProductStockResponse(UUID productId, int quantity) {

  public static ProductStockResponse from(ProductStock stock) {
    return new ProductStockResponse(stock.getProductId(), stock.getQuantity());
  }
}
