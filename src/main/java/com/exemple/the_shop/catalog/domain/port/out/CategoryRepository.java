package com.exemple.the_shop.catalog.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import com.exemple.the_shop.catalog.domain.model.Category;
import com.exemple.the_shop.shared.domain.Slug;

public interface CategoryRepository {
  Optional<Category> findById(UUID categoryId);

  Optional<Category> findBySlug(Slug slug);

  void save(Category category);
}
