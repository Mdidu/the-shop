package com.exemple.the_shop.catalog.application.product;

import java.math.BigDecimal;

import com.exemple.the_shop.catalog.domain.model.ProductStatus;

/**
 * Read model d'une vignette de produit dans un listing — projection plate
 * pilotée par les besoins UI, distincte de {@link ProductResponse} (détail).
 *
 * <p>Pas de factory {@code from(Product)} : cet item n'est PAS construit depuis
 * l'agrégat de domaine mais directement par la query de lecture (jointures
 * produit + catégorie + stock). {@code inStock} est dérivé en SQL
 * ({@code quantity > 0}) pour que la quantité brute ne sorte jamais de la base.
 *
 * <p>Exclus volontairement : l'{@code id} interne (le {@code slug} est la clé
 * publique) et la {@code description} (réservée à la page détail).
 */
public record ProductListItem(
    String slug,
    String name,
    BigDecimal price,
    String categoryName,
    String categorySlug,
    boolean inStock,
    ProductStatus status) {
}
