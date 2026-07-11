package com.exemple.the_shop.cart.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import com.exemple.the_shop.cart.domain.model.Cart;

public interface CartRepository {
  Optional<Cart> findByUserId(UUID userId);

  void save(Cart cart);
}
