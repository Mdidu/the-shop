package com.exemple.the_shop.catalog.infrastructure.web.category;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.exemple.the_shop.catalog.application.category.CategoryResponse;
import com.exemple.the_shop.catalog.application.category.CategoryService;
import com.exemple.the_shop.catalog.domain.model.Category;
import com.exemple.the_shop.shared.domain.Slug;

import jakarta.validation.Valid;

@RestController
@RequestMapping("categories")
public class CategoryController {

  private final CategoryService categoryService;

  public CategoryController(CategoryService categoryService) {
    this.categoryService = categoryService;
  }

  @PostMapping
  public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
    Category category = Category.create(request.name(), request.parentId());
    return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(category));
  }

  @GetMapping("/{slug}")
  public ResponseEntity<CategoryResponse> get(@PathVariable String slug) {
    return ResponseEntity.ok(categoryService.getCategory(Slug.of(slug)));
  }

  @PatchMapping("/{slug}/rename")
  public ResponseEntity<CategoryResponse> rename(@PathVariable String slug,
      @Valid @RequestBody RenameCategoryRequest request) {
    return ResponseEntity.ok(categoryService.renameCategory(Slug.of(slug), request.name()));
  }

  @PatchMapping("/{slug}/move")
  public ResponseEntity<CategoryResponse> move(@PathVariable String slug,
      @RequestBody MoveCategoryRequest request) {
    return ResponseEntity.ok(categoryService.moveCategory(Slug.of(slug), request.parentId()));
  }
}
