package com.exemple.the_shop.cart.domain.port.out;

import java.util.UUID;

import com.exemple.the_shop.shared.domain.Money;

public record ProductSnapshot(UUID productId, Money price, boolean isActive) {

}
