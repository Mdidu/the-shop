package com.exemple.the_shop.cart.domain.exception;

/** Levée quand on tente de retirer ou modifier un produit absent du panier. */
public class CartItemNotFoundException extends RuntimeException {
  public CartItemNotFoundException(String message) {
    super(message);
  }
}
