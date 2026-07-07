package com.exemple.the_shop.cart.domain.model;

import java.util.UUID;

import com.exemple.the_shop.shared.domain.Money;

public class CartItem {
  private final UUID id;
  private final UUID productId;
  private final int quantity;
  private final Money unitPrice;

  public CartItem(UUID id, UUID productId, int quantity, Money unitPrice) {
    if (productId == null) {
      throw new IllegalArgumentException("Le produit d'une ligne de panier est obligatoire");
    }
    if (quantity <= 0) {
      throw new IllegalArgumentException("La quantité d'une ligne de panier doit être positive");
    }
    if (unitPrice == null) {
      throw new IllegalArgumentException("Le prix unitaire d'une ligne de panier est obligatoire");
    }
    this.id = id;
    this.productId = productId;
    this.quantity = quantity;
    this.unitPrice = unitPrice;
  }

  public static CartItem create(UUID productId, int quantity, Money unitPrice) {
    return new CartItem(UUID.randomUUID(), productId, quantity, unitPrice);
  }

  /** Nouvelle ligne = même identité, quantité augmentée du delta (merge d'un ré-ajout). */
  public CartItem increaseQuantity(int delta) {
    return new CartItem(id, productId, quantity + delta, unitPrice);
  }

  /** Nouvelle ligne = même identité, quantité remplacée (le constructeur rejette <= 0). */
  public CartItem withQuantity(int newQuantity) {
    return new CartItem(id, productId, newQuantity, unitPrice);
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

  public Money getUnitPrice() {
    return unitPrice;
  }
}
