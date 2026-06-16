package com.exemple.the_shop.user.infrastructure.persistence.user;

import com.exemple.the_shop.user.domain.model.User;

public final class UserMapper {

  private UserMapper() {
  }

  public static UserJpaEntity toJpaEntity(User user) {
    UserJpaEntity entity = new UserJpaEntity();
    entity.setId(user.getId());
    entity.setEmail(user.getEmail());
    entity.setPassword(user.getPassword());
    entity.setFirstName(user.getFirstName());
    entity.setLastName(user.getLastName());
    entity.setRole(user.getRole());
    return entity;
  }

  public static User toDomain(UserJpaEntity entity) {
    return new User(
        entity.getId(),
        entity.getEmail(),
        entity.getPassword(),
        entity.getFirstName(),
        entity.getLastName(),
        entity.getRole(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
