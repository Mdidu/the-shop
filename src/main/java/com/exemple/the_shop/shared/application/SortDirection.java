package com.exemple.the_shop.shared.application;

/**
 * Direction de tri, sous forme « maison » pour ne pas faire fuiter le
 * {@code org.springframework.data.domain.Sort.Direction} de Spring au-dessus
 * de la couche persistence. L'adapter traduit vers le type Spring au dernier
 * moment.
 */
public enum SortDirection {
  ASC,
  DESC
}
