package com.exemple.the_shop.cart.domain.port.out;

import java.util.Optional;
import java.util.UUID;

public interface ProductCatalogPort {
  Optional<ProductSnapshot> findProduct(UUID productId);
}
