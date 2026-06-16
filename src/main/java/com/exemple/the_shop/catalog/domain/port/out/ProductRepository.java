package com.exemple.the_shop.catalog.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.exemple.the_shop.catalog.domain.model.Product;
import com.exemple.the_shop.shared.domain.Slug;

public interface ProductRepository {
  Optional<Product> findById(UUID id);

  Optional<Product> findBySlug(Slug slug);

  List<Product> findAllByCategoryId(UUID categoryId);

  void save(Product product);
}
