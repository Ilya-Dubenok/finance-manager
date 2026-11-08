package org.example.finmanager.adapter.inbound.cli.render;

import java.math.BigDecimal;
import java.util.Locale;
import lombok.experimental.UtilityClass;

/** Формат денег: запятая между тысячами, точка перед копейками, два знака (D-10). */
@UtilityClass
public class MoneyFormatter {

  private static final String FORMAT = "%,.2f";

  /**
   * Форматирует сумму.
   *
   * @param amount сумма
   * @return текст вида {@code 54,700.00}
   */
  public String format(BigDecimal amount) {
    return String.format(Locale.US, FORMAT, amount);
  }
}
