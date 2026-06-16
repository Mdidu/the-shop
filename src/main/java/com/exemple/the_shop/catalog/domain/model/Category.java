package com.exemple.the_shop.catalog.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.exemple.the_shop.catalog.domain.exception.IllegalCategoryParentException;
import com.exemple.the_shop.shared.domain.Slug;

public class Category {

  private final UUID id;
  private final String name;
  private final Slug slug;
  private final UUID parentId;
  private final Instant createdAt;

  public Category(UUID id, String name, Slug slug, UUID parentId, Instant createdAt) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Le nom de la catégorie ne peut pas être vide");
    }
    this.id = id;
    this.name = name;
    this.slug = slug;
    this.parentId = parentId;
    this.createdAt = createdAt;
  }

  /**
   * Crée une nouvelle catégorie : l'identité ({@code id}) est générée ici, et le
   * {@code slug} est dérivé du {@code name} (figé à la création — un renommage
   * ultérieur ne le recalcule pas). {@code parentId} est nul pour une catégorie
   * racine. {@code createdAt} reste nul : attribué par la persistence
   * ({@code @PrePersist}).
   */
  public static Category create(String name, UUID parentId) {
    return new Category(UUID.randomUUID(), name, Slug.fromName(name), parentId, null);
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

  public UUID getParentId() {
    return parentId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Renomme la catégorie. Le slug est scellé : un renommage ne le recalcule pas. */
  public Category rename(String newName) {
    return new Category(id, newName, slug, parentId, createdAt);
  }

  /**
   * Déplace la catégorie sous un nouveau parent ({@code null} pour la remonter en
   * racine). Refuse le cas trivial où la catégorie se prendrait elle-même comme
   * parent ; la détection des cycles indirects (parent qui est un descendant)
   * vit dans le service, car elle nécessite de remonter la chaîne via le repository.
   */
  public Category moveTo(UUID newParentId) {
    if (id.equals(newParentId)) {
      throw new IllegalCategoryParentException("Une catégorie ne peut pas être son propre parent");
    }
    return new Category(id, name, slug, newParentId, createdAt);
  }
}
