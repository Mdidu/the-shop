package com.exemple.the_shop.shared.domain.exception;

/** Levée quand on tente de persister une entité avec un slug déjà utilisé (contrainte d'unicité). */
public class SlugAlreadyUsedException extends RuntimeException {
  public SlugAlreadyUsedException(String message) {
    super(message);
  }
}
