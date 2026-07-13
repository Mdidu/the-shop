package com.exemple.the_shop.cart.infrastructure.web.cart;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Corps de mise à jour d'une ligne : la nouvelle quantité (strictement positive). */
public record UpdateQuantityRequest(
    @NotNull @Positive Integer quantity) {
}
