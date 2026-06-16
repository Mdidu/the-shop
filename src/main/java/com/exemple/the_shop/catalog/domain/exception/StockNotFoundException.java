package com.exemple.the_shop.catalog.domain.exception;

/** Levée quand aucun stock n'est associé au produit recherché. */
public class StockNotFoundException extends RuntimeException {
  public StockNotFoundException(String message) {
    super(message);
  }
}
