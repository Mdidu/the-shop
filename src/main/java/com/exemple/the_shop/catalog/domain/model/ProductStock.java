package com.exemple.the_shop.catalog.domain.model;

import java.util.UUID;

import com.exemple.the_shop.catalog.domain.exception.InsufficientStockException;

public class ProductStock {
  private final UUID id;
  private final UUID productId;
  private final int quantity;
  private final int version;

  public ProductStock(UUID id, UUID productId, int quantity, int version) {
    if (quantity < 0) {
      throw new IllegalArgumentException("La quantité de stock ne peut pas être négative : " + quantity);
    }
    this.id = id;
    this.productId = productId;
    this.quantity = quantity;
    this.version = version;
  }

  public static ProductStock create(UUID productId, int quantity) {
    return new ProductStock(UUID.randomUUID(), productId, quantity, 0);
  }

  public UUID getId() {
    return id;
  }

  public UUID getProductId() {
    return productId;
  }

  public int getQuantity() {
    return quantity;
  }

  public ProductStock increase(int quantityIncrease) {
    if (quantityIncrease <= 0) {
      throw new IllegalArgumentException("La quantité à ajouter doit être strictement positive : " + quantityIncrease);
    }
    return new ProductStock(id, productId, quantity + quantityIncrease, version);
  }

  public ProductStock decrease(int quantityDecrease) {
    if (quantityDecrease <= 0) {
      throw new IllegalArgumentException("La quantité à retirer doit être strictement positive : " + quantityDecrease);
    }
    if (quantity - quantityDecrease < 0) {
      throw new InsufficientStockException(
          "Stock insuffisant : disponible " + quantity + ", demandé " + quantityDecrease);
    }
    return new ProductStock(id, productId, quantity - quantityDecrease, version);
  }

  public int getVersion() {
    return version;
  }
}
