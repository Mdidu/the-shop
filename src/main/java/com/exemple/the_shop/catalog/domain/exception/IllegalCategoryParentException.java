package com.exemple.the_shop.catalog.domain.exception;

/** Levée quand le parent demandé pour une catégorie est invalide (ex. se prendre soi-même comme parent, ou créer un cycle). */
public class IllegalCategoryParentException extends RuntimeException {

  public IllegalCategoryParentException(String message) {
    super(message);
  }
}
