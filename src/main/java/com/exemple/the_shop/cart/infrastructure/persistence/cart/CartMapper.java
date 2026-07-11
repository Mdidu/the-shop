package com.exemple.the_shop.cart.infrastructure.persistence.cart;

import java.util.List;

import com.exemple.the_shop.cart.domain.model.Cart;
import com.exemple.the_shop.cart.domain.model.CartItem;
import com.exemple.the_shop.shared.domain.Money;

public final class CartMapper {
  private CartMapper() {
  }

  public static Cart toDomain(CartJpaEntity entity) {
    List<CartItem> items = entity.getCartItems().stream()
        .map(CartMapper::toDomain)
        .toList();
    return new Cart(
        entity.getId(),
        entity.getUserId(),
        items,
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  public static CartItem toDomain(CartItemJpaEntity entity) {
    return new CartItem(
        entity.getId(),
        entity.getProductId(),
        entity.getQuantity(),
        Money.of(entity.getUnitPrice()));
  }

  /**
   * Construit une entity NEUVE depuis le domaine. À n'utiliser que pour un cart
   * jamais persisté (branche INSERT). Pour un cart existant, on MUTE l'entity
   * managée (cf. CartRepositoryImpl.syncItems) au lieu de reconstruire.
   * createdAt/updatedAt laissés à null : @PrePersist onCreate les remplit.
   */
  public static CartJpaEntity toJpaEntity(Cart cart) {
    CartJpaEntity entity = new CartJpaEntity();
    entity.setId(cart.getId());
    entity.setUserId(cart.getUserId());
    entity.setCreatedAt(cart.getCreatedAt());
    entity.setCartItems(cart.getItems().stream()
        .map(CartMapper::toJpaEntity)
        .toList());
    return entity;
  }

  public static CartItemJpaEntity toJpaEntity(CartItem item) {
    CartItemJpaEntity entity = new CartItemJpaEntity();
    entity.setId(item.getId());
    entity.setProductId(item.getProductId());
    entity.setQuantity(item.getQuantity());
    entity.setUnitPrice(item.getUnitPrice().amount());
    return entity;
  }
}
