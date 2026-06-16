package com.exemple.the_shop.catalog.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.exemple.the_shop.catalog.domain.exception.IllegalProductStatusTransitionException;
import com.exemple.the_shop.shared.domain.Money;
import com.exemple.the_shop.shared.domain.Slug;

public class Product {
  private final UUID id;
  private final String name;
  private final Slug slug;
  private final String description;
  private final UUID categoryId;
  private final Money price;
  private final ProductStatus status;
  private final int version;
  private final Instant createdAt;
  private final Instant updatedAt;

  public Product(UUID id, String name, Slug slug, String description, UUID categoryId, Money price,
      ProductStatus status, int version, Instant createdAt, Instant updatedAt) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Le nom du produit ne peut pas être vide");
    }
    this.id = id;
    this.name = name;
    this.slug = slug;
    this.description = description;
    this.categoryId = categoryId;
    this.price = price;
    this.status = status;
    this.version = version;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static Product create(String name, String description, UUID categoryId, Money price) {
    return new Product(UUID.randomUUID(), name, Slug.fromName(name), description, categoryId, price,
        ProductStatus.DRAFT, 0, null, null);
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Slug getSlug() {
    return slug;
  }

  public String getDescription() {
    return description;
  }

  public UUID getCategoryId() {
    return categoryId;
  }

  public Money getPrice() {
    return price;
  }

  public ProductStatus getStatus() {
    return status;
  }

  public int getVersion() {
    return version;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Product activate() {
    if (this.status == ProductStatus.ACTIVE) {
      throw new IllegalProductStatusTransitionException("Impossible d'activer un produit déjà activé !");
    }
    return new Product(
        id,
        name,
        slug,
        description,
        categoryId,
        price,
        ProductStatus.ACTIVE,
        version,
        createdAt, updatedAt);
  }

  public Product deactivate() {
    if (this.status != ProductStatus.ACTIVE) {
      throw new IllegalProductStatusTransitionException("Impossible de désactiver un produit non activé !");
    }
    return new Product(
        id,
        name,
        slug,
        description,
        categoryId,
        price,
        ProductStatus.INACTIVE,
        version,
        createdAt, updatedAt);
  }

  public Product updatePrice(Money newPrice) {
    return new Product(
        id,
        name,
        slug,
        description,
        categoryId,
        newPrice,
        status,
        version,
        createdAt, updatedAt);
  }

  public Product updateName(String newName) {
    return new Product(
        id,
        newName,
        slug,
        description,
        categoryId,
        price,
        status,
        version,
        createdAt, updatedAt);
  }

  public Product updateDescription(String newDescription) {
    return new Product(
        id,
        name,
        slug,
        newDescription,
        categoryId,
        price,
        status,
        version,
        createdAt, updatedAt);
  }

  public Product updateCategory(UUID newCategoryId) {
    return new Product(
        id,
        name,
        slug,
        description,
        newCategoryId,
        price,
        status,
        version,
        createdAt, updatedAt);
  }
}
