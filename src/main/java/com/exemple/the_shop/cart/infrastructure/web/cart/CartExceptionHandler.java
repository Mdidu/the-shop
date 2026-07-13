package com.exemple.the_shop.cart.infrastructure.web.cart;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.exemple.the_shop.cart.domain.exception.CartItemNotFoundException;
import com.exemple.the_shop.cart.domain.exception.ProductNotAvailableException;
import com.exemple.the_shop.cart.domain.exception.ProductNotFoundException;
import com.exemple.the_shop.shared.web.ApiError;

/**
 * Mapping HTTP des exceptions <b>métier du module cart</b>.
 *
 * <p>
 * Scopé via {@code basePackageClasses} sur lui-même : ne couvre que les
 * controllers du package {@code cart.infrastructure.web}, sans empiéter sur les
 * autres modules. Priorité haute (0) pour passer avant le filet générique de
 * {@code GlobalExceptionHandler}. Les erreurs transverses (validation @Valid,
 * {@code IllegalArgumentException} du domaine — ex. quantité ≤ 0) sont déjà
 * prises en charge par {@code GlobalExceptionHandler} en 400.
 */
@Order(0)
@RestControllerAdvice(basePackageClasses = CartExceptionHandler.class)
public class CartExceptionHandler {

  /** Ligne visée absente du panier (ou panier inexistant) → ressource absente. */
  @ExceptionHandler(CartItemNotFoundException.class)
  public ResponseEntity<ApiError> handleCartItemNotFound(CartItemNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiError.of(HttpStatus.NOT_FOUND, ex.getMessage()));
  }

  /** Produit ajouté introuvable dans le catalogue → ressource absente. */
  @ExceptionHandler(ProductNotFoundException.class)
  public ResponseEntity<ApiError> handleProductNotFound(ProductNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiError.of(HttpStatus.NOT_FOUND, ex.getMessage()));
  }

  /** Produit existant mais non actif → conflit avec l'état du catalogue. */
  @ExceptionHandler(ProductNotAvailableException.class)
  public ResponseEntity<ApiError> handleProductNotAvailable(ProductNotAvailableException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiError.of(HttpStatus.CONFLICT, ex.getMessage()));
  }
}
