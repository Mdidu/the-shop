package com.exemple.the_shop.catalog.application.product;

import com.exemple.the_shop.shared.application.PageResponse;

/**
 * Port de LECTURE du catalogue produit (côté « query » du CQRS). Distinct du
 * {@code ProductRepository} d'écriture (qui vit, lui, dans {@code
 * domain/port/out} et rend des agrégats) : ce port rend une projection plate
 * {@link ProductListItem}, façonnée par l'UI, et n'a donc pas sa place dans le
 * domaine. Read-only : aucune mutation ne passe par ici.
 *
 * <p>{@code includeNonActive} est décidé par le SERVEUR, jamais par le client :
 * {@code false} pour le listing public (force {@code status = ACTIVE}),
 * {@code true} pour la vue d'administration. Il est passé à part du {@link
 * ProductListQuery} (qui ne porte que les préoccupations client : fenêtrage,
 * filtre catégorie, tri).
 */
public interface ProductQueryRepository {

  PageResponse<ProductListItem> findProducts(ProductListQuery query, boolean includeNonActive);
}
