package com.exemple.the_shop.common.web;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Corps de réponse standard pour toutes les erreurs de l'API.
 *
 * <p>{@code fieldErrors} n'est sérialisé que s'il est non vide (erreurs de
 * validation) — sinon il est omis du JSON.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
    Instant timestamp,
    int status,
    String error,
    String message,
    List<FieldError> fieldErrors) {

  /** Détail d'une violation de validation sur un champ précis. */
  public record FieldError(String field, String message) {}

  public static ApiError of(HttpStatus status, String message) {
    return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, List.of());
  }

  public static ApiError of(HttpStatus status, String message, List<FieldError> fieldErrors) {
    return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, fieldErrors);
  }
}
