package com.exemple.the_shop.catalog.infrastructure.persistence.stock;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.exemple.the_shop.catalog.domain.model.ProductStock;
import com.exemple.the_shop.catalog.domain.port.out.ProductStockRepository;

@Repository
public class ProductStockRepositoryImpl implements ProductStockRepository {
  private final ProductStockJpaRepository productStockJpaRepository;

  public ProductStockRepositoryImpl(ProductStockJpaRepository productStockJpaRepository) {
    this.productStockJpaRepository = productStockJpaRepository;
  }

  @Override
  public Optional<ProductStock> findById(UUID id) {
    return productStockJpaRepository.findById(id).map(ProductStockMapper::toDomain);
  }

  @Override
  public Optional<ProductStock> findByProductId(UUID productId) {
    return productStockJpaRepository.findByProductId(productId).map(ProductStockMapper::toDomain);
  }

  @Override
  public void save(ProductStock productStock) {
    ProductStockJpaEntity entity = ProductStockMapper.toJpaEntity(productStock);
    productStockJpaRepository.save(entity);
  }

}
