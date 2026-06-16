package com.exemple.the_shop.catalog.infrastructure.web.category;

import java.util.UUID;

/** Corps de déplacement. {@code parentId} nul = remonter la catégorie en racine. */
public record MoveCategoryRequest(
    UUID parentId) {
}
