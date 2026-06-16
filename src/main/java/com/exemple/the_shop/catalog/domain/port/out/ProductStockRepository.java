package com.exemple.the_shop.catalog.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import com.exemple.the_shop.catalog.domain.model.ProductStock;

public interface ProductStockRepository {
  Optional<ProductStock> findById(UUID id);

  Optional<ProductStock> findByProductId(UUID productId);

  void save(ProductStock productStock);
}
