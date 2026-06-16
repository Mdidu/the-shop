package com.exemple.the_shop.catalog.domain.exception;

/** Levée quand on tente de décrémenter un stock en dessous de zéro. */
public class InsufficientStockException extends RuntimeException {

  public InsufficientStockException(String message) {
    super(message);
  }
}
