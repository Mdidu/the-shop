package com.exemple.the_shop.catalog.application.category;

import java.util.UUID;

import com.exemple.the_shop.catalog.domain.model.Category;

public record CategoryResponse(
    UUID id,
    String name,
    String slug,
    UUID parentId) {

  public static CategoryResponse from(Category category) {
    return new CategoryResponse(
        category.getId(),
        category.getName(),
        category.getSlug().value(),
        category.getParentId());
  }
}
