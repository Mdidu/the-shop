package com.exemple.the_shop.catalog.infrastructure.persistence.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.exemple.the_shop.catalog.application.product.ProductListItem;
import com.exemple.the_shop.catalog.application.product.ProductListQuery;
import com.exemple.the_shop.catalog.application.product.ProductQueryRepository;
import com.exemple.the_shop.catalog.application.product.ProductSortField;
import com.exemple.the_shop.catalog.domain.model.ProductStatus;
import com.exemple.the_shop.shared.application.PageResponse;
import com.exemple.the_shop.shared.application.SortDirection;

/**
 * Adapter du port de lecture {@link ProductQueryRepository}. Point de
 * confinement : c'est le SEUL endroit où le {@code Pageable}/{@code Sort}/{@code
 * Page} de Spring existent. Il traduit le {@link ProductListQuery} maison vers
 * Spring, ajoute le tie-breaker déterministe ({@code id asc}) pour une
 * pagination stable sur les ex æquo, exécute, puis reconvertit la {@code Page}
 * Spring en {@link PageResponse} maison.
 */
@Repository
public class ProductQueryRepositoryImpl implements ProductQueryRepository {

  private final ProductListingJpaRepository listingJpaRepository;

  public ProductQueryRepositoryImpl(ProductListingJpaRepository listingJpaRepository) {
    this.listingJpaRepository = listingJpaRepository;
  }

  @Override
  public PageResponse<ProductListItem> findProducts(ProductListQuery query, boolean includeNonActive) {
    PageRequest pageRequest = PageRequest.of(query.page(), query.size(), toSort(query));

    Page<ProductListItem> page = listingJpaRepository.findListing(
        query.categorySlug(),
        includeNonActive,
        ProductStatus.ACTIVE,
        pageRequest);

    return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
  }

  /**
   * Tri demandé + tie-breaker {@code id asc} toujours en dernier critère. Sans
   * tie-breaker déterministe, deux ex æquo (ex. même prix) peuvent être
   * ré-ordonnés entre deux requêtes → un produit apparaît en page 1 ET page 2.
   */
  private Sort toSort(ProductListQuery query) {
    Sort tieBreaker = Sort.by(Sort.Direction.ASC, "id");
    if (query.sortField() == null) {
      return tieBreaker;
    }
    Sort primary = Sort.by(toDirection(query.sortDirection()), toProperty(query.sortField()));
    return primary.and(tieBreaker);
  }

  /** Mappe la whitelist applicative vers la propriété d'entité (jamais la valeur brute du client). */
  private String toProperty(ProductSortField field) {
    return switch (field) {
      case NAME -> "name";
      case PRICE -> "price";
    };
  }

  private Sort.Direction toDirection(SortDirection direction) {
    return direction == SortDirection.DESC ? Sort.Direction.DESC : Sort.Direction.ASC;
  }
}
