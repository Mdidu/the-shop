package com.exemple.the_shop.catalog.infrastructure.web;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.exemple.the_shop.catalog.domain.exception.CategoryNotFoundException;
import com.exemple.the_shop.catalog.domain.exception.IllegalCategoryParentException;
import com.exemple.the_shop.catalog.domain.exception.InsufficientStockException;
import com.exemple.the_shop.catalog.domain.exception.ProductNotFoundException;
import com.exemple.the_shop.catalog.domain.exception.StockNotFoundException;
import com.exemple.the_shop.shared.domain.exception.SlugAlreadyUsedException;
import com.exemple.the_shop.shared.web.ApiError;

/**
 * Mapping HTTP des exceptions <b>métier du module catalog</b>.
 *
 * <p>
 * Scopé via {@code basePackageClasses} sur lui-même : le handler vit à la
 * racine du package {@code web}, il couvre donc tous les sous-packages de
 * controllers (product, category…) quel que soit le découpage par feature, sur
 * le gabarit d'{@code AuthExceptionHandler}. Priorité haute (0) pour passer
 * avant le filet générique de {@code GlobalExceptionHandler}. À étendre avec les
 * exceptions produit/stock quand le controller produit arrivera.
 */
@Order(0)
@RestControllerAdvice(basePackageClasses = CatalogExceptionHandler.class)
public class CatalogExceptionHandler {

  /** Catégorie (ou ancêtre) introuvable → ressource absente. */
  @ExceptionHandler(CategoryNotFoundException.class)
  public ResponseEntity<ApiError> handleCategoryNotFound(CategoryNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiError.of(HttpStatus.NOT_FOUND, ex.getMessage()));
  }

  /** Produit introuvable → ressource absente. */
  @ExceptionHandler(ProductNotFoundException.class)
  public ResponseEntity<ApiError> handleProductNotFound(ProductNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiError.of(HttpStatus.NOT_FOUND, ex.getMessage()));
  }

  /** Stock d'un produit introuvable → ressource absente. */
  @ExceptionHandler(StockNotFoundException.class)
  public ResponseEntity<ApiError> handleStockNotFound(StockNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiError.of(HttpStatus.NOT_FOUND, ex.getMessage()));
  }

  /** Décrément sous le stock disponible → conflit avec l'état courant. */
  @ExceptionHandler(InsufficientStockException.class)
  public ResponseEntity<ApiError> handleInsufficientStock(InsufficientStockException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiError.of(HttpStatus.CONFLICT, ex.getMessage()));
  }

  /** Slug déjà pris → conflit avec une catégorie existante. */
  @ExceptionHandler(SlugAlreadyUsedException.class)
  public ResponseEntity<ApiError> handleSlugAlreadyUsed(SlugAlreadyUsedException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiError.of(HttpStatus.CONFLICT, ex.getMessage()));
  }

  /** Parent invalide (self-parent ou cycle) → conflit avec la hiérarchie existante. */
  @ExceptionHandler(IllegalCategoryParentException.class)
  public ResponseEntity<ApiError> handleIllegalParent(IllegalCategoryParentException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiError.of(HttpStatus.CONFLICT, ex.getMessage()));
  }
}
