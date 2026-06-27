package com.exemple.the_shop.catalog.application.product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.exemple.the_shop.shared.application.PageResponse;

/**
 * Service de LECTURE des listings produit (côté query du CQRS), distinct du
 * {@link ProductService} d'écriture. Les deux méthodes nomment la décision de
 * visibilité (le booléen {@code includeNonActive} du port) plutôt que de la
 * laisser au controller : {@link #listActiveProducts} pour le listing public,
 * {@link #listAllProducts} pour la vue d'administration.
 */
@Service
public class ProductQueryService {

  private final ProductQueryRepository productQueryRepository;

  public ProductQueryService(ProductQueryRepository productQueryRepository) {
    this.productQueryRepository = productQueryRepository;
  }

  /** Listing public : produits ACTIVE uniquement. */
  @Transactional(readOnly = true)
  public PageResponse<ProductListItem> listActiveProducts(ProductListQuery query) {
    return productQueryRepository.findProducts(query, false);
  }

  /** Listing d'administration : tous statuts (DRAFT / ACTIVE / INACTIVE). */
  @Transactional(readOnly = true)
  public PageResponse<ProductListItem> listAllProducts(ProductListQuery query) {
    return productQueryRepository.findProducts(query, true);
  }
}
