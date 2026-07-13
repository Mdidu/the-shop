package com.exemple.the_shop.cart.domain.exception;

/** Levée quand on tente d'ajouter au panier un produit qui n'existe pas dans le catalogue. */
public class ProductNotFoundException extends RuntimeException {
  public ProductNotFoundException(String message) {
    super(message);
  }
}
