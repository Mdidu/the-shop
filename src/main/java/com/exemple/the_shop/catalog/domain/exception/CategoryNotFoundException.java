package com.exemple.the_shop.catalog.domain.exception;

/** Levée quand aucune catégorie ne correspond à l'identifiant ou au slug recherché. */
public class CategoryNotFoundException extends RuntimeException {
  public CategoryNotFoundException(String message) {
    super(message);
  }
}
