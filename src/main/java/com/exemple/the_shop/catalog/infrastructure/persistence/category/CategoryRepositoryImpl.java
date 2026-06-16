package com.exemple.the_shop.catalog.infrastructure.persistence.category;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.exemple.the_shop.catalog.domain.model.Category;
import com.exemple.the_shop.catalog.domain.port.out.CategoryRepository;
import com.exemple.the_shop.shared.domain.Slug;

@Repository
public class CategoryRepositoryImpl implements CategoryRepository {

  private final CategoryJpaRepository categoryJpaRepository;

  public CategoryRepositoryImpl(CategoryJpaRepository categoryJpaRepository) {
    this.categoryJpaRepository = categoryJpaRepository;
  }

  @Override
  public Optional<Category> findById(UUID categoryId) {
    return categoryJpaRepository.findById(categoryId).map(CategoryMapper::toDomain);
  }

  @Override
  public Optional<Category> findBySlug(Slug slug) {
    return categoryJpaRepository.findBySlug(slug.value()).map(CategoryMapper::toDomain);
  }

  @Override
  public void save(Category category) {
    CategoryJpaEntity entity = CategoryMapper.toJpaEntity(category);
    categoryJpaRepository.save(entity);
  }
}
