package com.exemple.the_shop.user.domain.exception;

/** Levée à l'inscription quand l'email fourni est déjà associé à un compte existant. */
public class EmailAlreadyUsedException extends RuntimeException {

  public EmailAlreadyUsedException(String message) {
    super(message);
  }
}
