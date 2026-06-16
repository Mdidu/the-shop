package com.exemple.the_shop.catalog.domain.exception;

/** Levée quand aucun produit ne correspond à l'identifiant ou au slug recherché. */
public class ProductNotFoundException extends RuntimeException {

  public ProductNotFoundException(String message) {
    super(message);
  }
}
