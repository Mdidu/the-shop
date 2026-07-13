package com.exemple.the_shop.cart.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.exemple.the_shop.cart.domain.exception.CartItemNotFoundException;
import com.exemple.the_shop.shared.domain.Money;

public class Cart {
  private final UUID id;
  private final UUID userId;
  private final List<CartItem> items;
  private final Instant createdAt;
  private final Instant updatedAt;

  public Cart(UUID id, UUID userId, List<CartItem> items, Instant createdAt, Instant updatedAt) {
    if (userId == null) {
      throw new IllegalArgumentException("Le panier doit appartenir à un utilisateur");
    }
    this.id = id;
    this.userId = userId;
    // Copie non modifiable : final gèle la référence, pas le contenu.
    this.items = List.copyOf(items == null ? List.of() : items);
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static Cart create(UUID userId) {
    return new Cart(UUID.randomUUID(), userId, List.of(), null, null);
  }

  /**
   * Ajoute un produit au panier. Si le produit y est déjà, la quantité est
   * cumulée
   * (règle métier qui porte la contrainte UNIQUE(cart_id, product_id) dans le
   * domaine).
   */
  public Cart addItem(UUID productId, int quantity, Money unitPrice) {
    List<CartItem> updated = new ArrayList<>();
    boolean merged = false;
    for (CartItem item : items) {
      if (item.getProductId().equals(productId)) {
        updated.add(item.increaseQuantity(quantity));
        merged = true;
        continue;
      }
      updated.add(item);
    }
    if (!merged) {
      updated.add(CartItem.create(productId, quantity, unitPrice));
    }
    return new Cart(id, userId, updated, createdAt, updatedAt);
  }

  public Cart removeItem(UUID productId) {
    List<CartItem> updated = items.stream()
        .filter(item -> !item.getProductId().equals(productId))
        .toList();
    if (updated.size() == items.size()) {
      throw new CartItemNotFoundException(
          "Aucune ligne pour le produit " + productId + " dans le panier");
    }
    return new Cart(id, userId, updated, createdAt, updatedAt);
  }

  public Cart updateItemQuantity(UUID productId, int newQuantity) {
    boolean present = items.stream()
        .anyMatch(item -> item.getProductId().equals(productId));
    if (!present) {
      throw new CartItemNotFoundException(
          "Aucune ligne pour le produit " + productId + " dans le panier");
    }
    List<CartItem> updated = items.stream()
        .map(item -> item.getProductId().equals(productId)
            ? item.withQuantity(newQuantity)
            : item)
        .toList();
    return new Cart(id, userId, updated, createdAt, updatedAt);
  }

  public Cart clear() {
    return new Cart(id, userId, List.of(), createdAt, updatedAt);
  }

  public boolean isEmpty() {
    return items.isEmpty();
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public List<CartItem> getItems() {
    return items;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
