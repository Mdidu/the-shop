package com.exemple.the_shop.catalog.application.product;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.exemple.the_shop.catalog.domain.exception.CategoryNotFoundException;
import com.exemple.the_shop.catalog.domain.exception.ProductNotFoundException;
import com.exemple.the_shop.catalog.domain.model.Product;
import com.exemple.the_shop.catalog.domain.model.ProductStock;
import com.exemple.the_shop.catalog.domain.port.out.CategoryRepository;
import com.exemple.the_shop.catalog.domain.port.out.ProductRepository;
import com.exemple.the_shop.catalog.domain.port.out.ProductStockRepository;
import com.exemple.the_shop.shared.domain.Slug;
import com.exemple.the_shop.shared.domain.exception.SlugAlreadyUsedException;

@Service
public class ProductService {
  private static final Logger log = LoggerFactory.getLogger(ProductService.class);

  private final ProductRepository productRepository;
  private final ProductStockRepository productStockRepository;
  private final CategoryRepository categoryRepository;

  public ProductService(ProductRepository productRepository, ProductStockRepository productStockRepository,
      CategoryRepository categoryRepository) {
    this.productRepository = productRepository;
    this.productStockRepository = productStockRepository;
    this.categoryRepository = categoryRepository;
  }

  @Transactional
  public ProductResponse createProduct(Product product) {
    requireCategoryExists(product.getCategoryId());
    if (productRepository.findBySlug(product.getSlug()).isPresent()) {
      throw new SlugAlreadyUsedException("Le produit existe déjà : " + product.getSlug().value());
    }
    productRepository.save(product);
    ProductStock productStock = ProductStock.create(product.getId(), 0);
    productStockRepository.save(productStock);

    return ProductResponse.from(product, productStock.getQuantity());
  }

  @Transactional
  public ProductResponse activateProduct(Slug slug) {
    Product activated = loadBySlug(slug).activate();
    productRepository.save(activated);

    return ProductResponse.from(activated, resolveQuantity(activated.getId()));
  }

  @Transactional
  public ProductResponse deactivateProduct(Slug slug) {
    Product deactivated = loadBySlug(slug).deactivate();
    productRepository.save(deactivated);

    return ProductResponse.from(deactivated, resolveQuantity(deactivated.getId()));
  }

  @Transactional
  public ProductResponse updateProduct(Slug slug, UpdateProductCommand command) {
    Product updated = loadBySlug(slug);

    if (command.name() != null) {
      updated = updated.updateName(command.name());
    }
    if (command.description() != null) {
      updated = updated.updateDescription(command.description());
    }
    if (command.price() != null) {
      updated = updated.updatePrice(command.price());
    }
    if (command.categoryId() != null) {
      requireCategoryExists(command.categoryId());
      updated = updated.updateCategory(command.categoryId());
    }

    productRepository.save(updated);

    return ProductResponse.from(updated, resolveQuantity(updated.getId()));
  }

  @Transactional(readOnly = true)
  public ProductResponse getProduct(Slug slug) {
    Product product = loadBySlug(slug);
    return ProductResponse.from(product, resolveQuantity(product.getId()));
  }

  private Product loadBySlug(Slug slug) {
    return productRepository.findBySlug(slug)
        .orElseThrow(() -> new ProductNotFoundException("Le produit n'existe pas : " + slug.value()));
  }

  private void requireCategoryExists(UUID categoryId) {
    categoryRepository.findById(categoryId)
        .orElseThrow(() -> new CategoryNotFoundException("Catégorie non trouvée : " + categoryId));
  }

  private int resolveQuantity(UUID productId) {
    return productStockRepository.findByProductId(productId)
        .map(ProductStock::getQuantity)
        .orElseGet(() -> {
          log.warn("Stock introuvable pour le produit {} — devrait toujours exister", productId);
          return 0;
        });
  }
}
