package com.exemple.the_shop.catalog.infrastructure.web.product;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.exemple.the_shop.catalog.application.product.ProductResponse;
import com.exemple.the_shop.catalog.application.product.ProductService;
import com.exemple.the_shop.catalog.application.stock.ProductStockResponse;
import com.exemple.the_shop.catalog.application.stock.ProductStockService;
import com.exemple.the_shop.catalog.application.product.UpdateProductCommand;
import com.exemple.the_shop.catalog.domain.model.Product;
import com.exemple.the_shop.shared.domain.Money;
import com.exemple.the_shop.shared.domain.Slug;

import jakarta.validation.Valid;

@RestController
@RequestMapping("products")
public class ProductController {

  private final ProductService productService;
  private final ProductStockService productStockService;

  public ProductController(ProductService productService, ProductStockService productStockService) {
    this.productService = productService;
    this.productStockService = productStockService;
  }

  @PostMapping
  public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
    Product product = Product.create(request.name(), request.description(), request.categoryId(),
        Money.of(request.price()));
    return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(product));
  }

  @GetMapping("/{slug}")
  public ResponseEntity<ProductResponse> get(@PathVariable String slug) {
    return ResponseEntity.ok(productService.getProduct(Slug.of(slug)));
  }

  @PatchMapping("/{slug}")
  public ResponseEntity<ProductResponse> update(@PathVariable String slug,
      @Valid @RequestBody UpdateProductRequest request) {
    UpdateProductCommand command = new UpdateProductCommand(
        request.name(),
        request.description(),
        request.price() != null ? Money.of(request.price()) : null,
        request.categoryId());
    return ResponseEntity.ok(productService.updateProduct(Slug.of(slug), command));
  }

  @PostMapping("/{slug}/activate")
  public ResponseEntity<ProductResponse> activate(@PathVariable String slug) {
    return ResponseEntity.ok(productService.activateProduct(Slug.of(slug)));
  }

  @PostMapping("/{slug}/deactivate")
  public ResponseEntity<ProductResponse> deactivate(@PathVariable String slug) {
    return ResponseEntity.ok(productService.deactivateProduct(Slug.of(slug)));
  }

  @GetMapping("/{slug}/stock")
  public ResponseEntity<ProductStockResponse> getStock(@PathVariable String slug) {
    return ResponseEntity.ok(productStockService.getStockByProduct(resolveProductId(slug)));
  }

  @PostMapping("/{slug}/stock/increase")
  public ResponseEntity<ProductStockResponse> increaseStock(@PathVariable String slug,
      @Valid @RequestBody StockChangeRequest request) {
    return ResponseEntity.ok(productStockService.increaseStock(resolveProductId(slug), request.amount()));
  }

  @PostMapping("/{slug}/stock/decrease")
  public ResponseEntity<ProductStockResponse> decreaseStock(@PathVariable String slug,
      @Valid @RequestBody StockChangeRequest request) {
    return ResponseEntity.ok(productStockService.decreaseStock(resolveProductId(slug), request.amount()));
  }

  /** Résout l'identité interne (UUID) à partir du slug d'URL ; 404 si le produit n'existe pas. */
  private UUID resolveProductId(String slug) {
    return productService.getProduct(Slug.of(slug)).id();
  }
}
