package com.exemple.the_shop.catalog.infrastructure.web.category;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

/** Corps de création d'une catégorie. {@code parentId} nul = catégorie racine. */
public record CreateCategoryRequest(
    @NotBlank String name,
    UUID parentId) {
}
