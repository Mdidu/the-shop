package com.exemple.the_shop.cart.domain.exception;

/** Levée quand on tente d'ajouter au panier un produit qui existe mais n'est pas actif. */
public class ProductNotAvailableException extends RuntimeException {
  public ProductNotAvailableException(String message) {
    super(message);
  }
}
