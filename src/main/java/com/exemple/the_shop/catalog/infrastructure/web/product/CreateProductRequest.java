package com.exemple.the_shop.catalog.infrastructure.web.product;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Corps de création d'un produit. Le slug est dérivé du nom côté domaine. */
public record CreateProductRequest(
    @NotBlank String name,
    String description,
    @NotNull UUID categoryId,
    @NotNull @PositiveOrZero BigDecimal price) {
}
