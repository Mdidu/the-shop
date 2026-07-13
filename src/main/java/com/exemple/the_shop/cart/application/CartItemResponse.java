package com.exemple.the_shop.cart.application;

import java.math.BigDecimal;
import java.util.UUID;

import com.exemple.the_shop.cart.domain.model.CartItem;

public record CartItemResponse(
    UUID productId,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal lineTotal) {

  public static CartItemResponse from(CartItem item) {
    BigDecimal unitPrice = item.getUnitPrice().amount();
    BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
    return new CartItemResponse(item.getProductId(), item.getQuantity(), unitPrice, lineTotal);
  }
}
