package com.exemple.the_shop.user.domain.port.out;

import java.util.Optional;

import com.exemple.the_shop.user.domain.model.User;

public interface UserRepository {
  Optional<User> findByEmail(String email);

  void save(User user);
}
