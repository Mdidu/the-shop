package com.exemple.the_shop.cart.infrastructure.web.cart;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.exemple.the_shop.cart.application.CartResponse;
import com.exemple.the_shop.cart.application.CartService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("cart")
public class CartController {

  private final CartService cartService;

  public CartController(CartService cartService) {
    this.cartService = cartService;
  }

  @GetMapping
  public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal String userId) {
    return ResponseEntity.ok(cartService.getCart(UUID.fromString(userId)));
  }

  @DeleteMapping
  public ResponseEntity<CartResponse> clearCart(@AuthenticationPrincipal String userId) {
    return ResponseEntity.ok(cartService.clearCart(UUID.fromString(userId)));
  }

  @DeleteMapping("/items/{productId}")
  public ResponseEntity<CartResponse> removeItem(@AuthenticationPrincipal String userId,
      @PathVariable UUID productId) {
    return ResponseEntity.ok(cartService.removeItem(UUID.fromString(userId), productId));
  }

  @PostMapping("/items")
  public ResponseEntity<CartResponse> addItem(@AuthenticationPrincipal String userId,
      @Valid @RequestBody AddItemRequest request) {
    return ResponseEntity.ok(
        cartService.addItem(UUID.fromString(userId), request.productId(), request.quantity()));
  }

  @PutMapping("/items/{productId}")
  public ResponseEntity<CartResponse> updateQuantity(@AuthenticationPrincipal String userId,
      @PathVariable UUID productId, @Valid @RequestBody UpdateQuantityRequest request) {
    return ResponseEntity.ok(
        cartService.updateQuantity(UUID.fromString(userId), productId, request.quantity()));
  }
}
