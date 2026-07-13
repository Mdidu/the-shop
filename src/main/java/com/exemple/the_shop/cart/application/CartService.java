package com.exemple.the_shop.cart.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.exemple.the_shop.cart.domain.exception.CartItemNotFoundException;
import com.exemple.the_shop.cart.domain.exception.ProductNotAvailableException;
import com.exemple.the_shop.cart.domain.exception.ProductNotFoundException;
import com.exemple.the_shop.cart.domain.model.Cart;
import com.exemple.the_shop.cart.domain.port.out.CartRepository;
import com.exemple.the_shop.cart.domain.port.out.ProductCatalogPort;
import com.exemple.the_shop.cart.domain.port.out.ProductSnapshot;

@Service
public class CartService {
  private final CartRepository cartRepository;
  private final ProductCatalogPort productCatalogPort;

  public CartService(CartRepository cartRepository, ProductCatalogPort productCatalogPort) {
    this.cartRepository = cartRepository;
    this.productCatalogPort = productCatalogPort;
  }

  @Transactional(readOnly = true)
  public CartResponse getCart(UUID userId) {
    return cartRepository.findByUserId(userId)
        .map(CartResponse::from)
        .orElseGet(CartResponse::empty);
  }

  @Transactional
  public CartResponse clearCart(UUID userId) {
    return cartRepository.findByUserId(userId)
        .map(cart -> {
          Cart cleared = cart.clear();
          cartRepository.save(cleared);
          return CartResponse.from(cleared);
        })
        .orElseGet(CartResponse::empty);
  }

  @Transactional
  public CartResponse addItem(UUID userId, UUID productId, int quantity) {
    ProductSnapshot product = productCatalogPort.findProduct(productId)
        .orElseThrow(() -> new ProductNotFoundException("Produit introuvable : " + productId));
    if (!product.isActive()) {
      throw new ProductNotAvailableException("Produit non disponible : " + productId);
    }

    Cart cart = cartRepository.findByUserId(userId)
        .orElseGet(() -> Cart.create(userId));

    Cart updated = cart.addItem(productId, quantity, product.price());
    cartRepository.save(updated);

    return CartResponse.from(updated);
  }

  @Transactional
  public CartResponse updateQuantity(UUID userId, UUID productId, int newQuantity) {
    Cart cart = loadCartOrThrow(userId, productId);
    Cart updated = cart.updateItemQuantity(productId, newQuantity);
    cartRepository.save(updated);
    return CartResponse.from(updated);
  }

  @Transactional
  public CartResponse removeItem(UUID userId, UUID productId) {
    Cart cart = loadCartOrThrow(userId, productId);
    Cart updated = cart.removeItem(productId);
    cartRepository.save(updated);
    return CartResponse.from(updated);
  }

  private Cart loadCartOrThrow(UUID userId, UUID productId) {
    return cartRepository.findByUserId(userId)
        .orElseThrow(() -> new CartItemNotFoundException(
            "Aucune ligne pour le produit " + productId + " dans le panier"));
  }
}
