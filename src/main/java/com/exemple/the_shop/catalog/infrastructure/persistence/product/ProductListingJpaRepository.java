package com.exemple.the_shop.catalog.infrastructure.persistence.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.exemple.the_shop.catalog.application.product.ProductListItem;
import com.exemple.the_shop.catalog.domain.model.ProductStatus;

/**
 * Repository Spring Data dédié au read model du listing produit. Volontairement
 * {@code Repository} (et non {@code JpaRepository}) : le côté lecture n'hérite
 * d'aucune méthode d'écriture CRUD — séparation lecture/écriture du CQRS.
 *
 * <p>La query projette directement vers {@link ProductListItem} (constructor
 * expression). Jointures par {@code ON} (pas d'association JPA) : {@code INNER}
 * sur la catégorie (FK {@code NOT NULL}), {@code LEFT} sur le stock pour qu'un
 * produit sans ligne de stock reste visible avec {@code inStock = false}. Le
 * tri et la fenêtre arrivent par le {@link Pageable} (le tie-breaker
 * déterministe est ajouté par l'adapter).
 */
public interface ProductListingJpaRepository extends Repository<ProductJpaEntity, java.util.UUID> {

  @Query(value = """
      SELECT new com.exemple.the_shop.catalog.application.product.ProductListItem(
          p.slug,
          p.name,
          p.price,
          c.name,
          c.slug,
          CASE WHEN s.quantity > 0 THEN TRUE ELSE FALSE END,
          p.status)
      FROM ProductJpaEntity p
      JOIN CategoryJpaEntity c ON c.id = p.categoryId
      LEFT JOIN ProductStockJpaEntity s ON s.productId = p.id
      WHERE (:categorySlug IS NULL OR c.slug = :categorySlug)
        AND (:includeNonActive = TRUE OR p.status = :activeStatus)
      """,
      countQuery = """
          SELECT count(p)
          FROM ProductJpaEntity p
          JOIN CategoryJpaEntity c ON c.id = p.categoryId
          WHERE (:categorySlug IS NULL OR c.slug = :categorySlug)
            AND (:includeNonActive = TRUE OR p.status = :activeStatus)
          """)
  Page<ProductListItem> findListing(
      @Param("categorySlug") String categorySlug,
      @Param("includeNonActive") boolean includeNonActive,
      @Param("activeStatus") ProductStatus activeStatus,
      Pageable pageable);
}
