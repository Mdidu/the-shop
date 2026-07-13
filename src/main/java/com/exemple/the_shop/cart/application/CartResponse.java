package com.exemple.the_shop.cart.application;

import java.math.BigDecimal;
import java.util.List;

import com.exemple.the_shop.cart.domain.model.Cart;

public record CartResponse(
    List<CartItemResponse> items,
    BigDecimal total) {

  public static CartResponse empty() {
    return new CartResponse(List.of(), BigDecimal.ZERO);
  }

  public static CartResponse from(Cart cart) {
    // Deux parcours assumés (map puis somme) : un panier reste petit par
    // nature, la lisibilité prime sur le fait de tout faire en un seul passage.
    List<CartItemResponse> items = cart.getItems().stream()
        .map(CartItemResponse::from)
        .toList();
    BigDecimal total = items.stream()
        .map(CartItemResponse::lineTotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    return new CartResponse(items, total);
  }
}
