package com.exemple.the_shop.catalog.infrastructure.web.product;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.exemple.the_shop.catalog.application.product.ProductListItem;
import com.exemple.the_shop.catalog.application.product.ProductListQuery;
import com.exemple.the_shop.catalog.application.product.ProductQueryService;
import com.exemple.the_shop.shared.application.PageResponse;

/**
 * Vue d'administration du catalogue. Sous le préfixe {@code /admin/**}, donc
 * gardé par {@code hasRole("ADMIN")} dans {@code SecurityConfig}. Le listing
 * voit TOUS les statuts (DRAFT / ACTIVE / INACTIVE), à l'inverse du {@code
 * GET /products} public qui force ACTIVE.
 */
@RestController
@RequestMapping("/admin/products")
public class AdminProductController {

  private final ProductQueryService productQueryService;

  public AdminProductController(ProductQueryService productQueryService) {
    this.productQueryService = productQueryService;
  }

  @GetMapping
  public ResponseEntity<PageResponse<ProductListItem>> list(
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String category) {
    ProductListQuery query = ProductListQueryFactory.from(page, size, sort, category);
    return ResponseEntity.ok(productQueryService.listAllProducts(query));
  }
}
