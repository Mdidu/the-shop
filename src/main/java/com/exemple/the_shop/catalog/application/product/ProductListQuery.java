package com.exemple.the_shop.catalog.application.product;

import com.exemple.the_shop.shared.application.SortDirection;

/**
 * Critère de listing produit, sous forme « maison » : c'est le point de
 * confinement amont. Le controller reçoit les query params bruts, les valide
 * (clamp du {@code size}, mapping du tri sur la whitelist {@link
 * ProductSortField}) puis construit ce record. Aucun type de pagination Spring
 * ne remonte jusqu'ici.
 *
 * <p>{@code categorySlug} à {@code null} = pas de filtre catégorie. La
 * visibilité (ACTIVE only vs vue complète admin) n'est PAS portée ici : c'est
 * un flag décidé par le serveur, passé à part au port.
 */
public record ProductListQuery(
    int page,
    int size,
    String categorySlug,
    ProductSortField sortField,
    SortDirection sortDirection) {
}
