package com.exemple.the_shop.catalog.application.product;

import java.util.UUID;

import com.exemple.the_shop.shared.domain.Money;

/**
 * Commande de mise à jour partielle (PATCH) d'un produit. Chaque champ à
 * {@code null} signifie « ne pas modifier ». Le slug n'y figure pas : il est
 * scellé à la création et sert d'identité (passée à part au service).
 */
public record UpdateProductCommand(
    String name,
    String description,
    Money price,
    UUID categoryId) {
}
