package com.exemple.the_shop.user.infrastructure.web;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.exemple.the_shop.common.web.ApiError;
import com.exemple.the_shop.user.domain.EmailAlreadyUsedException;
import com.exemple.the_shop.user.domain.InvalidRefreshTokenException;

/**
 * Mapping HTTP des exceptions <b>métier du module auth</b>.
 *
 * <p>Scopé au package des controllers auth via {@code basePackageClasses} :
 * c'est le gabarit à dupliquer pour chaque module (catalog, cart, orders…),
 * chacun gardant la connaissance de ses propres exceptions. Priorité haute
 * (0) pour passer avant le filet générique de {@code GlobalExceptionHandler}.
 */
@Order(0)
@RestControllerAdvice(basePackageClasses = AuthController.class)
public class AuthExceptionHandler {

  /** Email déjà enregistré → conflit avec l'état existant. */
  @ExceptionHandler(EmailAlreadyUsedException.class)
  public ResponseEntity<ApiError> handleEmailAlreadyUsed(EmailAlreadyUsedException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiError.of(HttpStatus.CONFLICT, ex.getMessage()));
  }

  /** Refresh token introuvable, révoqué ou expiré → authentification refusée. */
  @ExceptionHandler(InvalidRefreshTokenException.class)
  public ResponseEntity<ApiError> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiError.of(HttpStatus.UNAUTHORIZED, ex.getMessage()));
  }
}
