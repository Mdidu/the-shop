package com.exemple.the_shop.user.domain.model;

import java.time.Instant;
import java.util.UUID;

public class User {

  private final UUID id;
  private final String email;
  private final String password;
  private final String firstName;
  private final String lastName;
  private final Role role;
  private final Instant createdAt;
  private final Instant updatedAt;

  public User(UUID id, String email, String password, String firstName, String lastName,
      Role role, Instant createdAt, Instant updatedAt) {
    this.id = id;
    this.email = email;
    this.password = password;
    this.firstName = firstName;
    this.lastName = lastName;
    this.role = role;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  /**
   * Crée un nouvel utilisateur : l'identité ({@code id}) est générée ici, dans
   * le domaine, et le rôle par défaut est {@link Role#CUSTOMER}. {@code password}
   * est attendu déjà encodé. {@code createdAt}/{@code updatedAt} restent nuls :
   * attribués par la persistence ({@code @PrePersist}).
   */
  public static User create(String email, String password, String firstName, String lastName) {
    return new User(UUID.randomUUID(), email, password, firstName, lastName,
        Role.CUSTOMER, null, null);
  }

  public UUID getId() { return id; }
  public String getEmail() { return email; }
  public String getPassword() { return password; }
  public String getFirstName() { return firstName; }
  public String getLastName() { return lastName; }
  public Role getRole() { return role; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
