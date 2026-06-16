package com.exemple.the_shop.catalog.domain.exception;

/** Levée quand une transition de statut produit n'est pas autorisée (ex. activer un produit déjà actif). */
public class IllegalProductStatusTransitionException extends RuntimeException {

  public IllegalProductStatusTransitionException(String message) {
    super(message);
  }
}
