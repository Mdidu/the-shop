package com.exemple.the_shop.catalog.infrastructure.web.product;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * Corps de mise à jour partielle (PATCH) d'un produit : chaque champ nul = « ne
 * pas modifier ». Le slug n'y figure pas (scellé, sert d'identité dans l'URL).
 */
public record UpdateProductRequest(
    String name,
    String description,
    @PositiveOrZero BigDecimal price,
    UUID categoryId) {
}
