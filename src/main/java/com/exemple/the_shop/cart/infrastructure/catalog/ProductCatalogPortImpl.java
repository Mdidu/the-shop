package com.exemple.the_shop.cart.infrastructure.catalog;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.exemple.the_shop.cart.domain.port.out.ProductCatalogPort;
import com.exemple.the_shop.cart.domain.port.out.ProductSnapshot;
import com.exemple.the_shop.catalog.domain.model.Product;
import com.exemple.the_shop.catalog.domain.model.ProductStatus;
import com.exemple.the_shop.catalog.domain.port.out.ProductRepository;

@Component
public class ProductCatalogPortImpl implements ProductCatalogPort {
  private final ProductRepository productRepository;

  public ProductCatalogPortImpl(ProductRepository productRepository) {
    this.productRepository = productRepository;
  }

  @Override
  public Optional<ProductSnapshot> findProduct(UUID productId) {
    return productRepository.findById(productId).map(this::toSnapshot);
  }

  private ProductSnapshot toSnapshot(Product product) {
    return new ProductSnapshot(product.getId(), product.getPrice(), product.getStatus() == ProductStatus.ACTIVE);
  }

}
