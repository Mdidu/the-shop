package com.exemple.the_shop.shared.application;

import java.util.List;

/**
 * Enveloppe de pagination « maison », générique et réutilisable (produits,
 * catégories…). Volontairement indépendante de Spring : la {@code Page} de
 * Spring est confinée à l'adapter de persistence, qui construit ce
 * {@code PageResponse} au dernier moment.
 *
 * <p>{@code page} et {@code size} sont l'écho de la requête (position et
 * largeur de la fenêtre). {@code totalElements} coûte un {@code COUNT} séparé
 * et sert l'affichage du total. {@code totalPages} est volontairement absent :
 * dérivable via {@code ceil(totalElements / size)}.
 */
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements) {
}
