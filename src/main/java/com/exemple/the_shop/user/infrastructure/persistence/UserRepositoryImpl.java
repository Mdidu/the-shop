package com.exemple.the_shop.user.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.exemple.the_shop.user.domain.model.User;
import com.exemple.the_shop.user.domain.port.out.UserRepository;

@Repository
public class UserRepositoryImpl implements UserRepository {

  private final UserJpaRepository userJpaRepository;

  public UserRepositoryImpl(UserJpaRepository userJpaRepository) {
    this.userJpaRepository = userJpaRepository;
  }

  @Override
  public Optional<User> findByEmail(String email) {
    return userJpaRepository.findByEmail(email).map(UserMapper::toDomain);
  }

  @Override
  public void save(User user) {
    UserJpaEntity entity = UserMapper.toJpaEntity(user);
    userJpaRepository.save(entity);
  }

}
