package com.exemple.the_shop.cart.infrastructure.persistence.cart;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "carts")
public class CartJpaEntity {
  @Id
  private UUID id;

  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "cart_id", nullable = false)
  private List<CartItemJpaEntity> cartItems = new ArrayList<>();

  protected CartJpaEntity() {
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public List<CartItemJpaEntity> getCartItems() {
    return cartItems;
  }

  public void setCartItems(List<CartItemJpaEntity> cartItems) {
    // On MUTE la collection en place (clear + addAll), on ne réassigne jamais la
    // référence : Hibernate suit ce wrapper (PersistentBag) pour l'orphanRemoval.
    this.cartItems.clear();
    if (cartItems != null) {
      this.cartItems.addAll(cartItems);
    }
  }

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
    updatedAt = Instant.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = Instant.now();
  }
}
