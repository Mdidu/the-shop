package com.exemple.the_shop.catalog.application.category;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.exemple.the_shop.catalog.domain.exception.CategoryNotFoundException;
import com.exemple.the_shop.catalog.domain.exception.IllegalCategoryParentException;
import com.exemple.the_shop.catalog.domain.model.Category;
import com.exemple.the_shop.catalog.domain.port.out.CategoryRepository;
import com.exemple.the_shop.shared.domain.Slug;
import com.exemple.the_shop.shared.domain.exception.SlugAlreadyUsedException;

@Service
public class CategoryService {

  private final CategoryRepository categoryRepository;

  public CategoryService(CategoryRepository categoryRepository) {
    this.categoryRepository = categoryRepository;
  }

  @Transactional
  public CategoryResponse createCategory(Category category) {
    if (category.getParentId() != null) {
      requireCategoryExists(category.getParentId());
    }
    if (categoryRepository.findBySlug(category.getSlug()).isPresent()) {
      throw new SlugAlreadyUsedException("La catégorie existe déjà : " + category.getSlug().value());
    }
    categoryRepository.save(category);

    return CategoryResponse.from(category);
  }

  @Transactional
  public CategoryResponse renameCategory(Slug slug, String newName) {
    Category renamed = loadBySlug(slug).rename(newName);
    categoryRepository.save(renamed);

    return CategoryResponse.from(renamed);
  }

  @Transactional
  public CategoryResponse moveCategory(Slug slug, UUID newParentId) {
    Category category = loadBySlug(slug);
    if (newParentId != null) {
      assertNoCycle(category.getId(), newParentId);
    }
    Category moved = category.moveTo(newParentId);
    categoryRepository.save(moved);

    return CategoryResponse.from(moved);
  }

  @Transactional(readOnly = true)
  public CategoryResponse getCategory(Slug slug) {
    return CategoryResponse.from(loadBySlug(slug));
  }

  private Category loadBySlug(Slug slug) {
    return categoryRepository.findBySlug(slug)
        .orElseThrow(() -> new CategoryNotFoundException("Catégorie non trouvée : " + slug.value()));
  }

  private Category requireCategoryExists(UUID categoryId) {
    return categoryRepository.findById(categoryId)
        .orElseThrow(() -> new CategoryNotFoundException("Catégorie non trouvée : " + categoryId));
  }

  /**
   * Refuse un déplacement qui créerait un cycle : on remonte la chaîne des ancêtres
   * depuis {@code newParentId} ; si on recroise {@code categoryId}, c'est que le
   * futur parent est en réalité un descendant de la catégorie déplacée. La remontée
   * valide aussi l'existence de chaque ancêtre (donc du parent lui-même).
   */
  private void assertNoCycle(UUID categoryId, UUID newParentId) {
    UUID current = newParentId;
    while (current != null) {
      if (current.equals(categoryId)) {
        throw new IllegalCategoryParentException(
            "Déplacement impossible : la catégorie deviendrait son propre ancêtre");
      }
      current = requireCategoryExists(current).getParentId();
    }
  }
}
