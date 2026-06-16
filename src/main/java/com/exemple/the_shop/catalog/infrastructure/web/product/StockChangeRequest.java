package com.exemple.the_shop.catalog.infrastructure.web.product;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Corps d'un mouvement de stock (increase/decrease) : quantité strictement positive. */
public record StockChangeRequest(
    @NotNull @Positive Integer amount) {
}
