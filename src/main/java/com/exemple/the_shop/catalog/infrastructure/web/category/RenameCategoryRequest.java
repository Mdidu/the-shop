package com.exemple.the_shop.catalog.infrastructure.web.category;

import jakarta.validation.constraints.NotBlank;

/** Corps de renommage : le slug reste scellé, seul le libellé change. */
public record RenameCategoryRequest(
    @NotBlank String name) {
}
