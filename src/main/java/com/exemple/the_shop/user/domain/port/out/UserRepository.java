package com.exemple.the_shop.user.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import com.exemple.the_shop.user.domain.model.User;

public interface UserRepository {
  Optional<User> findByEmail(String email);

  Optional<User> findById(UUID id);

  void save(User user);
}
