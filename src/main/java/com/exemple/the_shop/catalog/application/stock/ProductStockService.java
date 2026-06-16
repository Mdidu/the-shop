package com.exemple.the_shop.catalog.application.stock;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.exemple.the_shop.catalog.domain.exception.StockNotFoundException;
import com.exemple.the_shop.catalog.domain.model.ProductStock;
import com.exemple.the_shop.catalog.domain.port.out.ProductStockRepository;

@Service
public class ProductStockService {

  private final ProductStockRepository productStockRepository;

  public ProductStockService(ProductStockRepository productStockRepository) {
    this.productStockRepository = productStockRepository;
  }

  @Transactional
  public ProductStockResponse increaseStock(UUID productId, int amount) {
    ProductStock increased = loadStock(productId).increase(amount);
    productStockRepository.save(increased);
    return ProductStockResponse.from(increased);
  }

  @Transactional
  public ProductStockResponse decreaseStock(UUID productId, int amount) {
    ProductStock decreased = loadStock(productId).decrease(amount);
    productStockRepository.save(decreased);
    return ProductStockResponse.from(decreased);
  }

  @Transactional(readOnly = true)
  public ProductStockResponse getStockByProduct(UUID productId) {
    return ProductStockResponse.from(loadStock(productId));
  }

  private ProductStock loadStock(UUID productId) {
    return productStockRepository.findByProductId(productId)
        .orElseThrow(() -> new StockNotFoundException("Stock introuvable pour le produit : " + productId));
  }
}
