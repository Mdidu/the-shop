package com.exemple.the_shop.catalog.application.product;

/**
 * Whitelist fermée des champs sur lesquels un listing produit peut être trié.
 * La valeur brute du client n'atteint JAMAIS le {@code ORDER BY} : elle est
 * mappée vers une de ces constantes (sécu : pas de tri sur colonne arbitraire ;
 * perf : tri sur colonnes indexées seulement). Hors whitelist → rejet côté web.
 */
public enum ProductSortField {
  NAME,
  PRICE
}
