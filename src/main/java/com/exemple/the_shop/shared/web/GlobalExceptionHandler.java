package com.exemple.the_shop.shared.web;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Gestion des exceptions <b>transverses</b> à toute l'API : erreurs de
 * validation, échecs d'authentification (types Spring), et filet de sécurité
 * générique.
 *
 * <p>
 * Priorité la plus basse ({@link Ordered#LOWEST_PRECEDENCE}) : les advices
 * de module (ex. auth) sont consultés en premier, ce handler n'attrape que ce
 * qu'aucun module n'a pris en charge.
 */
@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /** Échec de validation des @Valid sur les corps de requête → 400. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
    List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
        .map(fe -> new ApiError.FieldError(fe.getField(), fe.getDefaultMessage()))
        .toList();
    return ResponseEntity.badRequest()
        .body(ApiError.of(HttpStatus.BAD_REQUEST, "Validation échouée", fieldErrors));
  }

  /** Mauvaises identifiants (levé par la couche application) → 401. */
  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiError.of(HttpStatus.UNAUTHORIZED, ex.getMessage()));
  }

  /**
   * Argument invalide non rattrapé par un module : entrée client malformée plutôt
   * qu'un bug serveur. Couvre notamment un value object construit depuis l'URL
   * (ex. {@code Slug.of} sur un path variable mal formé) ou un invariant de domaine
   * franchi en dernier rideau. → 400 plutôt que 500.
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.badRequest()
        .body(ApiError.of(HttpStatus.BAD_REQUEST, ex.getMessage()));
  }

  /** Filet de sécurité : tout le reste → 500, sans fuiter le détail au client. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
    log.error("Erreur non gérée", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiError.of(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue"));
  }
}
