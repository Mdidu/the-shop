package com.exemple.the_shop.catalog.infrastructure.persistence.product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.exemple.the_shop.catalog.domain.model.Product;
import com.exemple.the_shop.catalog.domain.port.out.ProductRepository;
import com.exemple.the_shop.shared.domain.Slug;

@Repository
public class ProductRepositoryImpl implements ProductRepository {

  private final ProductJpaRepository productJpaRepository;

  public ProductRepositoryImpl(ProductJpaRepository productJpaRepository) {
    this.productJpaRepository = productJpaRepository;
  }

  @Override
  public Optional<Product> findById(UUID id) {
    return productJpaRepository.findById(id).map(ProductMapper::toDomain);
  }

  @Override
  public Optional<Product> findBySlug(Slug slug) {
    return productJpaRepository.findBySlug(slug.value()).map(ProductMapper::toDomain);
  }

  @Override
  public List<Product> findAllByCategoryId(UUID categoryId) {
    return productJpaRepository.findAllByCategoryId(categoryId).stream().map(ProductMapper::toDomain).toList();
  }

  @Override
  public void save(Product product) {
    ProductJpaEntity entity = ProductMapper.toJpaEntity(product);
    productJpaRepository.save(entity);
  }

}
