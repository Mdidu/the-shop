package com.exemple.the_shop.shared.domain;

import java.text.Normalizer;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Value object représentant un identifiant URL-safe (ex. {@code telephones-pro}).
 *
 * <p>Deux chemins de construction, volontairement distincts :
 * <ul>
 *   <li>{@link #fromName(String)} — génère un slug à partir d'un texte libre
 *       (désaccentuation, minuscules, tirets). C'est le chemin <em>création</em>.</li>
 *   <li>{@link #of(String)} — enrobe et valide une valeur déjà slugifiée
 *       (lecture depuis la base). On ne re-slugifie jamais une valeur stockée.</li>
 * </ul>
 *
 * <p>Pur domaine : aucune dépendance framework.
 */
public record Slug(String value) {

  /** Format attendu : minuscules/chiffres séparés par des tirets simples, sans tiret aux extrémités. */
  private static final Pattern VALID = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

  private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
  private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
  private static final Pattern EDGE_HYPHENS = Pattern.compile("(^-+)|(-+$)");

  public Slug {
    Objects.requireNonNull(value, "Le slug ne peut pas être nul");
    if (!VALID.matcher(value).matches()) {
      throw new IllegalArgumentException("Slug invalide : " + value);
    }
  }

  /**
   * Enrobe une valeur déjà slugifiée (typiquement lue en base). Valide le format
   * via le compact constructor, mais ne transforme pas la chaîne.
   */
  public static Slug of(String existing) {
    return new Slug(existing);
  }

  /**
   * Génère un slug à partir d'un texte libre. « Téléphones Pro » → {@code telephones-pro}.
   *
   * <p>Si le texte ne produit aucun caractère slugifiable (ex. chaîne vide, ou
   * uniquement des symboles/idéogrammes), on retombe sur un jeton aléatoire
   * valide plutôt que d'échouer — un slug est toujours produit.
   */
  public static Slug fromName(String name) {
    Objects.requireNonNull(name, "Le nom ne peut pas être nul");
    String slug = Normalizer.normalize(name, Normalizer.Form.NFD); // sépare lettre + accent
    slug = DIACRITICS.matcher(slug).replaceAll("");                // retire les accents
    slug = slug.toLowerCase();
    slug = NON_ALPHANUMERIC.matcher(slug).replaceAll("-");         // espaces & symboles → tirets
    slug = EDGE_HYPHENS.matcher(slug).replaceAll("");              // pas de tiret en début/fin
    if (slug.isEmpty()) {
      slug = randomToken();
    }
    return new Slug(slug);
  }

  /** Jeton hexadécimal aléatoire (12 caractères) : chiffres et {@code a-f}, donc slug valide. */
  private static String randomToken() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
  }

  @Override
  public String toString() {
    return value;
  }
}
