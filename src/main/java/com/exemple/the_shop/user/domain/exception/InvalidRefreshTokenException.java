package com.exemple.the_shop.user.domain.exception;

/** Levée quand un refresh token présenté est introuvable, révoqué ou expiré. */
public class InvalidRefreshTokenException extends RuntimeException {

  public InvalidRefreshTokenException(String message) {
    super(message);
  }
}
