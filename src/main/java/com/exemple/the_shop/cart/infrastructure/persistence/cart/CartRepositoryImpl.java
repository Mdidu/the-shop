package com.exemple.the_shop.cart.infrastructure.persistence.cart;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.exemple.the_shop.cart.domain.model.Cart;
import com.exemple.the_shop.cart.domain.model.CartItem;
import com.exemple.the_shop.cart.domain.port.out.CartRepository;

@Repository
public class CartRepositoryImpl implements CartRepository {

  private final CartJpaRepository cartJpaRepository;

  public CartRepositoryImpl(CartJpaRepository cartJpaRepository) {
    this.cartJpaRepository = cartJpaRepository;
  }

  @Override
  public Optional<Cart> findByUserId(UUID userId) {
    return cartJpaRepository.findByUserId(userId).map(CartMapper::toDomain);
  }

  /**
   * IMPORTANT : la branche « cart existant » charge une entity MANAGÉE et la mute
   * (dirty checking + orphanRemoval). Le flush n'a lieu qu'au commit → la méthode
   * de service appelante DOIT être @Transactional, sinon l'entity est détachée
   * dès le retour de findById et les mutations sont perdues.
   */
  @Override
  public void save(Cart cart) {
    CartJpaEntity managed = cartJpaRepository.findById(cart.getId()).orElse(null);
    if (managed == null) {
      // Cart jamais persisté (get-or-create au 1er addItem) → INSERT complet.
      cartJpaRepository.save(CartMapper.toJpaEntity(cart));
      return;
    }
    // Cart existant → on synchronise la collection managée par id.
    syncItems(managed, cart.getItems());
  }

  /**
   * Synchro par id, en mutant la collection managée EN PLACE (jamais de
   * réassignation) pour préserver le suivi orphanRemoval du PersistentBag :
   * - id déjà présent → UPDATE ciblé de la quantité (created_at préservé)
   * - id nouveau → INSERT
   * - id disparu du domaine → orphelin → DELETE
   */
  private void syncItems(CartJpaEntity managed, List<CartItem> domainItems) {
    Map<UUID, CartItemJpaEntity> managedById = managed.getCartItems().stream()
        .collect(Collectors.toMap(CartItemJpaEntity::getId, Function.identity()));

    Set<UUID> domainIds = new HashSet<>();
    for (CartItem domainItem : domainItems) {
      domainIds.add(domainItem.getId());
      CartItemJpaEntity existing = managedById.get(domainItem.getId());
      if (existing != null) {
        existing.setQuantity(domainItem.getQuantity());
      } else {
        managed.getCartItems().add(CartMapper.toJpaEntity(domainItem));
      }
    }
    managed.getCartItems().removeIf(item -> !domainIds.contains(item.getId()));
  }
}
