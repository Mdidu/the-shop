package com.exemple.the_shop.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal amount) {

  public Money {
    Objects.requireNonNull(amount, "Le montant ne peut pas être nul !");

    if (amount.signum() < 0) {
      throw new IllegalArgumentException("Le montant ne peut pas être négatif ! " + amount);
    }

    amount = amount.setScale(2, RoundingMode.HALF_UP);
  }

  public static Money of(BigDecimal amount) {
    return new Money(amount);
  }

  public Money plus(Money other) {
    return new Money(this.amount.add(other.amount));
  }
}
