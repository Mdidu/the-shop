package com.exemple.the_shop.catalog.infrastructure.web.product;

import com.exemple.the_shop.catalog.application.product.ProductListQuery;
import com.exemple.the_shop.catalog.application.product.ProductSortField;
import com.exemple.the_shop.shared.application.SortDirection;

/**
 * Traduit les query params bruts d'un listing produit en {@link
 * ProductListQuery} maison. Factorisé ici car les deux portes d'entrée
 * (public {@code GET /products} et admin {@code GET /admin/products})
 * partagent exactement la même validation — éviter qu'elles divergent.
 *
 * <p>C'est le rideau de validation web : clamp du {@code size} (flexibilité
 * encadrée), et mapping du {@code sort} brut sur la whitelist {@link
 * ProductSortField} (défense anti-injection sur l'ORDER BY : un champ hors
 * whitelist lève {@code IllegalArgumentException} → 400). La visibilité
 * (ACTIVE only vs tout) n'est PAS ici : c'est le service qui la décide.
 */
final class ProductListQueryFactory {

  private static final int DEFAULT_SIZE = 20;
  private static final int MAX_SIZE = 100;

  private ProductListQueryFactory() {
  }

  static ProductListQuery from(Integer page, Integer size, String sort, String category) {
    int safePage = (page == null || page < 0) ? 0 : page;
    int safeSize = clampSize(size);
    String categorySlug = (category == null || category.isBlank()) ? null : category.trim();

    ProductSortField sortField = null;
    SortDirection sortDirection = null;
    if (sort != null && !sort.isBlank()) {
      String[] parts = sort.split(",");
      sortField = parseField(parts[0].trim());
      sortDirection = parts.length > 1 ? parseDirection(parts[1].trim()) : SortDirection.ASC;
    }

    return new ProductListQuery(safePage, safeSize, categorySlug, sortField, sortDirection);
  }

  private static int clampSize(Integer size) {
    if (size == null || size < 1) {
      return DEFAULT_SIZE;
    }
    return Math.min(size, MAX_SIZE);
  }

  private static ProductSortField parseField(String field) {
    return switch (field.toLowerCase()) {
      case "name" -> ProductSortField.NAME;
      case "price" -> ProductSortField.PRICE;
      default -> throw new IllegalArgumentException("Champ de tri non autorisé : " + field);
    };
  }

  private static SortDirection parseDirection(String direction) {
    return switch (direction.toLowerCase()) {
      case "asc" -> SortDirection.ASC;
      case "desc" -> SortDirection.DESC;
      default -> throw new IllegalArgumentException("Direction de tri invalide : " + direction);
    };
  }
}
