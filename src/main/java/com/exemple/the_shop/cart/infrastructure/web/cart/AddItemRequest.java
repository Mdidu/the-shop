package com.exemple.the_shop.cart.infrastructure.web.cart;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Corps d'ajout au panier : le produit visé et la quantité (strictement positive). */
public record AddItemRequest(
    @NotNull UUID productId,
    @NotNull @Positive Integer quantity) {
}
